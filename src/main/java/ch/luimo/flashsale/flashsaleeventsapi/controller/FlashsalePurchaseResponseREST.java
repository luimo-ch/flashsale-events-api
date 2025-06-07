package ch.luimo.flashsale.flashsaleeventsapi.controller;

import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FlashsalePurchaseResponseREST {
    String purchaseRequestId;
    String userId;
    String itemId;
    Integer quantity;
    OffsetDateTime requestedAt;
    SourceType sourceType;
    OffsetDateTime confirmedAt;
    PurchaseRequestStatus status;
    String rejectionReason;
}
