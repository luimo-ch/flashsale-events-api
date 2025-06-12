package ch.luimo.flashsale.flashsaleeventsapi.service;

import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseRequestRest;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PublishingService {

    private static final Logger LOG = LoggerFactory.getLogger(PublishingService.class);

    private final KafkaTemplate<String, AvroPurchaseRequest> purchaseRequestKafkaTemplate;

    @Value("${application.kafka-topics.purchase-requests}")
    private String purchaseRequestsTopic;

    public PublishingService(KafkaTemplate<String, AvroPurchaseRequest> purchaseRequestKafkaTemplate) {
        this.purchaseRequestKafkaTemplate = purchaseRequestKafkaTemplate;
    }

    public void publishPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequest) {
        AvroPurchaseRequest event = toAvroPurchaseRequest(purchaseRequest);
        LOG.info("Publishing purchase request {} to topic: {}", purchaseRequestsTopic, event);
        purchaseRequestKafkaTemplate.send(purchaseRequestsTopic, String.valueOf(purchaseRequest.getPurchaseRequestId()), event)
                .thenRun(() -> LOG.info("Publishing flash sale event finished: {}", event))
                .exceptionally(ex -> {
                    LOG.error("Error publishing flash sale event", ex);
                    return null;
                });
    }

    private AvroPurchaseRequest toAvroPurchaseRequest(FlashsalePurchaseRequestRest source){
        return new AvroPurchaseRequest(
            source.getPurchaseRequestId(),
            source.getUserId(),
            source.getItemId(),
            source.getQuantity(),
            source.getRequestedAt().toInstant(),
            ch.luimode.flashsale.SourceType.valueOf(source.getSourceType().name())
        );
    }

}
