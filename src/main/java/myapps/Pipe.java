package myapps;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Pipe {
    /*
     Before starting:
     1. bin/zookeeper-server-start.sh config/zookeeper.properties
     2. bin/kafka-server-start.sh config/server.properties
     3. bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
     4. bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test-output
     5. bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-output --from-beginning
     6. bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
     */
    public static void main(String ... args) throws Exception {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<byte[], byte[]> source =
                builder.stream("test", Consumed.with(Serdes.ByteArray(), Serdes.ByteArray()))
                        .mapValues(s -> {
                            final String sStr = new String(s);
                            System.out.format("Got %s\n", sStr);
                            return String.format("MODIFIED_STRING[%s]", sStr).getBytes();
                        });

        source.to("test-output", Produced.with(Serdes.ByteArray(), Serdes.ByteArray()));
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
