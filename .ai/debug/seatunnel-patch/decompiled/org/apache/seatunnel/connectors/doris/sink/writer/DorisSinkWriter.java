/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.connectors.doris.config.DorisConfig;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorErrorCode;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorException;
import org.apache.seatunnel.connectors.doris.rest.RestService;
import org.apache.seatunnel.connectors.doris.rest.models.BackendV2;
import org.apache.seatunnel.connectors.doris.rest.models.RespContent;
import org.apache.seatunnel.connectors.doris.serialize.DorisSerializer;
import org.apache.seatunnel.connectors.doris.serialize.SeaTunnelRowSerializer;
import org.apache.seatunnel.connectors.doris.sink.committer.DorisCommitInfo;
import org.apache.seatunnel.connectors.doris.sink.writer.DorisSinkState;
import org.apache.seatunnel.connectors.doris.sink.writer.DorisStreamLoad;
import org.apache.seatunnel.connectors.doris.sink.writer.LabelGenerator;
import org.apache.seatunnel.connectors.doris.util.HttpUtil;
import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DorisSinkWriter
implements SinkWriter<SeaTunnelRow, DorisCommitInfo, DorisSinkState> {
    private static final Logger log = LoggerFactory.getLogger(DorisSinkWriter.class);
    private static final int INITIAL_DELAY = 200;
    private static final int CONNECT_TIMEOUT = 1000;
    private static final List<String> DORIS_SUCCESS_STATUS = new ArrayList<String>(Arrays.asList("Success", "Publish Timeout"));
    private long lastCheckpointId;
    private DorisStreamLoad dorisStreamLoad;
    volatile boolean loading;
    private final DorisConfig dorisConfig;
    private final String labelPrefix;
    private final LabelGenerator labelGenerator;
    private final int intervalTime;
    private final DorisSinkState dorisSinkState;
    private final DorisSerializer serializer;
    private final transient ScheduledExecutorService scheduledExecutorService;
    private transient Thread executorThread;
    private volatile transient Exception loadException = null;
    private List<BackendV2.BackendRowV2> backends;
    private long pos;

    public DorisSinkWriter(SinkWriter.Context context, List<DorisSinkState> state, SeaTunnelRowType seaTunnelRowType, Config pluginConfig, String jobId) {
        this.dorisConfig = DorisConfig.loadConfig(pluginConfig);
        this.lastCheckpointId = state.size() != 0 ? state.get(0).getCheckpointId() : 0L;
        log.info("restore checkpointId {}", (Object)this.lastCheckpointId);
        log.info("labelPrefix " + this.dorisConfig.getLabelPrefix());
        this.dorisSinkState = new DorisSinkState(this.dorisConfig.getLabelPrefix(), this.lastCheckpointId);
        this.labelPrefix = this.dorisConfig.getLabelPrefix() + "_" + jobId + "_" + context.getIndexOfSubtask();
        this.labelGenerator = new LabelGenerator(this.labelPrefix, this.dorisConfig.getEnable2PC());
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("stream-load-check").build());
        this.serializer = this.createSerializer(this.dorisConfig, seaTunnelRowType);
        this.intervalTime = this.dorisConfig.getCheckInterval();
        this.loading = false;
    }

    public void initializeLoad(List<DorisSinkState> state) throws IOException {
        this.backends = RestService.getBackendsV2(this.dorisConfig, log);
        String backend = this.getAvailableBackend();
        try {
            this.dorisStreamLoad = new DorisStreamLoad(backend, this.dorisConfig, this.labelGenerator, new HttpUtil().getHttpClient());
            if (this.dorisConfig.getEnable2PC().booleanValue()) {
                this.dorisStreamLoad.abortPreCommit(this.labelPrefix, this.lastCheckpointId + 1L);
            }
        }
        catch (Exception e) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, (Throwable)e);
        }
        this.executorThread = Thread.currentThread();
        this.dorisStreamLoad.startLoad(this.labelGenerator.generateLabel(this.lastCheckpointId + 1L));
        this.scheduledExecutorService.scheduleWithFixedDelay(this::checkDone, 200L, this.intervalTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(SeaTunnelRow element) throws IOException {
        this.checkLoadException();
        byte[] serialize = this.serializer.serialize(element);
        if (Objects.isNull(serialize)) {
            return;
        }
        this.dorisStreamLoad.writeRecord(serialize);
    }

    @Override
    public Optional<DorisCommitInfo> prepareCommit() throws IOException {
        this.loading = false;
        Preconditions.checkState(this.dorisStreamLoad != null);
        RespContent respContent = this.dorisStreamLoad.stopLoad();
        if (!DORIS_SUCCESS_STATUS.contains(respContent.getStatus())) {
            String errMsg = String.format("stream load error: %s, see more in %s", respContent.getMessage(), respContent.getErrorURL());
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, errMsg);
        }
        if (!this.dorisConfig.getEnable2PC().booleanValue()) {
            return Optional.empty();
        }
        long txnId = respContent.getTxnId();
        return Optional.of(new DorisCommitInfo(this.dorisStreamLoad.getHostPort(), this.dorisStreamLoad.getDb(), txnId));
    }

    @Override
    public List<DorisSinkState> snapshotState(long checkpointId) throws IOException {
        Preconditions.checkState(this.dorisStreamLoad != null);
        this.dorisStreamLoad.setHostPort(this.getAvailableBackend());
        this.dorisStreamLoad.startLoad(this.labelGenerator.generateLabel(checkpointId + 1L));
        this.loading = true;
        this.lastCheckpointId = checkpointId;
        return Collections.singletonList(new DorisSinkState(this.labelPrefix, this.lastCheckpointId));
    }

    @Override
    public void abortPrepare() {
        if (this.dorisConfig.getEnable2PC().booleanValue()) {
            try {
                this.dorisStreamLoad.abortPreCommit(this.labelPrefix, this.lastCheckpointId + 1L);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkDone() {
        log.debug("start timer checker, interval {} ms", (Object)this.intervalTime);
        if (this.dorisStreamLoad.getPendingLoadFuture() != null && this.dorisStreamLoad.getPendingLoadFuture().isDone()) {
            String errorMsg;
            if (!this.loading) {
                log.debug("not loading, skip timer checker");
                return;
            }
            try {
                RespContent content = this.dorisStreamLoad.handlePreCommitResponse(this.dorisStreamLoad.getPendingLoadFuture().get());
                errorMsg = content.getMessage();
            }
            catch (Exception e) {
                errorMsg = e.getMessage();
            }
            this.loadException = new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, errorMsg);
            log.error("stream load finished unexpectedly, interrupt worker thread! {}", (Object)errorMsg);
            this.executorThread.interrupt();
        }
    }

    private void checkLoadException() {
        if (this.loadException != null) {
            throw new RuntimeException("error while loading data.", this.loadException);
        }
    }

    @VisibleForTesting
    public boolean isLoading() {
        return this.loading;
    }

    @VisibleForTesting
    public void setDorisStreamLoad(DorisStreamLoad streamLoad) {
        this.dorisStreamLoad = streamLoad;
    }

    @VisibleForTesting
    public void setBackends(List<BackendV2.BackendRowV2> backends) {
        this.backends = backends;
    }

    @Override
    public void close() throws IOException {
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdownNow();
        }
        if (this.dorisStreamLoad != null) {
            this.dorisStreamLoad.close();
        }
    }

    @VisibleForTesting
    public String getAvailableBackend() {
        long tmp = this.pos + (long)this.backends.size();
        while (this.pos < tmp) {
            BackendV2.BackendRowV2 backend = this.backends.get((int)(this.pos % (long)this.backends.size()));
            String res = backend.toBackendString();
            if (!this.tryHttpConnection(res)) continue;
            ++this.pos;
            return res;
        }
        String errMsg = "no available backend.";
        throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, errMsg);
    }

    public boolean tryHttpConnection(String backend) {
        try {
            backend = "http://" + backend;
            URL url = new URL(backend);
            HttpURLConnection co = (HttpURLConnection)url.openConnection();
            co.setConnectTimeout(1000);
            co.connect();
            co.disconnect();
            return true;
        }
        catch (Exception ex) {
            log.warn("Failed to connect to backend:{}", (Object)backend, (Object)ex);
            ++this.pos;
            return false;
        }
    }

    private DorisSerializer createSerializer(DorisConfig dorisConfig, SeaTunnelRowType seaTunnelRowType) {
        return new SeaTunnelRowSerializer(dorisConfig.getStreamLoadProps().getProperty("format").toLowerCase(), seaTunnelRowType, dorisConfig.getStreamLoadProps().getProperty("column_separator"), dorisConfig.getEnableDelete());
    }
}
