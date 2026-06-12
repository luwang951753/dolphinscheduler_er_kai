/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.config;

import java.util.Map;
import java.util.Properties;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.common.config.CheckConfigUtil;
import org.apache.seatunnel.shade.com.typesafe.config.Config;

public class DorisConfig {
    public static final int DORIS_TABLET_SIZE_MIN = 1;
    public static final int DORIS_TABLET_SIZE_DEFAULT = Integer.MAX_VALUE;
    public static final int DORIS_REQUEST_CONNECT_TIMEOUT_MS_DEFAULT = 30000;
    public static final int DORIS_REQUEST_READ_TIMEOUT_MS_DEFAULT = 30000;
    private static final int DORIS_REQUEST_QUERY_TIMEOUT_S_DEFAULT = 3600;
    public static final int DORIS_REQUEST_RETRIES_DEFAULT = 3;
    private static final Boolean DORIS_DESERIALIZE_ARROW_ASYNC_DEFAULT = false;
    private static final int DORIS_DESERIALIZE_QUEUE_SIZE_DEFAULT = 64;
    private static final int DORIS_BATCH_SIZE_DEFAULT = 1024;
    private static final long DORIS_EXEC_MEM_LIMIT_DEFAULT = 0x80000000L;
    private static final int DEFAULT_SINK_CHECK_INTERVAL = 10000;
    private static final int DEFAULT_SINK_MAX_RETRIES = 3;
    private static final int DEFAULT_SINK_BUFFER_SIZE = 262144;
    private static final int DEFAULT_SINK_BUFFER_COUNT = 3;
    public static final Option<String> FENODES = Options.key("fenodes").stringType().noDefaultValue().withDescription("doris fe http address.");
    public static final Option<String> TABLE_IDENTIFIER = Options.key("table.identifier").stringType().noDefaultValue().withDescription("the doris table name.");
    public static final Option<String> USERNAME = Options.key("username").stringType().noDefaultValue().withDescription("the doris user name.");
    public static final Option<String> PASSWORD = Options.key("password").stringType().noDefaultValue().withDescription("the doris password.");
    public static final Option<String> DORIS_READ_FIELD = Options.key("doris.read.field").stringType().noDefaultValue().withDescription("List of column names in the Doris table, separated by commas");
    public static final Option<String> DORIS_FILTER_QUERY = Options.key("doris.filter.query").stringType().noDefaultValue().withDescription("Filter expression of the query, which is transparently transmitted to Doris. Doris uses this expression to complete source-side data filtering");
    public static final Option<Integer> DORIS_TABLET_SIZE = Options.key("doris.request.tablet.size").intType().defaultValue(Integer.MAX_VALUE).withDescription("");
    public static final Option<Integer> DORIS_REQUEST_CONNECT_TIMEOUT_MS = Options.key("doris.request.connect.timeout.ms").intType().defaultValue(30000).withDescription("");
    public static final Option<Integer> DORIS_REQUEST_READ_TIMEOUT_MS = Options.key("doris.request.read.timeout.ms").intType().defaultValue(30000).withDescription("");
    public static final Option<Integer> DORIS_REQUEST_QUERY_TIMEOUT_S = Options.key("doris.request.query.timeout.s").intType().defaultValue(3600).withDescription("");
    public static final Option<Integer> DORIS_REQUEST_RETRIES = Options.key("doris.request.retries").intType().defaultValue(3).withDescription("");
    public static final Option<Boolean> DORIS_DESERIALIZE_ARROW_ASYNC = Options.key("doris.deserialize.arrow.async").booleanType().defaultValue(DORIS_DESERIALIZE_ARROW_ASYNC_DEFAULT).withDescription("");
    public static final Option<Integer> DORIS_DESERIALIZE_QUEUE_SIZE = Options.key("doris.request.retriesdoris.deserialize.queue.size").intType().defaultValue(64).withDescription("");
    public static final Option<Integer> DORIS_BATCH_SIZE = Options.key("doris.batch.size").intType().defaultValue(1024).withDescription("");
    public static final Option<Long> DORIS_EXEC_MEM_LIMIT = Options.key("doris.exec.mem.limit").longType().defaultValue(0x80000000L).withDescription("");
    public static final Option<Boolean> SOURCE_USE_OLD_API = Options.key("source.use-old-api").booleanType().defaultValue(false).withDescription("Whether to read data using the new interface defined according to the FLIP-27 specification,default false");
    public static final Option<Boolean> SINK_ENABLE_2PC = Options.key("sink.enable-2pc").booleanType().defaultValue(true).withDescription("enable 2PC while loading");
    public static final Option<Integer> SINK_CHECK_INTERVAL = Options.key("sink.check-interval").intType().defaultValue(10000).withDescription("check exception with the interval while loading");
    public static final Option<Integer> SINK_MAX_RETRIES = Options.key("sink.max-retries").intType().defaultValue(3).withDescription("the max retry times if writing records to database failed.");
    public static final Option<Integer> SINK_BUFFER_SIZE = Options.key("sink.buffer-size").intType().defaultValue(262144).withDescription("the buffer size to cache data for stream load.");
    public static final Option<Integer> SINK_BUFFER_COUNT = Options.key("sink.buffer-count").intType().defaultValue(3).withDescription("the buffer count to cache data for stream load.");
    public static final Option<String> SINK_LABEL_PREFIX = Options.key("sink.label-prefix").stringType().defaultValue("").withDescription("the unique label prefix.");
    public static final Option<Boolean> SINK_ENABLE_DELETE = Options.key("sink.enable-delete").booleanType().defaultValue(false).withDescription("whether to enable the delete function");
    public static final Option<Map<String, String>> DORIS_SINK_CONFIG_PREFIX = Options.key("doris.config").mapType().noDefaultValue().withDescription("The parameter of the Stream Load data_desc. The way to specify the parameter is to add the prefix `doris.config` to the original load parameter name ");
    private String frontends;
    private String username;
    private String password;
    private String tableIdentifier;
    private String readField;
    private String filterQuery;
    private Integer tabletSize;
    private Integer requestConnectTimeoutMs;
    private Integer requestReadTimeoutMs;
    private Integer requestQueryTimeoutS;
    private Integer requestRetries;
    private boolean deserializeArrowAsync;
    private int deserializeQueueSize;
    private int batchSize;
    private int execMemLimit;
    private boolean useOldApi;
    private Boolean enable2PC;
    private Boolean enableDelete;
    private String labelPrefix;
    private Integer checkInterval;
    private Integer maxRetries;
    private Integer bufferSize;
    private Integer bufferCount;
    private Properties streamLoadProps;

    public static DorisConfig loadConfig(Config pluginConfig) {
        DorisConfig dorisConfig = new DorisConfig();
        dorisConfig.setFrontends(pluginConfig.getString(FENODES.key()));
        dorisConfig.setUsername(pluginConfig.getString(USERNAME.key()));
        dorisConfig.setPassword(pluginConfig.getString(PASSWORD.key()));
        dorisConfig.setTableIdentifier(pluginConfig.getString(TABLE_IDENTIFIER.key()));
        dorisConfig.setStreamLoadProps(DorisConfig.parseStreamLoadProperties(pluginConfig));
        if (pluginConfig.hasPath(DORIS_READ_FIELD.key())) {
            dorisConfig.setReadField(pluginConfig.getString(DORIS_READ_FIELD.key()));
        } else {
            dorisConfig.setReadField(DORIS_READ_FIELD.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_FILTER_QUERY.key())) {
            dorisConfig.setFilterQuery(pluginConfig.getString(DORIS_FILTER_QUERY.key()));
        } else {
            dorisConfig.setFilterQuery(DORIS_FILTER_QUERY.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_TABLET_SIZE.key())) {
            dorisConfig.setTabletSize(pluginConfig.getInt(DORIS_TABLET_SIZE.key()));
        } else {
            dorisConfig.setTabletSize(DORIS_TABLET_SIZE.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_REQUEST_CONNECT_TIMEOUT_MS.key())) {
            dorisConfig.setRequestReadTimeoutMs(pluginConfig.getInt(DORIS_REQUEST_CONNECT_TIMEOUT_MS.key()));
        } else {
            dorisConfig.setRequestReadTimeoutMs(DORIS_REQUEST_CONNECT_TIMEOUT_MS.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_REQUEST_QUERY_TIMEOUT_S.key())) {
            dorisConfig.setRequestQueryTimeoutS(pluginConfig.getInt(DORIS_REQUEST_QUERY_TIMEOUT_S.key()));
        } else {
            dorisConfig.setRequestQueryTimeoutS(DORIS_REQUEST_QUERY_TIMEOUT_S.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_REQUEST_READ_TIMEOUT_MS.key())) {
            dorisConfig.setRequestReadTimeoutMs(pluginConfig.getInt(DORIS_REQUEST_READ_TIMEOUT_MS.key()));
        } else {
            dorisConfig.setRequestReadTimeoutMs(DORIS_REQUEST_READ_TIMEOUT_MS.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_REQUEST_RETRIES.key())) {
            dorisConfig.setRequestRetries(pluginConfig.getInt(DORIS_REQUEST_RETRIES.key()));
        } else {
            dorisConfig.setRequestRetries(DORIS_REQUEST_RETRIES.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_DESERIALIZE_ARROW_ASYNC.key())) {
            dorisConfig.setDeserializeArrowAsync(pluginConfig.getBoolean(DORIS_DESERIALIZE_ARROW_ASYNC.key()));
        } else {
            dorisConfig.setDeserializeArrowAsync(DORIS_DESERIALIZE_ARROW_ASYNC.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_DESERIALIZE_QUEUE_SIZE.key())) {
            dorisConfig.setDeserializeQueueSize(pluginConfig.getInt(DORIS_DESERIALIZE_QUEUE_SIZE.key()));
        } else {
            dorisConfig.setDeserializeQueueSize(DORIS_DESERIALIZE_QUEUE_SIZE.defaultValue());
        }
        if (pluginConfig.hasPath(DORIS_BATCH_SIZE.key())) {
            dorisConfig.setDeserializeQueueSize(pluginConfig.getInt(DORIS_BATCH_SIZE.key()));
        } else {
            dorisConfig.setDeserializeQueueSize(DORIS_BATCH_SIZE.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_ENABLE_2PC.key())) {
            dorisConfig.setEnable2PC(pluginConfig.getBoolean(SINK_ENABLE_2PC.key()));
        } else {
            dorisConfig.setEnable2PC(SINK_ENABLE_2PC.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_LABEL_PREFIX.key())) {
            dorisConfig.setLabelPrefix(pluginConfig.getString(SINK_LABEL_PREFIX.key()));
        } else {
            dorisConfig.setLabelPrefix(SINK_LABEL_PREFIX.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_CHECK_INTERVAL.key())) {
            dorisConfig.setCheckInterval(pluginConfig.getInt(SINK_CHECK_INTERVAL.key()));
        } else {
            dorisConfig.setCheckInterval(SINK_CHECK_INTERVAL.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_MAX_RETRIES.key())) {
            dorisConfig.setMaxRetries(pluginConfig.getInt(SINK_MAX_RETRIES.key()));
        } else {
            dorisConfig.setMaxRetries(SINK_MAX_RETRIES.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_BUFFER_SIZE.key())) {
            dorisConfig.setBufferSize(pluginConfig.getInt(SINK_BUFFER_SIZE.key()));
        } else {
            dorisConfig.setBufferSize(SINK_BUFFER_SIZE.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_BUFFER_COUNT.key())) {
            dorisConfig.setBufferCount(pluginConfig.getInt(SINK_BUFFER_COUNT.key()));
        } else {
            dorisConfig.setBufferCount(SINK_BUFFER_COUNT.defaultValue());
        }
        if (pluginConfig.hasPath(SINK_ENABLE_DELETE.key())) {
            dorisConfig.setEnableDelete(pluginConfig.getBoolean(SINK_ENABLE_DELETE.key()));
        } else {
            dorisConfig.setEnableDelete(SINK_ENABLE_DELETE.defaultValue());
        }
        return dorisConfig;
    }

    private static Properties parseStreamLoadProperties(Config pluginConfig) {
        Properties streamLoadProps = new Properties();
        if (CheckConfigUtil.isValidParam(pluginConfig, DORIS_SINK_CONFIG_PREFIX.key())) {
            pluginConfig.getObject(DORIS_SINK_CONFIG_PREFIX.key()).forEach((key, value) -> {
                String configKey = key.toLowerCase();
                streamLoadProps.put(configKey, value.unwrapped().toString());
            });
        }
        return streamLoadProps;
    }

    public void setFrontends(String frontends) {
        this.frontends = frontends;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTableIdentifier(String tableIdentifier) {
        this.tableIdentifier = tableIdentifier;
    }

    public void setReadField(String readField) {
        this.readField = readField;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public void setTabletSize(Integer tabletSize) {
        this.tabletSize = tabletSize;
    }

    public void setRequestConnectTimeoutMs(Integer requestConnectTimeoutMs) {
        this.requestConnectTimeoutMs = requestConnectTimeoutMs;
    }

    public void setRequestReadTimeoutMs(Integer requestReadTimeoutMs) {
        this.requestReadTimeoutMs = requestReadTimeoutMs;
    }

    public void setRequestQueryTimeoutS(Integer requestQueryTimeoutS) {
        this.requestQueryTimeoutS = requestQueryTimeoutS;
    }

    public void setRequestRetries(Integer requestRetries) {
        this.requestRetries = requestRetries;
    }

    public void setDeserializeArrowAsync(boolean deserializeArrowAsync) {
        this.deserializeArrowAsync = deserializeArrowAsync;
    }

    public void setDeserializeQueueSize(int deserializeQueueSize) {
        this.deserializeQueueSize = deserializeQueueSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setExecMemLimit(int execMemLimit) {
        this.execMemLimit = execMemLimit;
    }

    public void setUseOldApi(boolean useOldApi) {
        this.useOldApi = useOldApi;
    }

    public void setEnable2PC(Boolean enable2PC) {
        this.enable2PC = enable2PC;
    }

    public void setEnableDelete(Boolean enableDelete) {
        this.enableDelete = enableDelete;
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setBufferCount(Integer bufferCount) {
        this.bufferCount = bufferCount;
    }

    public void setStreamLoadProps(Properties streamLoadProps) {
        this.streamLoadProps = streamLoadProps;
    }

    public String getFrontends() {
        return this.frontends;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getTableIdentifier() {
        return this.tableIdentifier;
    }

    public String getReadField() {
        return this.readField;
    }

    public String getFilterQuery() {
        return this.filterQuery;
    }

    public Integer getTabletSize() {
        return this.tabletSize;
    }

    public Integer getRequestConnectTimeoutMs() {
        return this.requestConnectTimeoutMs;
    }

    public Integer getRequestReadTimeoutMs() {
        return this.requestReadTimeoutMs;
    }

    public Integer getRequestQueryTimeoutS() {
        return this.requestQueryTimeoutS;
    }

    public Integer getRequestRetries() {
        return this.requestRetries;
    }

    public boolean isDeserializeArrowAsync() {
        return this.deserializeArrowAsync;
    }

    public int getDeserializeQueueSize() {
        return this.deserializeQueueSize;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public int getExecMemLimit() {
        return this.execMemLimit;
    }

    public boolean isUseOldApi() {
        return this.useOldApi;
    }

    public Boolean getEnable2PC() {
        return this.enable2PC;
    }

    public Boolean getEnableDelete() {
        return this.enableDelete;
    }

    public String getLabelPrefix() {
        return this.labelPrefix;
    }

    public Integer getCheckInterval() {
        return this.checkInterval;
    }

    public Integer getMaxRetries() {
        return this.maxRetries;
    }

    public Integer getBufferSize() {
        return this.bufferSize;
    }

    public Integer getBufferCount() {
        return this.bufferCount;
    }

    public Properties getStreamLoadProps() {
        return this.streamLoadProps;
    }

    public String toString() {
        return "DorisConfig(frontends=" + this.getFrontends() + ", username=" + this.getUsername() + ", password=" + this.getPassword() + ", tableIdentifier=" + this.getTableIdentifier() + ", readField=" + this.getReadField() + ", filterQuery=" + this.getFilterQuery() + ", tabletSize=" + this.getTabletSize() + ", requestConnectTimeoutMs=" + this.getRequestConnectTimeoutMs() + ", requestReadTimeoutMs=" + this.getRequestReadTimeoutMs() + ", requestQueryTimeoutS=" + this.getRequestQueryTimeoutS() + ", requestRetries=" + this.getRequestRetries() + ", deserializeArrowAsync=" + this.isDeserializeArrowAsync() + ", deserializeQueueSize=" + this.getDeserializeQueueSize() + ", batchSize=" + this.getBatchSize() + ", execMemLimit=" + this.getExecMemLimit() + ", useOldApi=" + this.isUseOldApi() + ", enable2PC=" + this.getEnable2PC() + ", enableDelete=" + this.getEnableDelete() + ", labelPrefix=" + this.getLabelPrefix() + ", checkInterval=" + this.getCheckInterval() + ", maxRetries=" + this.getMaxRetries() + ", bufferSize=" + this.getBufferSize() + ", bufferCount=" + this.getBufferCount() + ", streamLoadProps=" + this.getStreamLoadProps() + ")";
    }
}

