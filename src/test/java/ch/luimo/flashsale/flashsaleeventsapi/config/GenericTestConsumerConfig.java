package ch.luimo.flashsale.flashsaleeventsapi.config;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Collections;

@TestConfiguration
public class GenericTestConsumerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GenericTestConsumerConfig.class);

    // for the purpose of manually polling messages
    @Bean
    public GenericTestConsumer genericTestConsumer(KafkaProperties kafkaProperties) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProperties.buildConsumerProperties());
        return new GenericTestConsumer(consumer);
    }

    public static class GenericTestConsumer {

        private final KafkaConsumer<String, String> consumer;

        public GenericTestConsumer(KafkaConsumer<String, String> consumer) {
            this.consumer = consumer;
        }

        public void subscribe(String topic) {
            consumer.subscribe(Collections.singletonList(topic));
        }

        public ConsumerRecords<String, String> poll(Duration timeout) {
            return consumer.poll(timeout);
        }

        public void close() {
            consumer.close(Duration.ofSeconds(1));
        }
    }
}
