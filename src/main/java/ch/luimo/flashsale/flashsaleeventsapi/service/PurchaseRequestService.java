package ch.luimo.flashsale.flashsaleeventsapi.service;

import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseRequestRest;
import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseResponseREST;
import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

import static ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus.*;

@Service
public class PurchaseRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseRequestService.class);

    private final PurchaseCacheService purchaseCacheService;

    public PurchaseRequestService(PurchaseCacheService purchaseCacheService) {
        this.purchaseCacheService = purchaseCacheService;
    }

    public FlashsalePurchaseResponseREST submitPurchase(FlashsalePurchaseRequestRest purchaseRequestRest) {
        String purchaseRequestId = purchaseRequestRest.getPurchaseRequestId();
        String requestStatus = purchaseCacheService.getPurchaseRequestStatus(purchaseRequestId);
        if (PurchaseRequestStatus.isRejected(requestStatus)) {
            String rejectionReason = purchaseCacheService.getRejectionReason(purchaseRequestId);
            return mapToFlashsalePurchaseRejectionREST(purchaseRequestRest, rejectionReason);
        }

        if (PurchaseRequestStatus.isUnknown(requestStatus)) {
            purchaseCacheService.setPendingStatus(purchaseRequestId);
            publishPurchaseRequest(purchaseRequestRest);
            LOG.info("Successfully submitted purchase for purchase request with ID {}", purchaseRequestId);
        }
        PurchaseRequestStatus purchaseRequestStatus = startPollingPurchaseStatus(purchaseRequestId);
        return mapToFlashsalePurchaseResponseREST(purchaseRequestRest, purchaseRequestStatus);
    }

    public PurchaseRequestStatus startPollingPurchaseStatus(String purchaseRequestId) {
        try {
            return pollResultStateAsync(purchaseRequestId).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while submitting purchase request", e);
            throw new RuntimeException(e);
        }
    }

    public Future<PurchaseRequestStatus> pollResultStateAsync(String purchaseRequestId) {
        CompletableFuture<PurchaseRequestStatus> future = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final long timeoutMillis = 5000;
        final long pollingIntervalMillis = 500;

        ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
            LOG.info("Polling purchase request state for purchaseRequestId {}", purchaseRequestId);
            String status = purchaseCacheService.getPurchaseRequestStatus(purchaseRequestId);
            if (PurchaseRequestStatus.isConfirmedOrRejected(status)) {
                LOG.info("Purchase request {} was processed. Purchase request is {}", purchaseRequestId, status);
                future.complete(PurchaseRequestStatus.valueOf(status));
            }
        }, 0, pollingIntervalMillis, TimeUnit.MILLISECONDS);

        scheduler.schedule(() -> {
            if (!future.isDone()) {
                LOG.info("Purchase request {} is still being processed. Will return PENDING, client must poll again...", purchaseRequestId);
                future.complete(PENDING);
            }
            pollingTask.cancel(true);
            scheduler.shutdown();
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        return future;
    }

    private void publishPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequestRest) {
        // publish event to kafka
        LOG.info("Publishing to kafka purchase request with ID {}", purchaseRequestRest.getPurchaseRequestId());
    }

    private FlashsalePurchaseResponseREST mapToFlashsalePurchaseRejectionREST(FlashsalePurchaseRequestRest purchaseRequestRest,
                                                                              String rejectionReason) {
        FlashsalePurchaseResponseREST response = new FlashsalePurchaseResponseREST();
        response.setPurchaseRequestId(purchaseRequestRest.getPurchaseRequestId());
        response.setQuantity(purchaseRequestRest.getQuantity());
        response.setRequestedAt(purchaseRequestRest.getRequestedAt());
        response.setSourceType(purchaseRequestRest.getSourceType());
        response.setConfirmedAt(OffsetDateTime.now());
        response.setItemId(purchaseRequestRest.getItemId());
        response.setUserId(purchaseRequestRest.getUserId());
        response.setStatus(REJECTED);
        response.setRejectionReason(rejectionReason);
        return response;
    }

    private FlashsalePurchaseResponseREST mapToFlashsalePurchaseResponseREST(FlashsalePurchaseRequestRest purchaseRequestRest,
                                                                             PurchaseRequestStatus status) {
        FlashsalePurchaseResponseREST response = new FlashsalePurchaseResponseREST();
        response.setPurchaseRequestId(purchaseRequestRest.getPurchaseRequestId());
        response.setQuantity(purchaseRequestRest.getQuantity());
        response.setRequestedAt(purchaseRequestRest.getRequestedAt());
        response.setSourceType(purchaseRequestRest.getSourceType());
        response.setConfirmedAt(OffsetDateTime.now());
        response.setItemId(purchaseRequestRest.getItemId());
        response.setUserId(purchaseRequestRest.getUserId());
        response.setStatus(status);
        return response;
    }

}
