package myapps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Pipe {
    /*
     To create:
     mvn archetype:generate     -DarchetypeGroupId=org.apache.kafka     -DarchetypeArtifactId=streams-quickstart-java     -DarchetypeVersion=1.0.0     -DgroupId=streams.examples     -DartifactId=streams.examples     -Dversion=0.1     -Dpackage=myapps
     Before starting (WITHOUT swarm):
     1. bin/zookeeper-server-start.sh config/zookeeper.properties
     2. bin/kafka-server-start.sh config/server.properties
     3. bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
     4. bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test-output
     5. bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-output --from-beginning
     6. bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test

     Before starting (WITH swarm):
     1. docker stack deploy -c docker-compose-v3.yml confluent
     2. inside kafka node:
        1. kafka-topics --create --zookeeper localhost:32181 --replication-factor 1 --partitions 1 --topic test
        2. kafka-topics --create --zookeeper localhost:32181 --replication-factor 1 --partitions 1 --topic test-output
        3. kafka-topics --zookeeper localhost:32181 --alter --topic test --config retention.ms=0
        4. kafka-topics --zookeeper localhost:32181 --alter --topic test-output --config retention.ms=0

     In order to clear topic:
       - bin/kafka-topics.sh --zookeeper localhost:9092 --alter --topic test --config retention.ms=100
     */
    public static void main(String ... args) throws Exception {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        final StreamsBuilder builder = new StreamsBuilder();
        final ObjectMapper jsonMapper = new ObjectMapper();
        final Serde<JsonNode> jsonSerdes = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer());
        KStream<String, JsonNode> source =
                builder.stream("test", Consumed.with(Serdes.String(), jsonSerdes))
                        .mapValues(s -> {
                            final Map<String, Object> event = jsonMapper.convertValue(s, Map.class);
                            System.out.format("Got %s\n", event);
                            event.put("DINO", "MY_ID" + event.get("id"));
                            return jsonMapper.convertValue(event, JsonNode.class);
                        });

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
