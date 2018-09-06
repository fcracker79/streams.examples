package myapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(
        name = "ConioLayout",
        category = "Core",
        elementType = "layout",
        printObject = true
)
public final class ConioLayout implements Layout<String> {
    private final String origin;
    public ConioLayout(String origin) {
        this.origin = origin;
    }
    @Override
    public byte[] getFooter() {
        return null;
    }

    @Override
    public byte[] getHeader() {
        return null;
    }

    @Override
    public byte[] toByteArray(LogEvent logEvent) {
        return toSerializable(logEvent).getBytes();
    }

    @Override
    public String toSerializable(LogEvent logEvent) {
        final Map m = new HashMap();
        m.put("@timestamp", new SimpleDateFormat("yyyy-MM-dd'T'KK:mm:ss.SSS'Z'")
                .format(new Date(logEvent.getTimeMillis())));
        m.put("payload.message", logEvent.getMessage().getFormattedMessage());
        m.put("origin", this.origin);
        m.put("source", logEvent.getSource().toString());
        m.put("type", "SERVICE_EVENT");
        m.put("tags", logEvent.getLoggerName());
        m.put("level", logEvent.getLevel().toString());
        m.put("process_id", ManagementFactory.getRuntimeMXBean().getName());
        m.put("thread_id", logEvent.getThreadId());
        m.put("thread_name", logEvent.getThreadName());
        if (logEvent.getThrown() != null) {
            final Map stacktrace = new HashMap();
            stacktrace.put("function", "???");
            stacktrace.put("process", ManagementFactory.getRuntimeMXBean().getName());
            stacktrace.put("thread", logEvent.getThreadName());
            stacktrace.put("function", "???");
            stacktrace.put(
                    "stack",
                    Stream.of(logEvent.getThrown().getStackTrace()).map(String::valueOf).collect(
                            Collectors.joining("\n")
                    ));
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(m);
            return jsonResult;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public Map<String, String> getContentFormat() {
        Map<String, String> result = new HashMap();
        result.put("version", "2.0");
        return result;
    }

    @Override
    public void encode(LogEvent logEvent, ByteBufferDestination byteBufferDestination) {
        byte[] data = this.toByteArray(logEvent);
        byteBufferDestination.writeBytes(data, 0, data.length);
    }

    @PluginBuilderFactory
    public static ConioLayout.Builder newBuilder() {
        return new ConioLayout.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ConioLayout> {
        @PluginBuilderAttribute
        private volatile String origin;

        public Builder setOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        @Override
        public ConioLayout build() {
            return new ConioLayout(origin);
        }
    }

}