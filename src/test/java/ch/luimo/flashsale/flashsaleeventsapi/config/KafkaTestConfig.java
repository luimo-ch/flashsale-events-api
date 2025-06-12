package ch.luimo.flashsale.flashsaleeventsapi.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

import static ch.luimo.flashsale.flashsaleeventsapi.IntegrationTestBase.BOOTSTRAP_SERVERS_PROPERTY;
import static ch.luimo.flashsale.flashsaleeventsapi.IntegrationTestBase.SCHEMA_REGISTRY_PROPERTY;

@TestConfiguration
public class KafkaTestConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTestConfig.class);

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public Map<String, Object> kafkaProducerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty(BOOTSTRAP_SERVERS_PROPERTY));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().getKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().getValueSerializer());
        props.put("schema.registry.url", System.getProperty(SCHEMA_REGISTRY_PROPERTY));
        return props;
    }

    @Bean
    public Map<String, Object> kafkaConsumerProperties() {
        Map<String, Object> props = new HashMap<>();
        String bootstrapServers = System.getProperty(BOOTSTRAP_SERVERS_PROPERTY);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", System.getProperty(SCHEMA_REGISTRY_PROPERTY));
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put("value.deserializer.type", "specific");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }


}