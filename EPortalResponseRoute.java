package com.banamex.acmt.psk.kcp.dual.own.account.information.route;

import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.ConditionalConstants;
import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.GeneralConstants;
import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.RouteConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.KafkaManualCommit;
import org.springframework.stereotype.Component;

/**
 * Route for consuming response messages from EPortal via Kafka.
 * 
 * <p><strong>Responsibilities:</strong>
 * <ul>
 *   <li>Consume response messages from EPortal topics (JRD and QRO)</li>
 *   <li>Filter messages by messageSchema header</li>
 *   <li>Store KafkaManualCommit for later execution</li>
 *   <li>Store input topic information for logging and routing</li>
 *   <li>Delegate to SEDA queue for asynchronous processing</li>
 *   <li>Route to ResponseValidationRoute for validation and transformation</li>
 * </ul>
 * 
 * <p><strong>Flow:</strong>
 * <pre>
 * EPortal (JRD/QRO) → Kafka Topic
 *     ↓
 * EPortalResponseRoute (consumer)
 *     ↓ (filter by messageSchema)
 *     ↓ (store KafkaManualCommit + topic)
 *     ↓
 * SEDA Queue (async, 8 consumers)
 *     ↓
 * ResponseValidationRoute → ResponseCommitRoute → Orchestrator
 *     ↓ (on success)
 * CompleteProcessRoute (manual commit)
 * </pre>
 * 
 * <p><strong>Manual Commit:</strong>
 * This route enables manual commit to ensure at-least-once delivery semantics.
 * The Kafka offset is only committed after successful message processing in
 * CompleteProcessRoute, preventing message loss in case of errors.
 * 
 * <p><strong>SEDA Queue Pattern:</strong>
 * Uses two-part pattern for decoupling Kafka consumer from processing:
 * <ul>
 *   <li>Part 1: Kafka consumer → SEDA producer (lightweight, fast)</li>
 *   <li>Part 2: SEDA consumer → Processing pipeline (can be slow)</li>
 * </ul>
 * 
 * @author jd35030
 * @version 2.0.0
 * @see ResponseValidationRoute
 * @see ResponseCommitRoute
 * @see CompleteProcessRoute
 */
@Slf4j
@Component
public class EPortalResponseRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // ========== PART 1: KAFKA CONSUMER TO SEDA ==========
        // Consumes from two topics (JRD and QRO) simultaneously
        // Lightweight processing: filter, store properties, send to SEDA
        
        from(RouteConstants.RECEIVE_EPORTAL_RESPONSE_TOPIC_JRD + ","
            + RouteConstants.RECEIVE_EPORTAL_RESPONSE_TOPIC_QRO
            + RouteConstants.MANUAL_COMMIT                    // Enable manual commit
            + RouteConstants.AUTO_COMMIT_ENABLE               // Disable auto-commit
            + RouteConstants.AUTO_OFFSET_RESET                // Start from earliest
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_BYTE_ARRAY_DESERIALIZER
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_STRING_DESERIALIZER)
            .routeId(RouteConstants.LOAD_EPORTAL_RESPONSE_ROUTE)
            
            // Log message reception
            .process(exchange -> {
                String topic = exchange.getIn().getHeader(KafkaConstants.TOPIC, String.class);
                String partition = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.PARTITION, Integer.class));
                String offset = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.OFFSET, Long.class));
                
                log.info("Response message received from EPortal - Topic: {}, Partition: {}, Offset: {}", 
                        topic, partition, offset);
                log.debug("Response message headers: {}", exchange.getIn().getHeaders());
            })
            
            // Filter by messageSchema header
            // Only process messages with valid messageSchema
            .filter().simple(ConditionalConstants.MESSAGE_SCHEMA_FILTER)
            
            // Log after filter
            .process(exchange -> {
                String messageSchema = exchange.getIn().getHeader(
                    GeneralConstants.MESSAGE_SCHEMA_RECEIVED_HEADER, String.class);
                log.debug("Response message passed filter - messageSchema: {}", messageSchema);
            })
            
            // Store KafkaManualCommit and topic information in exchange properties
            // Properties persist throughout the entire message processing pipeline
            .process(exchange -> {
                
                // Step 1: Store KafkaManualCommit for later execution in CompleteProcessRoute
                KafkaManualCommit manualCommit = exchange.getIn().getHeader(
                    KafkaConstants.MANUAL_COMMIT, 
                    KafkaManualCommit.class);
                
                if (manualCommit != null) {
                    exchange.setProperty(
                        GeneralConstants.KAFKA_MANUAL_COMMIT_PROPERTY, 
                        manualCommit);
                    log.debug("KafkaManualCommit stored in exchange property");
                } else {
                    log.warn("KafkaManualCommit not found in message headers. " +
                            "Manual commit will not be available.");
                }
                
                // Step 2: Store input topic for logging and routing decisions
                String topic = exchange.getIn().getHeader(
                    KafkaConstants.TOPIC, String.class);
                
                if (topic != null) {
                    exchange.setProperty(RouteConstants.HEADER_INPUT_TOPIC, topic);
                    log.debug("Input topic stored: {}", topic);
                } else {
                    log.warn("Topic header not found in message");
                }
                
                log.info("Response message properties stored successfully");
            })
            
            // Send to SEDA queue for asynchronous processing
            // This decouples Kafka consumption from message processing
            .to(RouteConstants.SEDA_RESPONSE_ROUTE_TO)
            
            .process(exchange -> 
                log.debug("Response message sent to SEDA queue"));

        // ========== PART 2: SEDA CONSUMER TO PROCESSING PIPELINE ==========
        // Processes messages from SEDA queue with 8 concurrent consumers
        // Delegates to ResponseValidationRoute for validation and transformation
        
        from(RouteConstants.SEDA_RESPONSE_ROUTE_FROM)
            .routeId(RouteConstants.LOAD_EPORTAL_RESPONSE_ROUTE + "-processor")
            
            .process(exchange -> 
                log.debug("Response message consumed from SEDA queue"))
            
            // Step 1: Validate and transform response message
            // Deserializes Avro, validates schema, transforms with JSONata
            .to(RouteConstants.DIRECT_RESPONSE_VALIDATION)
            
            .process(exchange -> 
                log.debug("Response validation completed successfully"))
            
            // Step 2: Send transformed message to Orchestrator
            // Routes to appropriate topic (JRD or QRO) based on input
            .to(RouteConstants.DIRECT_RESPONSE_COMMIT_ROUTE)
            
            .process(exchange -> 
                log.info("Response processing pipeline completed successfully"));
    }
}