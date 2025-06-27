package ch.luimo.flashsale.flashsaleeventsapi.config;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class FlashSaleEventsTestProducerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsTestProducerConfig.class);

    @Autowired
    private KafkaTemplate<String, AvroFlashSaleEvent> flashSaleEventKafkaTemplate;

    @Bean
    public FlashSaleEventsTestProducer flashSaleEventsTestProducer() {
        return new FlashSaleEventsTestProducer(flashSaleEventKafkaTemplate);
    }

    public static class FlashSaleEventsTestProducer {

        @Value("${application.kafka-topics.flashsale-events}")
        private String flashSaleEventsTopic;

        private final KafkaTemplate<String, AvroFlashSaleEvent> kafkaTemplate;

        public FlashSaleEventsTestProducer(KafkaTemplate<String, AvroFlashSaleEvent> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void publishEvent(AvroFlashSaleEvent event) {
            LOG.info("Publishing test event to topic {}: event ID {}", flashSaleEventsTopic, event.getEventId());
            kafkaTemplate.send(flashSaleEventsTopic, String.valueOf(event.getEventId()), event)
                    .thenRun(() -> LOG.info("Publishing flash sale event finished: {}", event))
                    .exceptionally(ex -> {
                        LOG.error("Error publishing flash sale event", ex);
                        return null;
                    });
        }
    }
}
