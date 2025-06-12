package ch.luimo.flashsale.flashsaleeventsapi.config;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConsumerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Value("${application.kafka-consumer-api-key:fake-consumer-api-key}")
    private String kafkaConsumerApiKey;

    @Value("${application.kafka-consumer-api-secret:fake-consumer-api-secret}")
    private String kafkaConsumerApiSecret;

    @Value("${application.schema-api-key:fake-schema-api-key}")
    private String schemaApiKey;

    @Value("${application.schema-api-secret:fake-schema-api-secret}")
    private String schemaApiSecret;

    @Bean
    @Profile("dev")
    public String kafkaJaasConfig() {
        return String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                kafkaConsumerApiKey,
                kafkaConsumerApiSecret
        );
    }

    @Bean
    @Profile("dev")
    public Map<String, Object> kafkaConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());

        // SASL Authentication
        props.put("security.protocol", kafkaProperties.getProperties().get("security.protocol"));
        props.put("sasl.mechanism", kafkaProperties.getProperties().get("sasl.mechanism"));
        props.put("sasl.jaas.config", kafkaJaasConfig());

        // Schema Registry Authentication
        props.put("schema.registry.url", kafkaProperties.getProperties().get("schema.registry.url"));
        props.put("basic.auth.credentials.source", kafkaProperties.getProperties().get("basic.auth.credentials.source"));
        props.put("basic.auth.user.info", schemaApiKey + ":" + schemaApiSecret);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put("value.deserializer.type", "specific");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(Map<String, Object> kafkaConsumerProperties) {
        KafkaAvroDeserializer avroDeserializer = new KafkaAvroDeserializer();
        avroDeserializer.configure(kafkaConsumerProperties, false);
        return new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties,
                new StringDeserializer(), avroDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AvroFlashSaleEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, AvroFlashSaleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

}
