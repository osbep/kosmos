package com.banamex.acmt.psk.kcp.dual.own.account.information.constant;

/**
 * Centralized application-wide constant values.
 * 
 * @author jd35030
 * @version 1.0.0
 */
public final class GeneralConstants {

    // ========== PACKAGE PATTERNS (for MDC/logging) ==========
    
    public static final String BANAMEX_PACKAGE =
            "com.banamex.acmt.psk.kcp.dual.own.account.information.*";
    public static final String CITI_PACKAGE = "com.citi.*";
    public static final String CITIBANAMEX_PACKAGE = "com.citibanamex.*";

    // ========== JSON VALIDATOR & JSONATA ==========
    
    public static final String JSON_VALIDATOR_SCHEMA =
            "json-validator:schemas/requestPayerCustomerOwnAccountRetrieve.json";
    public static final String JSONATA_SCHEMA_EXCHANGE =
            "jsonata:schemas/${exchangeProperty.jsonAta}";

    // ========== EXCHANGE PROPERTIES ==========
    
    public static final String PROPERTY_JSON_ATA_NAME = "jsonAta";
    public static final String PROPERTY_JSON_ATA_VALUE = "ataRequestPayerCustomerOwnAccountRetrieve.json";
    public static final String PROPERTY_ORIGINAL_CAMEL_CASE_HEADERS = "originalCamelCaseHeaders";
    public static final String STATUS_PROPERTY = "status";
    public static final String STATUS_JSON_MODE = "json";
    public static final String SEND_FILE_NAME_PROPERTY = "fileName";
    
    /**
     * Property name for storing channel configuration.
     */
    public static final String PROPERTY_CHANNEL_CONFIGURATION = "channelConfiguration";
    
    /**
     * Property name for storing operation configuration.
     */
    public static final String PROPERTY_OPERATION_CONFIGURATION = "operationConfiguration";

    // ========== HEADER NAMES ==========
    
    public static final String MESSAGE_SCHEMA = "messageSchema";
    public static final String CONTENT_HASH = "contentHash";
    
    /**
     * Header name for channelId.
     */
    public static final String CHANNEL_ID_HEADER = "channelId";

    // ========== REGEX PATTERNS ==========
    
    public static final String DECIMAL_REGEX = "^-?\\d+(?:\\.\\d+)?$";
    public static final String BASE64_REGEX =
            "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";

    // ========== ERROR MESSAGES (JsonToBytes) ==========
    
    public static final String ERROR_SCHEMA_NOT_FOUND = "Schema not found: ";
    public static final String ERROR_NULL_UNION = "null not allowed for this union";
    public static final String ERROR_NO_SUITABLE_UNION = "No suitable union branch for value: ";
    public static final String ERROR_NO_UNION_BRANCH_NAMED = "No union branch named: ";
    public static final String ERROR_EXPECTED_MAP = "Expected Map value but got: ";
    public static final String ERROR_EXPECTED_LIST = "Expected List value but got: ";
    public static final String ERROR_UNSUPPORTED_AVRO_TYPE = "Unsupported Avro type: ";
    public static final String ERROR_DECIMAL_TYPE =
            "Decimal logical type expects number/base64/byte[]/ByteBuffer but got: ";
    public static final String ERROR_BYTES_TYPE =
            "bytes type expects base64/byte[]/ByteBuffer but got: ";

    // ========== TECHNICAL CONSTANTS ==========
    
    public static final char SLASH = '/';
    public static final int INT = 1;
    public static final int OFFSET = 0;
    public static final String UNCHECKED = "unchecked";

    // ========== UUID GENERATION ==========
    
    public static final String OUTBOUND = "outbound";
    public static final char CH = '-';

    // ========== FILE EXTENSIONS ==========
    
    public static final String JSON_AVRO_FILE_EXTENSION = ".avsc";
    public static final String JSON_ATA_FILE_EXTENSION = ".jsonata";
    public static final String JSON_SCHEMA_FILE_EXTENSION = ".json";

    // ========== OPTIONAL (for future use) ==========
    
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd";
    public static final String CONTRACT_VERSION_VALUE = "1.0.0";
    public static final String APPLICATION_VND_APACHE_AVRO = "application/vnd.apache.avro";
    public static final String EMPTY_STRING = "";

    /**
     * Private constructor to prevent instantiation.
     */
    private GeneralConstants() {
        // Utility class
    }
}