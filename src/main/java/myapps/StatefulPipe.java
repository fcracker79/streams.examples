package myapps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class StatefulPipe {
    private static final String STATE_STORE_NAME = "event_counter";
    /*

     */
    public static void main(String ... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: <command> event_name [ event name]*");
            System.exit(1);
        }
        final Collection<String> topics = Arrays.asList(args);
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        final StreamsBuilder builder = new StreamsBuilder();
        final Serde<JsonNode> jsonSerdes = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer());

        final StoreBuilder<KeyValueStore<String, JsonNode>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STATE_STORE_NAME),
                Serdes.String(),
                jsonSerdes
        );
        builder.addStateStore(storeBuilder);
        KStream<String, JsonNode> source = builder.stream(topics, Consumed.with(Serdes.String(), jsonSerdes));
        source.process(
                new ProcessorSupplier<String, JsonNode>() {
                    @Override
                    public Processor<String, JsonNode> get() {
                        return new Processor<String, JsonNode>() {
                            private KeyValueStore<String, JsonNode> state;

                            @Override
                            public void init(ProcessorContext context) {
                                this.state = (KeyValueStore<String, JsonNode>) context.getStateStore(STATE_STORE_NAME);
                            }

                            @Override
                            public void process(String key, JsonNode value) {
                                final String storeKey = value.path("type").asText() + "_count";
                                final JsonNode oldValue = this.state.get(storeKey);
                                if (oldValue == null) {
                                    this.state.put(storeKey, JsonNodeFactory.instance.numberNode(1));
                                }else {
                                    this.state.put(storeKey, JsonNodeFactory.instance.numberNode(oldValue.asInt() + 1));
                                }
                                this.state.flush();
                                System.out.format("STORE STATUS: %s: %s\n", storeKey, this.state.get(storeKey));
                            }

                            @Override
                            public void punctuate(long timestamp) {

                            }

                            @Override
                            public void close() {

                            }
                        };
                    }
                },
                STATE_STORE_NAME
        );


        source.to("test-output", Produced.with(Serdes.String(), jsonSerdes));
        final Topology topology = builder.build();
        System.out.println(topology.describe());
        final KafkaStreams streams = new KafkaStreams(topology, props);

        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        } finally {
            streams.close();
        }
        System.exit(0);
    }
}
