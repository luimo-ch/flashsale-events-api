package ch.luimo.flashsale.flashsaleeventsapi.config;

import ch.luimode.flashsale.PurchaseRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Profile("dev")
@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaProducerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerConfig.class);

    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Value("${application.kafka-api-key}")
    private String kafkaApiKey;

    @Value("${application.kafka-api-secret}")
    private String kafkaApiSecret;

    @Value("${application.schema-api-key}")
    private String schemaApiKey;

    @Value("${application.schema-api-secret}")
    private String schemaApiSecret;

    public String kafkaJaasConfig() {
        return String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                kafkaApiKey,
                kafkaApiSecret
        );
    }

    public Map<String, Object> kafkaProducerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().getKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().getValueSerializer());
        props.put("security.protocol", kafkaProperties.getProperties().get("security.protocol"));
        props.put("sasl.mechanism", kafkaProperties.getProperties().get("sasl.mechanism"));
        props.put("sasl.jaas.config", kafkaJaasConfig());
        props.put("schema.registry.url", kafkaProperties.getProperties().get("schema.registry.url"));
        props.put("basic.auth.credentials.source", kafkaProperties.getProperties().get("basic.auth.credentials.source"));
        props.put("basic.auth.user.info", schemaApiKey + ":" + schemaApiSecret);
        return props;
    }

    @Bean
    public ProducerFactory<String, PurchaseRequest> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProducerConfig());
    }

    @Bean
    public KafkaTemplate<String, PurchaseRequest> kafkaTemplate(ProducerFactory<String, PurchaseRequest> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
