package myapps;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class CustomEventsConsumer {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092"; // I can use more servers comma separated

    private static Consumer<String, JsonNode> createConsumer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "EventsMultiplexer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MainGroup");
        return new KafkaConsumer<>(props);
    }

    public static void main(String ... args) {
        if (args.length == 0) {
            System.out.println("Usage: <command> event_name [ event name]*");
            System.exit(1);
        }
        final Consumer<String, JsonNode> consumer = createConsumer();
        final Collection<String> topics = Arrays.asList(args);
        consumer.subscribe(topics);
        final long time = System.currentTimeMillis();

        while (true) {
            final ConsumerRecords<String, JsonNode> consumerRecords = consumer.poll(1000);
            System.out.format("Records: %d\n", consumerRecords.count());
            for (ConsumerRecord<String, JsonNode> record : consumerRecords) {
                final String eventName = record.value().path("type").asText();
                System.out.format("[%s]: %s\nevent type %s, topics [%s]",
                        record.key(), record.value(), eventName, topics);
            }

            consumer.commitSync();
        }
    }

}
