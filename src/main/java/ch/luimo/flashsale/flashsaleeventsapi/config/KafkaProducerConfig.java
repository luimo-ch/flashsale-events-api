package ch.luimo.flashsale.flashsaleeventsapi.config;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Bean
    public ProducerFactory<String, AvroPurchaseRequest> purchaseRequestProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }
    @Bean
    public KafkaTemplate<String, AvroPurchaseRequest> purchaseRequestKafkaTemplate(ProducerFactory<String, AvroPurchaseRequest> purchaseRequestProducerFactory) {
        return new KafkaTemplate<>(purchaseRequestProducerFactory);
    }

    @Bean
    public ProducerFactory<String, AvroFlashSaleEvent> flashSaleEventProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, AvroFlashSaleEvent> flashSaleEventKafkaTemplate(ProducerFactory<String, AvroFlashSaleEvent> flashSaleEventProducerFactory) {
        return new KafkaTemplate<>(flashSaleEventProducerFactory);
    }

}
