/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.connectors.doris.config.DorisConfig;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorErrorCode;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorException;
import org.apache.seatunnel.connectors.doris.rest.PartitionDefinition;
import org.apache.seatunnel.connectors.doris.rest.models.Backend;
import org.apache.seatunnel.connectors.doris.rest.models.BackendRow;
import org.apache.seatunnel.connectors.doris.rest.models.BackendV2;
import org.apache.seatunnel.connectors.doris.rest.models.QueryPlan;
import org.apache.seatunnel.connectors.doris.rest.models.Schema;
import org.apache.seatunnel.connectors.doris.rest.models.Tablet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestService
implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(RestService.class);
    public static final int REST_RESPONSE_STATUS_OK = 200;
    public static final int REST_RESPONSE_CODE_OK = 0;
    private static final String REST_RESPONSE_BE_ROWS_KEY = "rows";
    private static final String API_PREFIX = "/api";
    private static final String SCHEMA = "_schema";
    private static final String QUERY_PLAN = "_query_plan";
    private static final String UNIQUE_KEYS_TYPE = "UNIQUE_KEYS";
    @Deprecated
    private static final String BACKENDS = "/rest/v1/system?path=//backends";
    private static final String BACKENDS_V2 = "/api/backends?is_alive=true";
    private static final String FE_LOGIN = "/rest/v1/login";
    private static final String BASE_URL = "http://%s%s";

    private static String send(DorisConfig dorisConfig, HttpRequestBase request, Logger logger) throws DorisConnectorException {
        int connectTimeout = dorisConfig.getRequestConnectTimeoutMs() == null ? 30000 : dorisConfig.getRequestConnectTimeoutMs();
        int socketTimeout = dorisConfig.getRequestReadTimeoutMs() == null ? 30000 : dorisConfig.getRequestReadTimeoutMs();
        int retries = dorisConfig.getRequestRetries() == null ? 3 : dorisConfig.getRequestRetries();
        logger.trace("connect timeout set to '{}'. socket timeout set to '{}'. retries set to '{}'.", connectTimeout, socketTimeout, retries);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
        request.setConfig(requestConfig);
        logger.info("Send request to Doris FE '{}' with user '{}'.", (Object)request.getURI(), (Object)dorisConfig.getUsername());
        IOException ex = null;
        int statusCode = -1;
        for (int attempt = 0; attempt < retries; ++attempt) {
            logger.debug("Attempt {} to request {}.", (Object)attempt, (Object)request.getURI());
            try {
                String response = request instanceof HttpGet ? RestService.getConnectionGet(request.getURI().toString(), dorisConfig.getUsername(), dorisConfig.getPassword(), logger) : RestService.getConnectionPost(request, dorisConfig.getUsername(), dorisConfig.getPassword(), logger);
                if (response != null) {
                    logger.trace("Success get response from Doris FE: {}, response is: {}.", (Object)request.getURI(), (Object)response);
                    ObjectMapper mapper = new ObjectMapper();
                    Map map = mapper.readValue(response, Map.class);
                    if (map.containsKey("code") && map.containsKey("msg")) {
                        Object data = map.get("data");
                        return mapper.writeValueAsString(data);
                    }
                    return response;
                }
                logger.warn("Failed to get response from Doris FE {}, http code is {}", (Object)request.getURI(), (Object)statusCode);
                continue;
            }
            catch (IOException e) {
                ex = e;
                logger.warn("Connect to doris {} failed.", (Object)request.getURI(), (Object)e);
            }
        }
        String errMsg = "Connect to " + request.getURI().toString() + "failed, status code is " + statusCode + ".";
        throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, ex);
    }

    private static String getConnectionPost(HttpRequestBase request, String user, String passwd, Logger logger) throws IOException {
        URL url = new URL(request.getURI().toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(request.getMethod());
        String authEncoding = Base64.getEncoder().encodeToString(String.format("%s:%s", user, passwd).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + authEncoding);
        InputStream content = ((HttpPost)request).getEntity().getContent();
        String res = IOUtils.toString(content);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        PrintWriter out = new PrintWriter(conn.getOutputStream());
        out.print(res);
        out.flush();
        return RestService.parseResponse(conn, logger);
    }

    private static String getConnectionGet(String request, String user, String passwd, Logger logger) throws IOException {
        URL realUrl = new URL(request);
        HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
        String authEncoding = Base64.getEncoder().encodeToString(String.format("%s:%s", user, passwd).getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + authEncoding);
        connection.connect();
        return RestService.parseResponse(connection, logger);
    }

    private static String parseResponse(HttpURLConnection connection, Logger logger) throws IOException {
        if (connection.getResponseCode() != 200) {
            logger.warn("Failed to get response from Doris  {}, http code is {}", (Object)connection.getURL(), (Object)connection.getResponseCode());
            throw new IOException("Failed to get response from Doris");
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader in = null;){
            String line;
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    @VisibleForTesting
    static String[] parseIdentifier(String tableIdentifier, Logger logger) throws DorisConnectorException {
        logger.trace("Parse identifier '{}'.", (Object)tableIdentifier);
        if (StringUtils.isEmpty(tableIdentifier)) {
            String errMsg = String.format("argument '{}' is illegal, value is '{}'.", "table.identifier", tableIdentifier);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        String[] identifier = tableIdentifier.split("\\.");
        if (identifier.length != 2) {
            String errMsg = String.format("argument '{}' is illegal, value is '{}'.", "table.identifier", tableIdentifier);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        return identifier;
    }

    @VisibleForTesting
    static String randomEndpoint(String feNodes, Logger logger) throws DorisConnectorException {
        logger.trace("Parse fenodes '{}'.", (Object)feNodes);
        if (StringUtils.isEmpty(feNodes)) {
            String errMsg = String.format("argument '{}' is illegal, value is '{}'.", "fenodes", feNodes);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        List<String> nodes = Arrays.asList(feNodes.split(","));
        Collections.shuffle(nodes);
        return nodes.get(0).trim();
    }

    @VisibleForTesting
    static List<String> allEndpoints(String feNodes, Logger logger) throws DorisConnectorException {
        logger.trace("Parse fenodes '{}'.", (Object)feNodes);
        if (StringUtils.isEmpty(feNodes)) {
            String errMsg = String.format("argument '{}' is illegal, value is '{}'.", "fenodes", feNodes);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        List<String> nodes = Arrays.stream(feNodes.split(",")).map(String::trim).collect(Collectors.toList());
        Collections.shuffle(nodes);
        return nodes;
    }

    @VisibleForTesting
    public static String randomBackend(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException, IOException {
        List<BackendV2.BackendRowV2> backends = RestService.getBackendsV2(dorisConfig, logger);
        logger.trace("Parse beNodes '{}'.", (Object)backends);
        if (backends == null || backends.isEmpty()) {
            logger.error("argument '{}' is illegal, value is '{}'.", (Object)"beNodes", (Object)backends);
            String errMsg = String.format("argument '{}' is illegal, value is '{}'.", "beNodes", backends);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        Collections.shuffle(backends);
        BackendV2.BackendRowV2 backend = backends.get(0);
        return backend.getIp() + ":" + backend.getHttpPort();
    }

    public static String getBackend(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException {
        try {
            return RestService.randomBackend(dorisConfig, logger);
        }
        catch (Exception e) {
            String errMsg = "Failed to get backend via " + dorisConfig.getFrontends();
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
    }

    @Deprecated
    @VisibleForTesting
    static List<BackendRow> getBackends(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException, IOException {
        String feNodes = dorisConfig.getFrontends();
        String feNode = RestService.randomEndpoint(feNodes, logger);
        String beUrl = String.format(BASE_URL, feNode, BACKENDS);
        HttpGet httpGet = new HttpGet(beUrl);
        String response = RestService.send(dorisConfig, httpGet, logger);
        logger.info("Backend Info:{}", (Object)response);
        List<BackendRow> backends = RestService.parseBackend(response, logger);
        return backends;
    }

    @Deprecated
    static List<BackendRow> parseBackend(String response, Logger logger) throws DorisConnectorException, IOException {
        Backend backend;
        ObjectMapper mapper = new ObjectMapper();
        try {
            backend = mapper.readValue(response, Backend.class);
        }
        catch (JsonParseException e) {
            String errMsg = "Doris BE's response is not a json. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (JsonMappingException e) {
            String errMsg = "Doris BE's response cannot map to schema. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (IOException e) {
            String errMsg = "Parse Doris BE's response to json failed. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        if (backend == null) {
            logger.error("Should not come here.");
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, "Should not come here.");
        }
        List<BackendRow> backendRows = backend.getRows().stream().filter(v -> v.getAlive()).collect(Collectors.toList());
        logger.debug("Parsing schema result is '{}'.", (Object)backendRows);
        return backendRows;
    }

    @VisibleForTesting
    public static List<BackendV2.BackendRowV2> getBackendsV2(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException, IOException {
        String feNodes = dorisConfig.getFrontends();
        List<String> feNodeList = RestService.allEndpoints(feNodes, logger);
        for (String feNode : feNodeList) {
            try {
                String beUrl = "http://" + feNode + BACKENDS_V2;
                HttpGet httpGet = new HttpGet(beUrl);
                String response = RestService.send(dorisConfig, httpGet, logger);
                logger.info("Backend Info:{}", (Object)response);
                List<BackendV2.BackendRowV2> backends = RestService.parseBackendV2(response, logger);
                return backends;
            }
            catch (DorisConnectorException e) {
                logger.info("Doris FE node {} is unavailable: {}, Request the next Doris FE node", (Object)feNode, (Object)e.getMessage());
            }
        }
        String errMsg = "No Doris FE is available, please check configuration";
        throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
    }

    static List<BackendV2.BackendRowV2> parseBackendV2(String response, Logger logger) throws DorisConnectorException, IOException {
        BackendV2 backend;
        ObjectMapper mapper = new ObjectMapper();
        try {
            backend = mapper.readValue(response, BackendV2.class);
        }
        catch (JsonParseException e) {
            String errMsg = "Doris BE's response is not a json. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (JsonMappingException e) {
            String errMsg = "Doris BE's response cannot map to schema. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (IOException e) {
            String errMsg = "Parse Doris BE's response to json failed. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        if (backend == null) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, "Should not come here.");
        }
        List<BackendV2.BackendRowV2> backendRows = backend.getBackends();
        logger.debug("Parsing schema result is '{}'.", (Object)backendRows);
        return backendRows;
    }

    @VisibleForTesting
    static String getUriStr(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException {
        String[] identifier = RestService.parseIdentifier(dorisConfig.getTableIdentifier(), logger);
        return "http://" + RestService.randomEndpoint(dorisConfig.getFrontends(), logger) + API_PREFIX + "/" + identifier[0] + "/" + identifier[1] + "/";
    }

    public static Schema getSchema(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException {
        logger.trace("Finding schema.");
        HttpGet httpGet = new HttpGet(RestService.getUriStr(dorisConfig, logger) + SCHEMA);
        String response = RestService.send(dorisConfig, httpGet, logger);
        logger.debug("Find schema response is '{}'.", (Object)response);
        return RestService.parseSchema(response, logger);
    }

    public static boolean isUniqueKeyType(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException {
        try {
            return UNIQUE_KEYS_TYPE.equals(RestService.getSchema(dorisConfig, logger).getKeysType());
        }
        catch (Exception e) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, (Throwable)e);
        }
    }

    @VisibleForTesting
    public static Schema parseSchema(String response, Logger logger) throws DorisConnectorException {
        Schema schema;
        logger.trace("Parse response '{}' to schema.", (Object)response);
        ObjectMapper mapper = new ObjectMapper();
        try {
            schema = mapper.readValue(response, Schema.class);
        }
        catch (JsonParseException e) {
            String errMsg = "Doris FE's response is not a json. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (JsonMappingException e) {
            String errMsg = "Doris FE's response cannot map to schema. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (IOException e) {
            String errMsg = "Parse Doris FE's response to json failed. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        if (schema == null) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, "Should not come here.");
        }
        if (schema.getStatus() != 200) {
            String errMsg = "Doris FE's response is not OK, status is " + schema.getStatus();
            logger.error(errMsg);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        logger.debug("Parsing schema result is '{}'.", (Object)schema);
        return schema;
    }

    public static List<PartitionDefinition> findPartitions(DorisConfig dorisConfig, Logger logger) throws DorisConnectorException {
        String[] tableIdentifiers = RestService.parseIdentifier(dorisConfig.getTableIdentifier(), logger);
        String readFields = StringUtils.isBlank(dorisConfig.getReadField()) ? "*" : dorisConfig.getReadField();
        String sql = "select " + readFields + " from `" + tableIdentifiers[0] + "`.`" + tableIdentifiers[1] + "`";
        if (!StringUtils.isEmpty(dorisConfig.getFilterQuery())) {
            sql = sql + " where " + dorisConfig.getFilterQuery();
        }
        logger.debug("Query SQL Sending to Doris FE is: '{}'.", (Object)sql);
        HttpPost httpPost = new HttpPost(RestService.getUriStr(dorisConfig, logger) + QUERY_PLAN);
        String entity = "{\"sql\": \"" + sql + "\"}";
        logger.debug("Post body Sending to Doris FE is: '{}'.", (Object)entity);
        StringEntity stringEntity = new StringEntity(entity, StandardCharsets.UTF_8);
        stringEntity.setContentEncoding("UTF-8");
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);
        String resStr = RestService.send(dorisConfig, httpPost, logger);
        logger.debug("Find partition response is '{}'.", (Object)resStr);
        QueryPlan queryPlan = RestService.getQueryPlan(resStr, logger);
        Map<String, List<Long>> be2Tablets = RestService.selectBeForTablet(queryPlan, logger);
        return RestService.tabletsMapToPartition(dorisConfig, be2Tablets, queryPlan.getOpaquedQueryPlan(), tableIdentifiers[0], tableIdentifiers[1], logger);
    }

    @VisibleForTesting
    static QueryPlan getQueryPlan(String response, Logger logger) throws DorisConnectorException {
        QueryPlan queryPlan;
        ObjectMapper mapper = new ObjectMapper();
        try {
            queryPlan = mapper.readValue(response, QueryPlan.class);
        }
        catch (JsonParseException e) {
            String errMsg = "Doris FE's response is not a json. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (JsonMappingException e) {
            String errMsg = "Doris FE's response cannot map to schema. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        catch (IOException e) {
            String errMsg = "Parse Doris FE's response to json failed. res: " + response;
            logger.error(errMsg, e);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
        }
        if (queryPlan == null) {
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, "Should not come here.");
        }
        if (queryPlan.getStatus() != 200) {
            String errMsg = "Doris FE's response is not OK, status is " + queryPlan.getStatus();
            logger.error(errMsg);
            throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
        }
        logger.debug("Parsing partition result is '{}'.", (Object)queryPlan);
        return queryPlan;
    }

    @VisibleForTesting
    static Map<String, List<Long>> selectBeForTablet(QueryPlan queryPlan, Logger logger) throws DorisConnectorException {
        HashMap<String, List<Long>> be2Tablets = new HashMap<String, List<Long>>();
        for (Map.Entry<String, Tablet> part : queryPlan.getPartitions().entrySet()) {
            long tabletId;
            logger.debug("Parse tablet info: '{}'.", (Object)part);
            try {
                tabletId = Long.parseLong(part.getKey());
            }
            catch (NumberFormatException e) {
                String errMsg = "Parse tablet id '" + part.getKey() + "' to long failed.";
                logger.error(errMsg, e);
                throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg, e);
            }
            String target = null;
            int tabletCount = Integer.MAX_VALUE;
            for (String candidate : part.getValue().getRoutings()) {
                logger.trace("Evaluate Doris BE '{}' to tablet '{}'.", (Object)candidate, (Object)tabletId);
                if (!be2Tablets.containsKey(candidate)) {
                    logger.debug("Choice a new Doris BE '{}' for tablet '{}'.", (Object)candidate, (Object)tabletId);
                    ArrayList tablets = new ArrayList();
                    be2Tablets.put(candidate, tablets);
                    target = candidate;
                    break;
                }
                if (((List)be2Tablets.get(candidate)).size() >= tabletCount) continue;
                target = candidate;
                tabletCount = ((List)be2Tablets.get(candidate)).size();
                logger.debug("Current candidate Doris BE to tablet '{}' is '{}' with tablet count {}.", tabletId, target, tabletCount);
            }
            if (target == null) {
                String errMsg = "Cannot choice Doris BE for tablet " + tabletId;
                logger.error(errMsg);
                throw new DorisConnectorException((SeaTunnelErrorCode)DorisConnectorErrorCode.REST_SERVICE_FAILED, errMsg);
            }
            logger.debug("Choice Doris BE '{}' for tablet '{}'.", (Object)target, (Object)tabletId);
            ((List)be2Tablets.get(target)).add(tabletId);
        }
        return be2Tablets;
    }

    @VisibleForTesting
    static int tabletCountLimitForOnePartition(DorisConfig dorisConfig, Logger logger) {
        int tabletsSize = Integer.MAX_VALUE;
        if (dorisConfig.getTabletSize() != null) {
            tabletsSize = dorisConfig.getTabletSize();
        }
        if (tabletsSize < 1) {
            logger.warn("{} is less than {}, set to default value {}.", DorisConfig.DORIS_TABLET_SIZE, 1, 1);
            tabletsSize = 1;
        }
        logger.debug("Tablet size is set to {}.", (Object)tabletsSize);
        return tabletsSize;
    }

    @VisibleForTesting
    static List<PartitionDefinition> tabletsMapToPartition(DorisConfig dorisConfig, Map<String, List<Long>> be2Tablets, String opaquedQueryPlan, String database, String table, Logger logger) throws DorisConnectorException {
        int tabletsSize = RestService.tabletCountLimitForOnePartition(dorisConfig, logger);
        ArrayList<PartitionDefinition> partitions = new ArrayList<PartitionDefinition>();
        for (Map.Entry<String, List<Long>> beInfo : be2Tablets.entrySet()) {
            logger.debug("Generate partition with beInfo: '{}'.", (Object)beInfo);
            HashSet tabletSet = new HashSet(beInfo.getValue());
            beInfo.getValue().clear();
            beInfo.getValue().addAll(tabletSet);
            for (int first = 0; first < beInfo.getValue().size(); first += tabletsSize) {
                HashSet<Long> partitionTablets = new HashSet<Long>(beInfo.getValue().subList(first, Math.min(beInfo.getValue().size(), first + tabletsSize)));
                PartitionDefinition partitionDefinition = new PartitionDefinition(database, table, beInfo.getKey(), partitionTablets, opaquedQueryPlan);
                logger.debug("Generate one PartitionDefinition '{}'.", (Object)partitionDefinition);
                partitions.add(partitionDefinition);
            }
        }
        return partitions;
    }
}

