package ch.luimo.flashsale.flashsaleeventsapi.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/v1/purchases")
public class FlashSalePurchaseController {

    @PostMapping
    public String createPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequest) {

        return "success";
    }
}
