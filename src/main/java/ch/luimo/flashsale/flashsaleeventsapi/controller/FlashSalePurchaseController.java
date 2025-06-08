package ch.luimo.flashsale.flashsaleeventsapi.controller;

import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/v1/purchases")
public class FlashSalePurchaseController {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSalePurchaseController.class);

    private final PurchaseRequestService purchaseRequestService;

    public FlashSalePurchaseController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @PostMapping
    public FlashsalePurchaseResponseREST createPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequest) {
        LOG.info("Received POST /v1/purchases, purchaseRequestId: {}", purchaseRequest.getPurchaseRequestId());
        return purchaseRequestService.submitPurchase(purchaseRequest);
    }
}
