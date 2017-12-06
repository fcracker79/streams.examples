package myapps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;

import java.util.Collections;
import java.util.Properties;

public class EventsConsumer {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092"; // I can use more servers comma separated
    private static final String TOPIC = "test-output";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Consumer<String, JsonNode> createConsumer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "EventsConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MainGroup");
        return new KafkaConsumer<>(props);
    }
    public static void main(String ... args) throws Exception {
        final Consumer<String, JsonNode> consumer = createConsumer();
        consumer.subscribe(Collections.singleton(TOPIC));
        while (true) {
            final ConsumerRecords<String, JsonNode> consumerRecords = consumer.poll(1000);
            System.out.format("Records: %d\n", consumerRecords.count());
            for (ConsumerRecord<String, JsonNode> record : consumerRecords) {
                System.out.format(
                        "[%s]: %s\n",
                        record.key(), record.value()
                );
            }
            consumer.commitSync();
        }
    }

}
