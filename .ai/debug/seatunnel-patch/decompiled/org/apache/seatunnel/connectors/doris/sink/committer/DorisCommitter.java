/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.committer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.seatunnel.api.sink.SinkCommitter;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.connectors.doris.config.DorisConfig;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorErrorCode;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorException;
import org.apache.seatunnel.connectors.doris.rest.RestService;
import org.apache.seatunnel.connectors.doris.sink.HttpPutBuilder;
import org.apache.seatunnel.connectors.doris.sink.committer.DorisCommitInfo;
import org.apache.seatunnel.connectors.doris.util.HttpUtil;
import org.apache.seatunnel.connectors.doris.util.ResponseUtil;
import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DorisCommitter
implements SinkCommitter<DorisCommitInfo> {
    private static final Logger log = LoggerFactory.getLogger(DorisCommitter.class);
    private static final String COMMIT_PATTERN = "http://%s/api/%s/_stream_load_2pc";
    private static final int HTTP_TEMPORARY_REDIRECT = 200;
    private final CloseableHttpClient httpClient;
    private final DorisConfig dorisConfig;
    int maxRetry;

    public DorisCommitter(Config pluginConfig) {
        this(DorisConfig.loadConfig(pluginConfig), DorisConfig.loadConfig(pluginConfig).getMaxRetries(), new HttpUtil().getHttpClient());
    }

    public DorisCommitter(DorisConfig dorisConfig, int maxRetry, CloseableHttpClient client) {
        this.dorisConfig = dorisConfig;
        this.maxRetry = maxRetry;
        this.httpClient = client;
    }

    @Override
    public List<DorisCommitInfo> commit(List<DorisCommitInfo> commitInfos) throws IOException {
        for (DorisCommitInfo commitInfo : commitInfos) {
            this.commitTransaction(commitInfo);
        }
        return Collections.emptyList();
    }

    @Override
    public void abort(List<DorisCommitInfo> commitInfos) throws IOException {
        for (DorisCommitInfo commitInfo : commitInfos) {
            this.abortTransaction(commitInfo);
        }
    }

    private void commitTransaction(DorisCommitInfo committable) throws IOException, DorisConnectorException {
        int statusCode = -1;
        String reasonPhrase = null;
        int retry = 0;
        String hostPort = committable.getHostPort();
        HttpResponse response = null;
        while (retry++ <= this.maxRetry) {
            HttpPutBuilder putBuilder = new HttpPutBuilder();
            putBuilder.setUrl(String.format(COMMIT_PATTERN, hostPort, committable.getDb())).baseAuth(this.dorisConfig.getUsername(), this.dorisConfig.getPassword()).addCommonHeader().addTxnId(committable.getTxbID()).setEmptyEntity().commit();
            try {
                response = this.httpClient.execute(putBuilder.build());
            }
            catch (IOException e) {
                log.error("commit transaction failed: ", e);
                hostPort = RestService.getBackend(this.dorisConfig, log);
                continue;
            }
            statusCode = response.getStatusLine().getStatusCode();
            reasonPhrase = response.getStatusLine().getReasonPhrase();
            if (statusCode == 200) break;
            log.warn("commit failed with {}, reason {}", (Object)hostPort, (Object)reasonPhrase);
            hostPort = RestService.getBackend(this.dorisConfig, log);
        }
        if (statusCode != 200) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, reasonPhrase);
        }
        ObjectMapper mapper = new ObjectMapper();
        if (response != null && response.getEntity() != null) {
            String loadResult = EntityUtils.toString(response.getEntity());
            Map res = mapper.readValue(loadResult, new TypeReference<HashMap<String, String>>(){});
            if (((String)res.get("status")).equals("Fail") && !ResponseUtil.isCommitted((String)res.get("msg"))) {
                throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.COMMIT_FAILED, loadResult);
            }
            log.info("load result {}", (Object)loadResult);
        }
    }

    private void abortTransaction(DorisCommitInfo committable) throws IOException, DorisConnectorException {
        int retry = 0;
        String hostPort = committable.getHostPort();
        HttpResponse response = null;
        while (retry++ <= this.maxRetry) {
            HttpPutBuilder builder = new HttpPutBuilder();
            builder.setUrl(String.format(COMMIT_PATTERN, hostPort, committable.getDb())).baseAuth(this.dorisConfig.getUsername(), this.dorisConfig.getPassword()).addCommonHeader().addTxnId(committable.getTxbID()).setEmptyEntity().abort();
            response = this.httpClient.execute(builder.build());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 && response.getEntity() != null) continue;
            log.warn("abort transaction response: " + response.getStatusLine().toString());
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "Fail to abort transaction " + committable.getTxbID() + " with url " + String.format(COMMIT_PATTERN, hostPort, committable.getDb()));
        }
        ObjectMapper mapper = new ObjectMapper();
        String loadResult = EntityUtils.toString(response.getEntity());
        Map res = mapper.readValue(loadResult, new TypeReference<HashMap<String, String>>(){});
        if (!"Success".equals(res.get("status"))) {
            if (ResponseUtil.isCommitted((String)res.get("msg"))) {
                throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.STREAM_LOAD_FAILED, "try abort committed transaction, do you recover from old savepoint?");
            }
            log.warn("Fail to abort transaction. txnId: {}, error: {}", (Object)committable.getTxbID(), res.get("msg"));
        }
    }
}

