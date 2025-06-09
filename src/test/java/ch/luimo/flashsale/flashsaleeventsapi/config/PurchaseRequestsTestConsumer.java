package ch.luimo.flashsale.flashsaleeventsapi.config;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;

public class PurchaseRequestsTestConsumer {

    private final KafkaConsumer<String, String> consumer;

    public PurchaseRequestsTestConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

    public void subscribe(String topic) {
        consumer.subscribe(Collections.singletonList(topic));
    }

    public ConsumerRecords<String, String> poll(Duration timeout) {
        return consumer.poll(timeout);
    }

    public void close() {
        consumer.close();
    }
}
