package ch.luimo.flashsale.flashsaleeventsapi.config;

import ch.luimode.flashsale.PurchaseRequest;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

import static ch.luimo.flashsale.flashsaleeventsapi.IntegrationTestBase.BOOTSTRAP_SERVERS_PROPERTY;
import static ch.luimo.flashsale.flashsaleeventsapi.IntegrationTestBase.SCHEMA_REGISTRY_PROPERTY;

@TestConfiguration
public class KafkaTestConsumerConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty(BOOTSTRAP_SERVERS_PROPERTY));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        props.put("schema.registry.url", System.getProperty(SCHEMA_REGISTRY_PROPERTY)); // dynamically set by test-containers
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put("value.deserializer.type", "specific");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        KafkaAvroDeserializer avroDeserializer = new KafkaAvroDeserializer();
        avroDeserializer.configure(consumerConfigs(), false);
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(),
                new StringDeserializer(), avroDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PurchaseRequest> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PurchaseRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
