package ch.luimo.flashsale.flashsaleeventsapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@Getter
@Setter
public class KafkaProperties {
    private String bootstrapServers;
    private Properties properties;
    private Producer producer;
    private Consumer consumer;

    @Getter
    @Setter
    public static class Producer {
        private String keySerializer;
        private String valueSerializer;
        private String acks;
        private Properties properties;
    }

    @Getter
    @Setter
    public static class Consumer {
        private String groupId;
    }
}
