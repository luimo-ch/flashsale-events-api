package ch.luimo.flashsale.flashsaleeventsapi.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/v1/purchases")
public class FlashSalePurchaseController {

    private final PurchaseCacheService purchaseCacheService;

    public FlashSalePurchaseController(PurchaseCacheService purchaseCacheService) {
        this.purchaseCacheService = purchaseCacheService;
    }

    @PostMapping
    public FlashsalePurchaseResponseREST createPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequest) {
        return purchaseCacheService.submitPurchase(purchaseRequest);
    }
}
