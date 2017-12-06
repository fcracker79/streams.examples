package myapps;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonSerializer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

public class EventsProducer {
    private final static String BOOTSTRAP_SERVERS = "localhost:9092"; // I can use more servers comma separated
    private static final String TOPIC = "test";

    private static Producer<String, Map<String, Object>> createProducer() {
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

    static void runProducer(final Iterator<Map<String, Object>> it) throws Exception {
        final Producer<String, Map<String, Object>> producer = createProducer();
        long time = System.currentTimeMillis();

        try {
            while (it.hasNext()) {
                final Map<String, Object> event = it.next();
                final ProducerRecord<String, Map<String, Object>> record =
                        new ProducerRecord<>(TOPIC, (String) event.get("uuid"), event);

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);

            }
        } finally {
            producer.flush();
            producer.close();
        }
    }

    public static void main(String ... args) throws Exception {
        try(FileInputStream is = new FileInputStream(args[0])) {
            final BufferedReader r = new BufferedReader(
                    new InputStreamReader(is)
            );
            runProducer(new BufferedReaderIterator(r));
        }
    }

    private static class BufferedReaderIterator implements Iterator<Map<String, Object>> {
        private final BufferedReader reader;
        private String lastLine = null;
        private boolean hasPendingLine = false;
        private final ObjectMapper objectMapper = new ObjectMapper();

        BufferedReaderIterator(BufferedReader reader) {
            this.reader = reader;
        }

        private String readLine() {
            try {
                return reader.readLine();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (!hasPendingLine) {
                hasPendingLine = true;
                lastLine = readLine();
            }
            return lastLine != null;
        }

        @Override
        public Map<String, Object> next() {
            final String valueToReturn;
            if (lastLine != null) {
                valueToReturn = lastLine;
                lastLine = null;
            } else {
                valueToReturn = readLine();
                if (valueToReturn == null) {
                    throw new NoSuchElementException();
                }
            }
            try {
                return objectMapper.convertValue(objectMapper.readTree(valueToReturn.getBytes()), Map.class);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
