package com.banamex.acmt.psk.kcp.dual.own.account.information.route;

import com.banamex.acmt.psk.kcp.dual.own.account.information.constant.AvroConstants;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

/**
 * Utility class for dynamic Avro serialization and deserialization.
 * 
 * <p>Provides methods to convert between JSON strings and Avro binary format,
 * as well as schema loading from multiple sources.
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>JSON to Avro binary conversion</li>
 *   <li>Avro binary to JSON conversion</li>
 *   <li>Schema loading from classpath, filesystem, or inline JSON</li>
 *   <li>Uses GenericRecord for dynamic schema handling</li>
 * </ul>
 * 
 * @author jd35030
 * @version 2.0.0
 */
public final class AvroDynamic {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private AvroDynamic() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Loads an Avro schema from various sources.
     * 
     * <p>Supports three formats:
     * <ul>
     *   <li>{@code classpath:/schemas/MySchema.avsc} - Loads from classpath resources</li>
     *   <li>{@code file:/tmp/schema.avsc} - Loads from filesystem</li>
     *   <li>{@code {"type": "record", ...}} - Parses inline JSON schemas</li>
     * </ul>
     * 
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * // Load from classpath
     * Schema schema1 = AvroDynamic.loadSchema("classpath:/avro/MySchema.avsc");
     * 
     * // Load from filesystem
     * Schema schema2 = AvroDynamic.loadSchema("file:/tmp/schema.avsc");
     * 
     * // Parse inline JSON
     * Schema schema3 = AvroDynamic.loadSchema("{\"type\": \"record\", \"name\": \"User\", ...}");
     * }</pre>
     * 
     * @param ref the schema reference (classpath, file, or inline JSON)
     * @return the parsed Avro schema
     * @throws IOException if the schema cannot be loaded or parsed
     * @throws IllegalArgumentException if ref is null or blank
     * @throws FileNotFoundException if classpath resource not found
     */
    public static Schema loadSchema(String ref) throws IOException {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException(AvroConstants.ERROR_SCHEMA_HEADER_REQUIRED);
        }

        // Load from classpath
        if (ref.startsWith(AvroConstants.CLASSPATH_PREFIX)) {
            String path = ref.substring(AvroConstants.CLASSPATH_PREFIX.length());
            try (InputStream in = AvroDynamic.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new FileNotFoundException(AvroConstants.ERROR_SCHEMA_NOT_FOUND + path);
                }
                return new Schema.Parser().parse(in);
            }
        }

        // Load from filesystem
        if (ref.startsWith(AvroConstants.FILE_PREFIX)) {
            Path path = Paths.get(ref.substring(AvroConstants.FILE_PREFIX.length()));
            return new Schema.Parser().parse(Files.readString(path));
        }

        // Parse inline JSON schema
        return new Schema.Parser().parse(ref);
    }

    /**
     * Converts JSON string to Avro binary bytes.
     * 
     * <p><strong>Conversion Flow:</strong>
     * <pre>
     * JSON String
     *    ↓ JsonDecoder
     * GenericRecord
     *    ↓ GenericDatumWriter
     * Binary Avro
     *    ↓ BinaryEncoder
     * byte[]
     * </pre>
     * 
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * String json = "{\"name\": \"John\", \"age\": 30}";
     * Schema schema = new Schema.Parser().parse(...);
     * byte[] avroBytes = AvroDynamic.jsonToAvroBytes(json, schema);
     * }</pre>
     * 
     * @param json the JSON string to convert
     * @param schema the Avro schema to use for encoding
     * @return Avro binary representation as byte array
     * @throws IOException if encoding fails
     */
    public static byte[] jsonToAvroBytes(String json, Schema schema) throws IOException {

        // Step 1: Decode JSON to GenericRecord
        Decoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, json);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        GenericRecord record = reader.read(null, jsonDecoder);

        // Step 2: Encode GenericRecord to binary Avro
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        writer.write(record, binaryEncoder);
        binaryEncoder.flush();

        return outputStream.toByteArray();
    }

    /**
     * Converts Avro binary bytes to JSON string.
     * 
     * <p><strong>Conversion Flow:</strong>
     * <pre>
     * byte[]
     *    ↓ BinaryDecoder
     * GenericRecord
     *    ↓ GenericDatumReader
     * JSON String
     *    ↓ JsonEncoder
     * String
     * </pre>
     * 
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * byte[] avroBytes = ...;
     * Schema schema = new Schema.Parser().parse(...);
     * String json = AvroDynamic.avroBytesToJsonString(avroBytes, schema);
     * System.out.println(json); // {"name": "John", "age": 30}
     * }</pre>
     * 
     * @param avroBytes the Avro binary bytes to convert
     * @param schema the Avro schema to use for decoding
     * @return JSON string representation
     * @throws IOException if decoding fails
     */
    public static String avroBytesToJsonString(byte[] avroBytes, Schema schema) throws IOException {

        // Step 1: Decode Avro bytes to GenericRecord
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(avroBytes, null);
        GenericRecord record = reader.read(null, binaryDecoder);

        // Step 2: Encode GenericRecord to JSON string
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
        writer.write(record, jsonEncoder);
        jsonEncoder.flush();
        outputStream.flush();

        return outputStream.toString(StandardCharsets.UTF_8);
    }
}