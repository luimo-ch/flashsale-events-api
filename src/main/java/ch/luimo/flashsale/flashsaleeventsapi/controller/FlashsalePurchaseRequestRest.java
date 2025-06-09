package ch.luimo.flashsale.flashsaleeventsapi.controller;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FlashsalePurchaseRequestRest {
    String purchaseRequestId;
    Long flashsaleEventId;
    String userId;
    String itemId;
    Integer quantity;
    OffsetDateTime requestedAt;
    SourceType sourceType;
}
