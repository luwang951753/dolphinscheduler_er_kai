/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.connectors.doris.config.DorisConfig;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorErrorCode;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorException;
import org.apache.seatunnel.connectors.doris.rest.models.RespContent;
import org.apache.seatunnel.connectors.doris.sink.HttpPutBuilder;
import org.apache.seatunnel.connectors.doris.sink.writer.LabelGenerator;
import org.apache.seatunnel.connectors.doris.sink.writer.RecordStream;
import org.apache.seatunnel.connectors.doris.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DorisStreamLoad
implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(DorisStreamLoad.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int HTTP_TEMPORARY_REDIRECT = 200;
    private final LabelGenerator labelGenerator;
    private final byte[] lineDelimiter;
    private static final String LOAD_URL_PATTERN = "http://%s/api/%s/%s/_stream_load";
    private static final String ABORT_URL_PATTERN = "http://%s/api/%s/_stream_load_2pc";
    private static final String JOB_EXIST_FINISHED = "FINISHED";
    private String loadUrlStr;
    private String hostPort;
    private final String abortUrlStr;
    private final String user;
    private final String passwd;
    private final String db;
    private final String table;
    private final boolean enable2PC;
    private final boolean enableDelete;
    private final Properties streamLoadProp;
    private final RecordStream recordStream;
    private Future<CloseableHttpResponse> pendingLoadFuture;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    private boolean loadBatchFirstRecord;

    public DorisStreamLoad(String hostPort, DorisConfig dorisConfig, LabelGenerator labelGenerator, CloseableHttpClient httpClient) {
        this.hostPort = hostPort;
        String[] tableInfo = dorisConfig.getTableIdentifier().split("\\.");
        this.db = tableInfo[0];
        this.table = tableInfo[1];
        this.user = dorisConfig.getUsername();
        this.passwd = dorisConfig.getPassword();
        this.labelGenerator = labelGenerator;
        this.loadUrlStr = String.format(LOAD_URL_PATTERN, hostPort, this.db, this.table);
        this.abortUrlStr = String.format(ABORT_URL_PATTERN, hostPort, this.db);
        this.enable2PC = dorisConfig.getEnable2PC();
        this.streamLoadProp = dorisConfig.getStreamLoadProps();
        this.enableDelete = dorisConfig.getEnableDelete();
        this.httpClient = httpClient;
        this.executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("stream-load-upload").build());
        this.recordStream = new RecordStream(dorisConfig.getBufferSize(), dorisConfig.getBufferCount());
        this.lineDelimiter = this.streamLoadProp.getProperty("line_delimiter", "\n").getBytes();
        this.loadBatchFirstRecord = true;
    }

    public String getDb() {
        return this.db;
    }

    public String getHostPort() {
        return this.hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
        this.loadUrlStr = String.format(LOAD_URL_PATTERN, hostPort, this.db, this.table);
    }

    public Future<CloseableHttpResponse> getPendingLoadFuture() {
        return this.pendingLoadFuture;
    }

    public void abortPreCommit(String labelSuffix, long chkID) throws Exception {
        long startChkID = chkID;
        log.info("abort for labelSuffix {}. start chkId {}.", (Object)labelSuffix, (Object)chkID);
        try {
            while (true) {
                String label = this.labelGenerator.generateLabel(startChkID);
                HttpPutBuilder builder = new HttpPutBuilder();
                builder.setUrl(this.loadUrlStr).baseAuth(this.user, this.passwd).addCommonHeader().enable2PC().setLabel(label).setEmptyEntity().addProperties(this.streamLoadProp);
                RespContent respContent = this.handlePreCommitResponse(this.httpClient.execute(builder.build()));
                Preconditions.checkState("true".equals(respContent.getTwoPhaseCommit()));
                if (!"Label Already Exists".equals(respContent.getStatus())) {
                    log.info("abort {} for check label {}.", (Object)respContent.getTxnId(), (Object)label);
                    this.abortTransaction(respContent.getTxnId());
                    break;
                }
                if (JOB_EXIST_FINISHED.equals(respContent.getExistingJobStatus())) {
                    throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "Load status is Label Already Exists and load job finished, change you label prefix or restore from latest savepoint!");
                }
                Matcher matcher = ResponseUtil.LABEL_EXIST_PATTERN.matcher(respContent.getMessage());
                if (!matcher.find()) {
                    throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "Load Status is Label Already Exists, but no txnID associated with it!response: " + respContent);
                }
                Preconditions.checkState(label.equals(matcher.group(1)));
                long txnId = Long.parseLong(matcher.group(2));
                log.info("abort {} for exist label {}", (Object)txnId, (Object)label);
                this.abortTransaction(txnId);
                ++startChkID;
            }
        }
        catch (Exception e) {
            log.warn("failed to stream load data", e);
            throw e;
        }
        log.info("abort for labelSuffix {} finished", (Object)labelSuffix);
    }

    public void writeRecord(byte[] record) throws IOException {
        if (this.loadBatchFirstRecord) {
            this.loadBatchFirstRecord = false;
        } else {
            this.recordStream.write(this.lineDelimiter);
        }
        this.recordStream.write(record);
    }

    @VisibleForTesting
    public RecordStream getRecordStream() {
        return this.recordStream;
    }

    public RespContent handlePreCommitResponse(CloseableHttpResponse response) throws Exception {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200 && response.getEntity() != null) {
            String loadResult = EntityUtils.toString(response.getEntity());
            log.info("load Result {}", (Object)loadResult);
            return OBJECT_MAPPER.readValue(loadResult, RespContent.class);
        }
        throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, response.getStatusLine().toString());
    }

    public RespContent stopLoad() throws IOException {
        this.recordStream.endInput();
        log.info("stream load stopped.");
        Preconditions.checkState(this.pendingLoadFuture != null);
        try {
            return this.handlePreCommitResponse(this.pendingLoadFuture.get());
        }
        catch (Exception e) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, (Throwable)e);
        }
    }

    public void startLoad(String label) throws IOException {
        this.loadBatchFirstRecord = true;
        HttpPutBuilder putBuilder = new HttpPutBuilder();
        this.recordStream.startInput();
        log.info("stream load started for {}", (Object)label);
        try {
            InputStreamEntity entity = new InputStreamEntity(this.recordStream);
            putBuilder.setUrl(this.loadUrlStr).baseAuth(this.user, this.passwd).addCommonHeader().addHiddenColumns(this.enableDelete).setLabel(label).setEntity(entity).addProperties(this.streamLoadProp);
            if (this.enable2PC) {
                putBuilder.enable2PC();
            }
            this.pendingLoadFuture = this.executorService.submit(() -> {
                log.info("start execute load");
                return this.httpClient.execute(putBuilder.build());
            });
        }
        catch (Exception e) {
            String err = "failed to stream load data with label: " + label;
            log.warn(err, e);
            throw e;
        }
    }

    public void abortTransaction(long txnID) throws Exception {
        HttpPutBuilder builder = new HttpPutBuilder();
        builder.setUrl(this.abortUrlStr).baseAuth(this.user, this.passwd).addCommonHeader().addTxnId(txnID).setEmptyEntity().abort();
        CloseableHttpResponse response = this.httpClient.execute(builder.build());
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200 || response.getEntity() == null) {
            log.warn("abort transaction response: " + response.getStatusLine().toString());
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "Fail to abort transaction " + txnID + " with url " + this.abortUrlStr);
        }
        ObjectMapper mapper = new ObjectMapper();
        String loadResult = EntityUtils.toString(response.getEntity());
        Map res = mapper.readValue(loadResult, new TypeReference<HashMap<String, String>>(){});
        if (!"Success".equals(res.get("status"))) {
            if (ResponseUtil.isCommitted((String)res.get("msg"))) {
                throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "try abort committed transaction, do you recover from old savepoint?");
            }
            log.warn("Fail to abort transaction. txnId: {}, error: {}", (Object)txnID, res.get("msg"));
        }
    }

    public void close() throws IOException {
        if (null != this.httpClient) {
            try {
                this.httpClient.close();
            }
            catch (IOException e) {
                throw new IOException("Closing httpClient failed.", e);
            }
        }
        if (null != this.executorService) {
            this.executorService.shutdownNow();
        }
    }
}

