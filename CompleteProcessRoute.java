package com.banamex.acmt.psk.kcp.dual.own.account.information.route;

import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.GeneralConstants;
import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.MessageConstants;
import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.RouteConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaManualCommit;
import org.springframework.stereotype.Component;

/**
 * Route for completing message processing with manual Kafka commit.
 * 
 * <p><strong>Responsibilities:</strong>
 * <ul>
 *   <li>Execute manual Kafka commit for successfully processed messages</li>
 *   <li>Retrieve KafkaManualCommit from exchange properties</li>
 *   <li>Execute commit operation</li>
 *   <li>Log commit success or failure</li>
 *   <li>Handle commit errors gracefully</li>
 * </ul>
 * 
 * <p><strong>When is this route called?</strong>
 * This route is invoked by {@code onCompletion().onCompleteOnly()} in:
 * <ul>
 *   <li>{@link CommitRoute} - After sending request to EPortal</li>
 *   <li>{@link ResponseCommitRoute} - After sending response to Orchestrator</li>
 *   <li>{@link GlobalDlqRoute} - After sending error to DLQ</li>
 * </ul>
 * 
 * <p><strong>Flow:</strong>
 * <pre>
 * Any Route with onCompletion()
 *      ↓
 * .onCompletion().onCompleteOnly()
 *      .to(RouteConstants.DIRECT_COMPLETE_PROCESS_ROUTE)
 *      ↓
 * CompleteProcessRoute.from(DIRECT_COMPLETE_PROCESS_ROUTE)
 *      ↓
 * 1. Retrieve KafkaManualCommit from properties
 * 2. Execute manualCommit.commit()
 * 3. Log success
 * 4. Handle errors if commit fails
 * </pre>
 * 
 * <p><strong>Why manual commit?</strong>
 * Manual commit ensures that Kafka offset is only committed after:
 * <ul>
 *   <li>Message is successfully validated</li>
 *   <li>Message is successfully transformed</li>
 *   <li>Message is successfully sent to output topic</li>
 *   <li>OR message is successfully sent to DLQ (in case of error)</li>
 * </ul>
 * 
 * <p>This prevents message loss and ensures at-least-once delivery semantics.
 * 
 * @author jd35030
 * @version 1.0.0
 * @see CommitRoute
 * @see ResponseCommitRoute
 * @see GlobalDlqRoute
 */
@Slf4j
@Component
public class CompleteProcessRoute extends RouteBuilder {

    /**
     * Configures the manual commit completion route.
     * 
     * <p>This route is called after successful message processing to commit
     * the Kafka offset manually, ensuring the message is not re-processed.
     * 
     * @throws Exception if route configuration fails
     */
    @Override
    public void configure() throws Exception {

        from(RouteConstants.DIRECT_COMPLETE_PROCESS_ROUTE)
            .routeId(RouteConstants.COMPLETE_PROCESS_ROUTE_ID)
            
            // Process manual commit
            .process(exchange -> {
                
                // Step 1: Retrieve KafkaManualCommit from exchange properties
                // This property is set by LoadMessageRoute and EPortalResponseRoute
                // when consuming from Kafka topics
                KafkaManualCommit manualCommit = exchange.getProperty(
                    GeneralConstants.KAFKA_MANUAL_COMMIT_PROPERTY,
                    KafkaManualCommit.class
                );
                
                // Step 2: Check if manual commit is available
                if (manualCommit != null) {
                    
                    try {
                        // Step 3: Execute manual commit
                        // This commits the Kafka offset, marking the message as processed
                        manualCommit.commit();
                        
                        // Step 4: Log success with topic information
                        String inputTopic = exchange.getProperty(
                            RouteConstants.HEADER_INPUT_TOPIC, String.class);
                        
                        log.info("{} - Topic: {}", 
                                MessageConstants.MANUAL_COMMIT_SUCCESS,
                                inputTopic != null ? inputTopic : "unknown");
                        
                        log.debug("Kafka offset committed successfully for topic: {}", 
                                 inputTopic);
                        
                    } catch (Exception e) {
                        // Step 5: Handle commit errors
                        // Log error but don't throw exception to avoid affecting message processing
                        log.error("Error executing manual commit: {}", 
                                 e.getMessage(), e);
                        
                        // Note: In production, you might want to implement retry logic
                        // or send to a dead letter queue for manual intervention
                    }
                    
                } else {
                    // Step 6: Log warning if manual commit is not available
                    // This might happen if the message didn't come from a Kafka consumer
                    // or if the property was not set correctly
                    log.warn("KafkaManualCommit not found in exchange properties. " +
                            "Skipping manual commit. This is expected for non-Kafka sources.");
                    
                    log.debug("Exchange properties: {}", 
                             exchange.getProperties().keySet());
                }
            })
            
            // Final log
            .process(exchange -> 
                log.debug("Complete process route finished successfully"));
    }
}