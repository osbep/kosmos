package com.banamex.acmt.psk.kcp.dual.own.account.information.constants;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration properties bound from external configuration
 * (e.g. YAML or properties files).
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Binds channel-specific configurations under the prefix {@code app}.</li>
 *   <li>Provides immutable configuration structures for channels, operations, topics,
 *       and DLQs.</li>
 *   <li>Applies defensive copying and null-safety defaults to ensure consistent runtime
 *       behavior.</li>
 * </ul>
 * </p>
 *
 * <p><b>Configuration Structure:</b>
 * <pre>
 * app:
 *   channel:
 *     BNE:
 *       enabled: true
 *       timeout: 30000
 *       operations:
 *         requestPayerCustomerOwnAccountRetrieve:
 *           enabled: true
 *           name: requestPayerCustomerOwnAccountRetrieve
 *           jsonschema: request-payer-schema.json
 *           jsonata: request-payer-transform.json
 *           jsonavro: RequestPayerCustomerOwnAccountRetrieve.avsc
 *           topics:
 *             dynamic: false
 *             name:
 *               - mx.jrd.accountManagement.oab.payerQuery.input
 *               - mx.qro.accountManagement.oab.payerQuery.input
 *           groupId: acmt-psk-kcp-dual-own-account-information-payer
 *       dlq:
 *         enabled: true
 *         name: mx.accountManagement.oab.dlq
 *       dataCenter:
 *         jrd: JRD
 *         qro: QRO
 * </pre>
 * </p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Uses Java records for immutability and conciseness.</li>
 *   <li>Annotated with {@link ConfigurationProperties} to allow automatic binding in Spring Boot.</li>
 *   <li>Ignores invalid fields to prevent startup failure when unknown properties are provided.</li>
 *   <li>Applies defensive copying in compact constructors to ensure immutability.</li>
 * </ul>
 * </p>
 *
 * @author jd35030
 * @version 2.0.0
 */
@ConfigurationProperties(prefix = "app", ignoreInvalidFields = true)
public record AppConfig(Map<String, Channel> channel) {

    /**
     * Constructs the {@code AppConfig} ensuring channels are copied and non-null.
     *
     * @param channel map of channel IDs to {@link Channel} configuration
     */
    public AppConfig {
        channel = Objects.isNull(channel) ? Map.of() : Map.copyOf(channel);
    }

    /**
     * Gets the BNE channel configuration.
     *
     * <p>Convenience method to retrieve the most commonly used channel.
     *
     * @return the BNE channel configuration, or null if not configured
     */
    public Channel getBneChannel() {
        return channel.get("BNE");
    }

    /**
     * Gets a specific operation from the BNE channel.
     *
     * <p>Convenience method that combines channel and operation lookup.
     *
     * @param operationName the name of the operation to retrieve
     * @return the operation configuration, or null if not found
     */
    public Operation getOperation(String operationName) {
        Channel bne = getBneChannel();
        if (bne == null || bne.operations() == null) {
            return null;
        }
        return bne.operations().get(operationName);
    }

    /**
     * Gets all operations from the BNE channel.
     *
     * @return map of all operations, or empty map if BNE channel not configured
     */
    public Map<String, Operation> getAllOperations() {
        Channel bne = getBneChannel();
        return bne != null ? bne.operations() : Map.of();
    }

    /**
     * Validates the entire configuration.
     *
     * <p>Checks that:
     * <ul>
     *   <li>At least one channel is configured</li>
     *   <li>The BNE channel exists and is enabled</li>
     *   <li>The BNE channel has at least one enabled operation</li>
     * </ul>
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        if (channel == null || channel.isEmpty()) {
            return false;
        }

        Channel bne = getBneChannel();
        if (bne == null || !bne.enabled() || bne.operations() == null || bne.operations().isEmpty()) {
            return false;
        }

        // At least one operation must be enabled
        return bne.operations().values().stream()
                .anyMatch(Operation::enabled);
    }
}

/**
 * Represents configuration for a logical channel in the system.
 *
 * <p><b>Fields:</b>
 * <ul>
 *   <li>{@code enabled} - whether the channel is active.</li>
 *   <li>{@code timeout} - timeout value in milliseconds for operations.</li>
 *   <li>{@code operations} - available operations within the channel.</li>
 *   <li>{@code dlq} - dead-letter queue configuration.</li>
 *   <li>{@code dataCenter} - datacenter-specific mappings (JRD, QRO).</li>
 * </ul>
 * </p>
 *
 * <p><b>Example:</b>
 * <pre>
 * BNE:
 *   enabled: true
 *   timeout: 30000
 *   operations:
 *     requestPayerCustomerOwnAccountRetrieve: ...
 *   dlq:
 *     enabled: true
 *     name: mx.accountManagement.oab.dlq
 *   dataCenter:
 *     jrd: JRD
 *     qro: QRO
 * </pre>
 * </p>
 */
public record Channel(
    boolean enabled,
    Integer timeout,
    Map<String, Operation> operations,
    Dlq dlq,
    Map<String, String> dataCenter
) {

    /**
     * Constructs the {@code Channel} ensuring operations and data center
     * mappings are copied and non-null.
     */
    public Channel {
        operations = Objects.isNull(operations) ? Map.of() : Map.copyOf(operations);
        dataCenter = Objects.isNull(dataCenter) ? Map.of() : Map.copyOf(dataCenter);
    }

    /**
     * Gets datacenter code for a given key.
     *
     * @param key the datacenter key (e.g., "jrd", "qro")
     * @return the datacenter code, or null if not found
     */
    public String getDataCenter(String key) {
        return dataCenter.get(key);
    }

    /**
     * Checks if DLQ is enabled for this channel.
     *
     * @return true if DLQ is configured and enabled
     */
    public boolean isDlqEnabled() {
        return dlq != null && dlq.enabled();
    }

    /**
     * Gets the DLQ topic name.
     *
     * @return the DLQ topic name, or null if not configured
     */
    public String getDlqName() {
        return dlq != null ? dlq.name() : null;
    }
}

/**
 * Represents an operation available within a channel.
 *
 * <p><b>Fields:</b>
 * <ul>
 *   <li>{@code enabled} - whether the operation is active.</li>
 *   <li>{@code name} - name of the operation (should match the key in operations map).</li>
 *   <li>{@code topics} - topics associated with this operation.</li>
 *   <li>{@code groupId} - Kafka consumer group ID.</li>
 *   <li>{@code jsonavro} - reference to Avro schema resource (filename).</li>
 *   <li>{@code jsonata} - reference to JSONata expression resource (filename).</li>
 *   <li>{@code jsonschema} - reference to JSON Schema resource (filename).</li>
 * </ul>
 * </p>
 *
 * <p><b>Processing Flow:</b>
 * <pre>
 * Incoming Message
 *    ↓ (validate with jsonschema)
 * Validated JSON
 *    ↓ (transform with jsonata)
 * Transformed JSON
 *    ↓ (serialize with jsonavro)
 * Avro Bytes
 *    ↓ (route to topics)
 * Kafka Topic(s)
 * </pre>
 * </p>
 *
 * <p><b>Example:</b>
 * <pre>
 * requestPayerCustomerOwnAccountRetrieve:
 *   enabled: true
 *   name: requestPayerCustomerOwnAccountRetrieve
 *   jsonschema: request-payer-schema.json
 *   jsonata: request-payer-transform.json
 *   jsonavro: RequestPayerCustomerOwnAccountRetrieve.avsc
 *   topics:
 *     dynamic: false
 *     name:
 *       - mx.jrd.accountManagement.oab.payerQuery.input
 *       - mx.qro.accountManagement.oab.payerQuery.input
 *   groupId: acmt-psk-kcp-dual-own-account-information-payer
 * </pre>
 * </p>
 */
public record Operation(
    boolean enabled,
    String name,
    Topics topics,
    String groupId,
    String jsonavro,
    String jsonata,
    String jsonschema
) {

    /**
     * Gets the primary topic name from the topics list.
     *
     * <p>If topics are dynamic or multiple topics exist, returns the first one.
     *
     * @return the primary topic name, or null if no topics configured
     */
    public String getTopicName() {
        if (topics == null || topics.name() == null || topics.name().isEmpty()) {
            return null;
        }
        return topics.name().get(0);
    }

    /**
     * Gets all topic names for this operation.
     *
     * @return list of topic names, or empty list if not configured
     */
    public List<String> getAllTopicNames() {
        return topics != null ? topics.name() : List.of();
    }

    /**
     * Checks if this operation has valid configuration.
     *
     * <p>An operation is considered valid if:
     * <ul>
     *   <li>It is enabled</li>
     *   <li>Has a name</li>
     *   <li>Has jsonschema, jsonata, and jsonavro</li>
     *   <li>Has at least one topic</li>
     *   <li>Has a groupId</li>
     * </ul>
     *
     * @return true if all required fields are valid
     */
    public boolean isValid() {
        return enabled
                && name != null && !name.isEmpty()
                && jsonschema != null && !jsonschema.isEmpty()
                && jsonata != null && !jsonata.isEmpty()
                && jsonavro != null && !jsonavro.isEmpty()
                && topics != null && topics.name() != null && !topics.name().isEmpty()
                && groupId != null && !groupId.isEmpty();
    }
}

/**
 * Represents Kafka topic configuration for an operation.
 *
 * <p><b>Fields:</b>
 * <ul>
 *   <li>{@code dynamic} - whether topic names are dynamically resolved at runtime.</li>
 *   <li>{@code topicD} - default or destination topic (optional).</li>
 *   <li>{@code name} - list of topic names associated with the operation.</li>
 * </ul>
 * </p>
 *
 * <p><b>Topic Resolution:</b>
 * <ul>
 *   <li>If {@code dynamic} is false: use topics from {@code name} list directly</li>
 *   <li>If {@code dynamic} is true: topics may be resolved at runtime based on context</li>
 *   <li>{@code topicD} can be used as a fallback or default topic</li>
 * </ul>
 * </p>
 *
 * <p><b>Example (Static Topics):</b>
 * <pre>
 * topics:
 *   dynamic: false
 *   name:
 *     - mx.jrd.accountManagement.oab.payerQuery.input
 *     - mx.qro.accountManagement.oab.payerQuery.input
 * </pre>
 * </p>
 *
 * <p><b>Example (Dynamic Topics):</b>
 * <pre>
 * topics:
 *   dynamic: true
 *   topicD: mx.{datacenter}.accountManagement.oab.payerQuery.input
 *   name: []
 * </pre>
 * </p>
 */
public record Topics(
    Boolean dynamic,
    String topicD,
    List<String> name
) {

    /**
     * Constructs the {@code Topics} ensuring defaults.
     * <ul>
     *   <li>{@code name} is empty list if null.</li>
     *   <li>{@code dynamic} is {@code false} if null.</li>
     * </ul>
     */
    public Topics {
        name = Objects.isNull(name) ? List.of() : List.copyOf(name);
        dynamic = Objects.isNull(dynamic) ? Boolean.FALSE : dynamic;
    }

    /**
     * Checks if topics are configured dynamically.
     *
     * @return true if dynamic resolution is enabled
     */
    public boolean isDynamic() {
        return Boolean.TRUE.equals(dynamic);
    }

    /**
     * Gets the default topic.
     *
     * @return the default topic, or first topic from name list, or null
     */
    public String getDefaultTopic() {
        if (topicD != null && !topicD.isEmpty()) {
            return topicD;
        }
        return name != null && !name.isEmpty() ? name.get(0) : null;
    }
}

/**
 * Represents a dead-letter queue (DLQ) configuration for a channel.
 *
 * <p><b>Fields:</b>
 * <ul>
 *   <li>{@code enabled} - whether the DLQ is active.</li>
 *   <li>{@code name} - DLQ topic name or identifier.</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage:</b>
 * When enabled, failed messages are routed to the DLQ topic instead of
 * being discarded. This allows for:
 * <ul>
 *   <li>Message replay after fixing issues</li>
 *   <li>Analysis of failure patterns</li>
 *   <li>Manual intervention for critical failures</li>
 * </ul>
 * </p>
 *
 * <p><b>Example:</b>
 * <pre>
 * dlq:
 *   enabled: true
 *   name: mx.accountManagement.oab.dlq
 * </pre>
 * </p>
 */
public record Dlq(
    boolean enabled,
    String name
) {

    /**
     * Checks if DLQ is properly configured.
     *
     * @return true if enabled and has a name
     */
    public boolean isValid() {
        return enabled && name != null && !name.isEmpty();
    }
}