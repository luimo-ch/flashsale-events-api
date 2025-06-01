package ch.luimo.flashsale.flashsaleeventsapi.controller;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlashsalePurchaseRequestRest {
    String purchaseRequestId;
    String userId;
    String itemId;
    Integer quantity;
    LocalDateTime requestedAt;
    SourceType sourceType;
}
