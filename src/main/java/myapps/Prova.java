package myapps;

import com.j256.simplejmx.client.JmxClient;

import javax.management.ObjectName;
import java.util.Set;

public class Prova {
    public static void main(String ... args) throws Exception {
        JmxClient client = new JmxClient("support-1.it.aws.conio.com", 10030);
        Set<ObjectName> objectNameSet = client.getBeanNames();
        System.out.println(objectNameSet);
    }
}
