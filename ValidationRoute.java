package com.banamex.acmt.psk.kcp.dual.own.account.information.route;

import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.*;
import com.banamex.acmt.psk.kcp.dual.own.account.information.process.*;
import com.banamex.acmt.psk.kcp.dual.own.account.information.utilities.AvroDynamic;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.InputStream;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

/**
 * Route for validating and transforming request messages.
 * 
 * <p><strong>Flow:</strong>
 * <ol>
 *   <li>Channel and operation selection</li>
 *   <li>Input headers validation</li>
 *   <li>Avro deserialization to JSON</li>
 *   <li>Preserve original headers</li>
 *   <li>JSON Schema validation</li>
 *   <li>JSONata transformation</li>
 *   <li>JSON to Avro serialization</li>
 *   <li>Clean headers for output</li>
 * </ol>
 * 
 * <p><strong>Error Handling:</strong>
 * Catches multiple exception types and delegates to GlobalDlqRoute for processing.
 * This avoids code duplication and maintains separation of concerns.
 * 
 * @author jd35030
 * @version 1.0.0
 */
@Slf4j
@Component
public class ValidationRoute extends RouteBuilder {

    private final ChannelSelectorProcessor channelSelectorProcessor;
    private final OperationSelectorProcessor operationSelectorProcessor;
    private final ValidInputHeaders validInputHeaders;
    private final JsonToBytes jsonToBytes;
    private final CleanHeadersProcessor cleanHeadersProcessor;

    public ValidationRoute(ChannelSelectorProcessor channelSelectorProcessor,
                          OperationSelectorProcessor operationSelectorProcessor,
                          ValidInputHeaders validInputHeaders,
                          JsonToBytes jsonToBytes,
                          CleanHeadersProcessor cleanHeadersProcessor) {
        this.channelSelectorProcessor = channelSelectorProcessor;
        this.operationSelectorProcessor = operationSelectorProcessor;
        this.validInputHeaders = validInputHeaders;
        this.jsonToBytes = jsonToBytes;
        this.cleanHeadersProcessor = cleanHeadersProcessor;
    }

    @Override
    public void configure() throws Exception {

        // ========== ERROR HANDLING ==========
        // Catches all validation and transformation errors
        // Delegates to GlobalDlqRoute for standardized error processing
        onException(
                JsonValidationException.class,
                JsonParseException.class,
                InvalidFormatException.class,
                MismatchedInputException.class,
                ValueInstantiationException.class,
                UnrecognizedPropertyException.class,
                AvroRuntimeException.class,
                AvroTypeException.class,
                DateTimeParseException.class,
                Exception.class)
            .handled(true)
            .process(exchange -> {
                Exception ex = exchange.getProperty(
                    org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception.class);
                
                log.error("Exception caught in ValidationRoute: {} - Message: {}", 
                         ex != null ? ex.getClass().getSimpleName() : "Unknown",
                         ex != null ? ex.getMessage() : "No message", ex);
            })
            .to(RouteConstants.ERROR_HANDLER_RTE)  // Delegates to GlobalDlqRoute
            .end();

        // ========== VALIDATION AND TRANSFORMATION FLOW ==========
        from(RouteConstants.DIRECT_VALIDATION)
            .routeId(RouteConstants.VALIDATION_ROUTE_ID)
            
            // Step 1: Channel and operation selection
            .process(channelSelectorProcessor)
            .process(operationSelectorProcessor)
            
            // Step 2: Validate input headers
            .process(validInputHeaders)
            
            // Step 3: Deserialize Avro to JSON
            .process(exchange -> {
                
                // Get operation configuration to retrieve Avro schema
                AppConfig.Operation operation = exchange.getProperty(
                    GeneralConstants.PROPERTY_OPERATION_CONFIGURATION,
                    AppConfig.Operation.class);
                
                String schemaPath = GeneralConstants.SCHEMAS_DIRECTORY
                    .concat(operation.jsonavro());
                
                log.debug("Reading Avro schema from: {}", schemaPath);
                
                // Load Avro schema
                Schema schema;
                String fileNotFound = GeneralConstants.ERROR_SCHEMA_NOT_FOUND
                    .concat(operation.jsonavro());
                
                try (InputStream in = Objects.requireNonNull(
                        getClass().getResourceAsStream(schemaPath), fileNotFound)) {
                    schema = new Schema.Parser().parse(in);
                }
                
                // Deserialize Avro bytes to JSON string
                byte[] avroBytes = exchange.getIn().getBody(byte[].class);
                String json = AvroDynamic.avroBytesToJsonString(avroBytes, schema);
                
                exchange.getIn().setBody(json);
                
                log.debug("Avro successfully deserialized to JSON for schema: {}", 
                         operation.jsonavro());
            })
            
            // Step 4: Preserve original headers before transformation
            .process(exchange -> {
                Map<String, Object> originalHeaders = new LinkedHashMap<>(
                    exchange.getIn().getHeaders());
                exchange.setProperty(
                    GeneralConstants.PROPERTY_ORIGINAL_CAMEL_CASE_HEADERS, 
                    originalHeaders);
                
                // Set JSONata transformation property
                exchange.setProperty(
                    GeneralConstants.PROPERTY_JSON_ATA_NAME,
                    GeneralConstants.PROPERTY_JSON_ATA_VALUE);
                
                log.debug("Original headers preserved, count: {}", originalHeaders.size());
            })
            
            // Step 5: JSON Schema validation
            .to(GeneralConstants.JSON_VALIDATOR_SCHEMA)
            .log("JSON Schema validation passed")
            
            // Step 6: Unmarshal JSON
            .unmarshal().json(JsonLibrary.Jackson)
            
            // Step 7: JSONata transformation
            .toD(GeneralConstants.JSONATA_SCHEMA_EXCHANGE)
            .log("JSONata transformation completed")
            
            // Step 8: Marshal back to JSON
            .marshal().json(JsonLibrary.Jackson)
            
            // Step 9: Prepare for Avro serialization
            .process(exchange -> {
                // Extract Avro schema filename from operation config
                AppConfig.Operation operation = exchange.getProperty(
                    GeneralConstants.PROPERTY_OPERATION_CONFIGURATION,
                    AppConfig.Operation.class);
                
                String avroPath = GeneralConstants.SCHEMAS_DIRECTORY
                    .concat(operation.jsonavro());
                
                String sendFileName = avroPath.substring(
                    avroPath.lastIndexOf(GeneralConstants.SLASH) + GeneralConstants.INT);
                
                exchange.setProperty(
                    GeneralConstants.SEND_FILE_NAME_PROPERTY, sendFileName);
                
                log.debug("Target Avro schema set: {}", sendFileName);
            })
            
            // Step 10: Serialize JSON to Avro bytes
            .process(jsonToBytes)
            
            // Step 11: Set output message schema for routing
            .setProperty(RouteConstants.OUTPUT_MESSAGE_SCHEMA_PROPERTY,
                simple("${exchangeProperty.operationConfiguration.messageSchema}"))
            
            // Step 12: Clean headers for output
            .process(cleanHeadersProcessor)
            
            // Final logging
            .process(exchange -> {
                log.info("Message validated and transformed successfully. Schema: {}", 
                        exchange.getProperty(RouteConstants.OUTPUT_MESSAGE_SCHEMA_PROPERTY));
                log.debug("Output headers: {}", exchange.getIn().getHeaders());
            });
    }
}