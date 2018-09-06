package myapps;

import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicListing;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class ListTopics {
    private static List<String> getBootstrapServers(String zookeeperUrl) throws IOException {
        System.out.println("Connecting to " + zookeeperUrl);
        final ObjectMapper mapper = new ObjectMapper();
        final List<String> result = new ArrayList<>();

        ZkClient zkClient = null;
        try {
            zkClient = new ZkClient(zookeeperUrl, 30000, 30000, ZKStringSerializer$.MODULE$);
            for (String clientId : zkClient.getChildren("/brokers/ids")) {
                System.out.println("Reading client " + clientId);
                final String strClient = zkClient.readData(
                        String.format("/brokers/ids/%s", clientId), true
                );
                System.out.println(strClient);
                final Map client = mapper.readValue(strClient, Map.class);
                result.add(String.format("%s:%s", client.get("host"), client.get("port")));
            }
            return result;
        } finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
    }

    private static List<String> getTopics(String zookeeperUrl) throws IOException, ExecutionException, InterruptedException {
        final Properties props = new Properties();
        props.setProperty(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                String.join(" and ", getBootstrapServers(zookeeperUrl))
        );
        System.out.println("Connecting to Kafka using properties " + props);
        final AdminClient adminClient = AdminClient.create(props);
        Collection<TopicListing> result = adminClient.listTopics().listings().get();
        return result.stream().filter(x -> !x.isInternal()).map(TopicListing::name).collect(Collectors.toList());
    }

    public static void main(String ... args) throws Exception {
        System.out.println(getTopics(args[0]));
    }
}
