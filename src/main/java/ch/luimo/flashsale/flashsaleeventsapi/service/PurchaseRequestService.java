package ch.luimo.flashsale.flashsaleeventsapi.service;

import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseRequestRest;
import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseResponseREST;
import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import ch.luimo.flashsale.flashsaleeventsapi.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

import static ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus.*;

@Service
public class PurchaseRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseRequestService.class);

    private static final Long pollingTimeoutMillis = 3000L;
    private static final Long pollingIntervalMillis = 500L;

    private final PurchaseCacheService purchaseCacheService;
    private final PublishingService publishingService;

    public PurchaseRequestService(PurchaseCacheService purchaseCacheService, PublishingService publishingService) {
        this.purchaseCacheService = purchaseCacheService;
        this.publishingService = publishingService;
    }

    public FlashsalePurchaseResponseREST submitPurchase(FlashsalePurchaseRequestRest purchaseRequestRest) {
        String purchaseRequestId = purchaseRequestRest.getPurchaseRequestId();

//        validatePurchaseRequest(purchaseRequestRest);

        String requestStatus = purchaseCacheService.getPurchaseRequestStatus(purchaseRequestId);

        if (PurchaseRequestStatus.isRejected(requestStatus)) {
            String rejectionReason = purchaseCacheService.getRejectionReason(purchaseRequestId);
            return mapToFlashsalePurchaseRejectionREST(purchaseRequestRest, rejectionReason);
        }
        if(PurchaseRequestStatus.isConfirmed(requestStatus)) {
            return mapToFlashsalePurchaseResponseREST(purchaseRequestRest, CONFIRMED);
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
            LOG.error("Error while polling purchase request", e);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<PurchaseRequestStatus> pollResultStateAsync(String purchaseRequestId) {
        CompletableFuture<PurchaseRequestStatus> future = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        final long startTime = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check if we've exceeded the timeout
                if (System.currentTimeMillis() - startTime >= pollingTimeoutMillis) {
                    if (future.complete(PurchaseRequestStatus.PENDING)) {
                        LOG.debug("Polling timed out after 5 seconds for request {}", purchaseRequestId);
                    }
                    scheduler.shutdown();
                    return;
                }
                LOG.info("Polling purchase request state for purchaseRequestId {}", purchaseRequestId);
                String status = purchaseCacheService.getPurchaseRequestStatus(purchaseRequestId);
                LOG.info("Status for purchaseRequestId {} is {}", purchaseRequestId, status);

                if (PurchaseRequestStatus.isConfirmedOrRejected(status)) {
                    PurchaseRequestStatus resultStatus = PurchaseRequestStatus.valueOf(status.toUpperCase());
                    if (future.complete(resultStatus)) {
                        LOG.info("Purchase request {} completed with status: {}", purchaseRequestId, status);
                    }
                    scheduler.shutdown();
                }
            } catch (Exception e) {
                LOG.error("Error during polling for request {}", purchaseRequestId, e);
                if (future.completeExceptionally(e)) {
                    scheduler.shutdown();
                }
            }
        }, 0, pollingIntervalMillis, TimeUnit.MILLISECONDS);

        return future;
    }

    private void validatePurchaseRequest(FlashsalePurchaseRequestRest purchaseRequest) {
        Long flashsaleEventId = purchaseRequest.getFlashsaleEventId();
        if (!purchaseCacheService.isEventActive(flashsaleEventId)) {
            throw new BadRequestException("Flashsale event is not active or does not exist: " + flashsaleEventId);
        }

        int perCustomerPurchaseLimit = purchaseCacheService.getPerCustomerPurchaseLimit(flashsaleEventId);
        if (purchaseRequest.getQuantity() > perCustomerPurchaseLimit) {
            throw new BadRequestException("The requested purchase amount " + purchaseRequest.getQuantity() +
                    " exceeds the per-customer limit of " + perCustomerPurchaseLimit + " items!");
        }
    }

    private void publishPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequestRest) {
        publishingService.publishPurchaseRequest(purchaseRequestRest);
    }

    private FlashsalePurchaseResponseREST mapToFlashsalePurchaseRejectionREST(FlashsalePurchaseRequestRest purchaseRequestRest,
                                                                              String rejectionReason) {
        FlashsalePurchaseResponseREST response = new FlashsalePurchaseResponseREST();
        response.setPurchaseRequestId(purchaseRequestRest.getPurchaseRequestId());
        response.setFlashsaleEventId(purchaseRequestRest.getFlashsaleEventId());
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
        response.setFlashsaleEventId(purchaseRequestRest.getFlashsaleEventId());
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
