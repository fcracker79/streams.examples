package myapps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;

import java.util.Collections;
import java.util.Properties;

public class EventsMultiplexer {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092"; // I can use more servers comma separated
    private static final String TOPIC = "events";

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

    private static Producer<String, JsonNode> createProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "EventsProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public static void main(String ... args) throws Exception {
        final Producer<String, JsonNode> producer = createProducer();
        final Consumer<String, JsonNode> consumer = createConsumer();
        consumer.subscribe(Collections.singleton(TOPIC));
        final long time = System.currentTimeMillis();

        while (true) {
            final ConsumerRecords<String, JsonNode> consumerRecords = consumer.poll(1000);
            System.out.format("Records: %d\n", consumerRecords.count());
            try {
                for (ConsumerRecord<String, JsonNode> record : consumerRecords) {
                    final String topicName = record.value().path("type").asText();


                    System.out.format(
                            "[%s]: %s\nsending to topic %s\n",
                            record.key(), record.value(), topicName
                    );
                    final ProducerRecord<String, JsonNode> producerRecord =
                            new ProducerRecord<>(topicName, record.key(), record.value());

                    RecordMetadata metadata = producer.send(producerRecord).get();

                    long elapsedTime = System.currentTimeMillis() - time;
                    System.out.printf("sent record(key=%s value=%s) " +
                                    "meta(partition=%d, offset=%d) time=%d\n",
                            record.key(), record.value(), metadata.partition(),
                            metadata.offset(), elapsedTime);
                }
            } finally {
                producer.flush();
            }
            consumer.commitSync();
        }
    }

}
