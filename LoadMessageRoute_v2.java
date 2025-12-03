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
 * Route for consuming request messages from Orchestrator via Kafka.
 * 
 * <p><strong>Responsibilities:</strong>
 * <ul>
 *   <li>Consume request messages from Orchestrator topics (Account Management and Customer Management)</li>
 *   <li>Filter messages by messageSchema header</li>
 *   <li>Store KafkaManualCommit for later execution</li>
 *   <li>Store input topic information for logging and routing</li>
 *   <li>Delegate to SEDA queue for asynchronous processing</li>
 *   <li>Route to ValidationRoute for validation and transformation</li>
 * </ul>
 * 
 * <p><strong>Flow:</strong>
 * <pre>
 * Orchestrator → Kafka Topics (2 topics)
 *     ↓
 * LoadMessageRoute (2 consumers)
 *     ↓ (filter by messageSchema)
 *     ↓ (store KafkaManualCommit + topic)
 *     ↓
 * SEDA Queue (async, 8 consumers)
 *     ↓
 * ValidationRoute → CommitRoute → EPortal
 *     ↓ (on success)
 * CompleteProcessRoute (manual commit)
 * </pre>
 * 
 * <p><strong>Two Kafka Consumers:</strong>
 * This route creates two separate Kafka consumer instances:
 * <ul>
 *   <li>Consumer 1: Account Management topic (Payer queries)</li>
 *   <li>Consumer 2: Customer Management topic (Payee and Contract queries)</li>
 * </ul>
 * Both consumers feed into the same SEDA queue for unified processing.
 * 
 * <p><strong>Manual Commit:</strong>
 * This route enables manual commit to ensure at-least-once delivery semantics.
 * The Kafka offset is only committed after successful message processing in
 * CompleteProcessRoute, preventing message loss in case of errors.
 * 
 * <p><strong>SEDA Queue Pattern:</strong>
 * Uses two-part pattern for decoupling Kafka consumer from processing:
 * <ul>
 *   <li>Part 1: Kafka consumers → SEDA producer (lightweight, fast)</li>
 *   <li>Part 2: SEDA consumer → Processing pipeline (can be slow)</li>
 * </ul>
 * 
 * @author jd35030
 * @version 2.0.0
 * @see ValidationRoute
 * @see CommitRoute
 * @see CompleteProcessRoute
 */
@Slf4j
@Component
public class LoadMessageRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // ========== CONSUMER 1: ACCOUNT MANAGEMENT TOPIC ==========
        // Consumes messages for Payer account queries
        // Topic: mx.{jrd|qro}.accountManagement.requestPayerCustomerOwnAccountRetrieve
        
        from(RouteConstants.RECEIVE_KAFKA_TOPIC_ACCOUNT_MANAGEMENT
            + RouteConstants.MANUAL_COMMIT                    // Enable manual commit
            + RouteConstants.AUTO_COMMIT_ENABLE               // Disable auto-commit
            + RouteConstants.AUTO_OFFSET_RESET                // Start from earliest
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_BYTE_ARRAY_DESERIALIZER
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_STRING_DESERIALIZER)
            .routeId(RouteConstants.LOAD_MESSAGE_ROUTE + "-account")
            
            // Log message reception
            .process(exchange -> {
                String topic = exchange.getIn().getHeader(KafkaConstants.TOPIC, String.class);
                String partition = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.PARTITION, Integer.class));
                String offset = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.OFFSET, Long.class));
                
                log.info("Request message received from Orchestrator [Account Management] - " +
                        "Topic: {}, Partition: {}, Offset: {}", 
                        topic, partition, offset);
                log.debug("Request message headers: {}", exchange.getIn().getHeaders());
            })
            
            // Filter by messageSchema header
            // Only process messages with valid messageSchema
            .filter().simple(ConditionalConstants.MESSAGE_SCHEMA_FILTER)
            
            // Log after filter
            .process(exchange -> {
                String messageSchema = exchange.getIn().getHeader(
                    GeneralConstants.MESSAGE_SCHEMA_RECEIVED_HEADER, String.class);
                log.debug("Request message passed filter - messageSchema: {}", messageSchema);
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
                
                log.info("Request message properties stored successfully [Account Management]");
            })
            
            // Send to SEDA queue for asynchronous processing
            // This decouples Kafka consumption from message processing
            .to(RouteConstants.SEDA_ROUTE_TO)
            
            .process(exchange -> 
                log.debug("Request message sent to SEDA queue [Account Management]"));

        // ========== CONSUMER 2: CUSTOMER MANAGEMENT TOPIC ==========
        // Consumes messages for Payee and Contract queries
        // Topic: mx.{jrd|qro}.accountManagement.requestPayeeCustomerOwnAccountContractRetrieve
        
        from(RouteConstants.RECEIVE_KAFKA_TOPIC_CUSTOMER_MANAGEMENT
            + RouteConstants.MANUAL_COMMIT                    // Enable manual commit
            + RouteConstants.AUTO_COMMIT_ENABLE               // Disable auto-commit
            + RouteConstants.AUTO_OFFSET_RESET                // Start from earliest
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_BYTE_ARRAY_DESERIALIZER
            + RouteConstants.APACHE_KAFKA_COMMON_SERIALIZATION_STRING_DESERIALIZER)
            .routeId(RouteConstants.LOAD_MESSAGE_ROUTE + "-customer")
            
            // Log message reception
            .process(exchange -> {
                String topic = exchange.getIn().getHeader(KafkaConstants.TOPIC, String.class);
                String partition = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.PARTITION, Integer.class));
                String offset = String.valueOf(
                    exchange.getIn().getHeader(KafkaConstants.OFFSET, Long.class));
                
                log.info("Request message received from Orchestrator [Customer Management] - " +
                        "Topic: {}, Partition: {}, Offset: {}", 
                        topic, partition, offset);
                log.debug("Request message headers: {}", exchange.getIn().getHeaders());
            })
            
            // Filter by messageSchema header
            // Only process messages with valid messageSchema
            .filter().simple(ConditionalConstants.MESSAGE_SCHEMA_FILTER)
            
            // Log after filter
            .process(exchange -> {
                String messageSchema = exchange.getIn().getHeader(
                    GeneralConstants.MESSAGE_SCHEMA_RECEIVED_HEADER, String.class);
                log.debug("Request message passed filter - messageSchema: {}", messageSchema);
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
                
                log.info("Request message properties stored successfully [Customer Management]");
            })
            
            // Send to SEDA queue for asynchronous processing
            // This decouples Kafka consumption from message processing
            .to(RouteConstants.SEDA_ROUTE_TO)
            
            .process(exchange -> 
                log.debug("Request message sent to SEDA queue [Customer Management]"));

        // ========== PART 3: SEDA CONSUMER TO PROCESSING PIPELINE ==========
        // Processes messages from SEDA queue with 8 concurrent consumers
        // Delegates to ValidationRoute for validation and transformation
        
        from(RouteConstants.SEDA_ROUTE_FROM)
            .routeId(RouteConstants.LOAD_MESSAGE_ROUTE + "-processor")
            
            .process(exchange -> 
                log.debug("Request message consumed from SEDA queue"))
            
            // Step 1: Validate and transform request message
            // Deserializes Avro, validates schema, transforms with JSONata
            .to(RouteConstants.DIRECT_VALIDATION)
            
            .process(exchange -> 
                log.debug("Request validation completed successfully"))
            
            // Step 2: Send transformed message to EPortal
            // Routes to appropriate topic (JRD or QRO) based on input
            .to(RouteConstants.DIRECT_COMMIT_ROUTE)
            
            .process(exchange -> 
                log.info("Request processing pipeline completed successfully"));
    }
}