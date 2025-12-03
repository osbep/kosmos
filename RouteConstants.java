package com.banamex.acmt.psk.kcp.dual.own.account.information.constant;

/**
 * Constants for Camel routes configuration.
 * Follows the pattern from movements microservice.
 * 
 * @author jd35030
 * @version 1.0.0
 */
public final class RouteConstants {

    private RouteConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========== ROUTE IDs ==========
    public static final String LOAD_MESSAGE_ROUTE = "loadMessageRoute";
    public static final String VALIDATION_ROUTE_ID = "validationRoute";
    public static final String COMMIT_ROUTE_ID = "commitRoute";
    public static final String COMPLETE_PROCESS_ROUTE_ID = "completeProcessRoute";
    public static final String LOAD_EPORTAL_RESPONSE_ROUTE = "loadEPortalResponseRoute";
    public static final String RESPONSE_VALIDATION_ROUTE_ID = "responseValidationRoute";
    public static final String RESPONSE_COMMIT_ROUTE_ID = "responseCommitRoute";
    public static final String RESPONSE_COMPLETE_PROCESS_ROUTE_ID = "responseCompleteProcessRoute";
    public static final String ERROR_HANDLER_RTE_ID = "sendAccountInformationDlq";

    // ========== DIRECT ENDPOINTS ==========
    public static final String DIRECT_VALIDATION = "direct:validation";
    public static final String DIRECT_COMMIT_ROUTE = "direct:commitRoute";
    public static final String DIRECT_COMPLETE_PROCESS_ROUTE = "direct:complete-process";
    public static final String DIRECT_RESPONSE_VALIDATION = "direct:responseValidation";
    public static final String DIRECT_RESPONSE_COMMIT_ROUTE = "direct:responseCommitRoute";
    public static final String DIRECT_RESPONSE_COMPLETE_PROCESS_ROUTE = "direct:responseCompleteProcess";
    public static final String ERROR_HANDLER_RTE = "direct:global-dlq";

    // ========== KAFKA CONSUMER TOPICS (REQUEST FLOW) ==========
    public static final String RECEIVE_KAFKA_TOPIC_ACCOUNT_MANAGEMENT = 
        "kafka:{{app.channel.BNE.operations.requestPayerCustomerOwnAccountRetrieve.topics.name}}"
        + "?groupId={{app.channel.BNE.operations.requestPayerCustomerOwnAccountRetrieve.groupId}}";
    
    public static final String RECEIVE_KAFKA_TOPIC_CUSTOMER_MANAGEMENT = 
        "kafka:{{app.channel.BNE.operations.requestPayeeCustomerOwnAccountContractRetrieve.topics.name}}"
        + "?groupId={{app.channel.BNE.operations.requestPayeeCustomerOwnAccountContractRetrieve.groupId}}";

    // ========== KAFKA PRODUCER TOPICS (TO EPORTAL) ==========
    public static final String SEND_TO_EPORTAL_TOPIC_JRD = 
        "kafka:{{app.channel.BNE.operations.requestOwnAccountInformationPayerBeS016.topics.name.jrd}}";
    
    public static final String SEND_TO_EPORTAL_TOPIC_QRO = 
        "kafka:{{app.channel.BNE.operations.requestOwnAccountInformationPayerBeS016.topics.name.qro}}";

    // ========== KAFKA CONSUMER TOPICS (RESPONSE FLOW) ==========
    public static final String RECEIVE_EPORTAL_RESPONSE_TOPIC_JRD = 
        "kafka:{{app.channel.BNE.operations.responseOwnAccountInformationPayerBeS016.topics.name.jrd}}"
        + "?groupId={{app.channel.BNE.operations.responseOwnAccountInformationPayerBeS016.groupId}}";
    
    public static final String RECEIVE_EPORTAL_RESPONSE_TOPIC_QRO = 
        "kafka:{{app.channel.BNE.operations.responseOwnAccountInformationPayerBeS016.topics.name.qro}}"
        + "?groupId={{app.channel.BNE.operations.responseOwnAccountInformationPayerBeS016.groupId}}";

    // ========== KAFKA PRODUCER TOPICS (TO ORCHESTRATOR) ==========
    public static final String SEND_FINAL_RESPONSE_TOPIC_JRD = 
        "kafka:{{app.channel.BNE.operations.responsePayerCustomerOwnAccountRetrieve.topics.name.jrd}}";
    
    public static final String SEND_FINAL_RESPONSE_TOPIC_QRO = 
        "kafka:{{app.channel.BNE.operations.responsePayerCustomerOwnAccountRetrieve.topics.name.qro}}";

    // ========== DLQ TOPICS ==========
    public static final String ACCOUNT_INFORMATION_DLQ_JRD = 
        "kafka:{{app.channel.BNE.operations.sendAccountInformationDlqCreate.topics.name.jrd}}";
    
    public static final String ACCOUNT_INFORMATION_DLQ_QRO = 
        "kafka:{{app.channel.BNE.operations.sendAccountInformationDlqCreate.topics.name.qro}}";

    // ========== KAFKA CONFIGURATION ==========
    public static final String MANUAL_COMMIT = "?allowManualCommit=true";
    public static final String AUTO_COMMIT_ENABLE = "&autoCommitEnable=false";
    public static final String AUTO_OFFSET_RESET = "&autoOffsetReset=earliest";
    public static final String APACHE_KAFKA_COMMON_SERIALIZATION_BYTE_ARRAY_DESERIALIZER = 
        "&valueDeserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer";
    public static final String APACHE_KAFKA_COMMON_SERIALIZATION_STRING_DESERIALIZER = 
        "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer";
    public static final String SEND_MESSAGE_ADDITIONAL_PROPERTIES = 
        "?requestRequiredAcks=all&idempotence=true&maxInFlightRequest=1";
    public static final String KAFKA_COMMON_SERIALIZATION_BYTE_ARRAY_SERIALIZER = 
        "&valueSerializer=org.apache.kafka.common.serialization.ByteArraySerializer";
    public static final String APACHE_KAFKA_COMMON_SERIALIZATION_STRING_SERIALIZER = 
        "&keySerializer=org.apache.kafka.common.serialization.StringSerializer";

    // ========== SEDA CONFIGURATION ==========
    public static final String SEDA_ROUTE_TO = 
        "seda:processAccountInformation?waitForTaskToComplete=Always&timeout=30000" +
        "&blockWhenFull=true&concurrentConsumers=8&size=10";
    
    public static final String SEDA_ROUTE_FROM = "seda:processAccountInformation?concurrentConsumers=8";
    
    public static final String SEDA_RESPONSE_ROUTE_TO = 
        "seda:processEPortalResponse?waitForTaskToComplete=Always&timeout=30000" +
        "&blockWhenFull=true&concurrentConsumers=8&size=10";
    
    public static final String SEDA_RESPONSE_ROUTE_FROM = "seda:processEPortalResponse?concurrentConsumers=8";

    // ========== EXCHANGE PROPERTIES ==========
    public static final String COMMIT_MANUAL_SAVED = "commitManualSaved";
    public static final String HEADER_INPUT_TOPIC = "inputTopic";
    public static final String OUTPUT_MESSAGE_SCHEMA_PROPERTY = "outputMessageSchema";
    public static final String FILE_NAME_AVSC = "fileNameAvsc";
    public static final String PROPERTY_CHANNEL_CONFIGURATION = "channelConfiguration";
    public static final String PROPERTY_OPERATION_CONFIGURATION = "operationConfiguration";

    // ========== HEADERS ==========
    public static final String CHANNEL_ID_HEADER = "channelId";
    public static final String MESSAGE_SCHEMA = "messageSchema";

    // ========== DATACENTER IDENTIFIERS ==========
    public static final String JRD = "jrd";
    public static final String QRO = "qro";
}