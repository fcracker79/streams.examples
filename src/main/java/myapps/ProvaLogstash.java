package myapps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.InputStream;


public class ProvaLogstash {
    public static void main(String ... args) throws Exception {
        final InputStream configStream = ProvaLogstash.class.getClassLoader().getResourceAsStream("log4j.properties");
        ConfigurationSource source = new ConfigurationSource(configStream, new File("log4j.properties"));
        final LoggerContext ctx = Configurator.initialize(null, source);
        LogManager.getContext(false);
        final Logger logger = LogManager.getLogger("dino");
        System.out.println(logger.getLevel());
        logger.error("A simple test");
    }
}
