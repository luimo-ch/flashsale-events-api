package ch.luimo.flashsale.flashsaleeventsapi.controller;

import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

import static ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus.*;

@Service
public class PurchaseCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseCacheService.class);

    private static final String PURCHASE_CACHE_KEY_PREFIX = "purchase:";
    private static final String STATUS = "status";
    private static final String REASON = "reason";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;
    private final SetOperations<String, String> setOps;

    public PurchaseCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.setOps = redisTemplate.opsForSet();
    }

    public FlashsalePurchaseResponseREST submitPurchase(FlashsalePurchaseRequestRest purchaseRequestRest) {
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestRest.getPurchaseRequestId();

        String requestStatus = hashOps.get(key, STATUS);
        if(REJECTED.equals(requestStatus)) {
            String rejectionReason = hashOps.get(key, REASON);
            return mapToFlashsalePurchaseRejectionREST(purchaseRequestRest, REJECTED, rejectionReason);
        }

        if (StringUtils.isEmpty(requestStatus)) {
            hashOps.put(key, STATUS, PENDING.name().toLowerCase());
            publishPurchaseRequest(purchaseRequestRest);
            LOG.info("Successfully submitted purchase for purchase request with ID {}",  purchaseRequestRest.getPurchaseRequestId());
        }
        PurchaseRequestStatus purchaseRequestStatus = startPollingPurchaseStatus(key);

        return mapToFlashsalePurchaseResponseREST(purchaseRequestRest, purchaseRequestStatus);
    }

    public PurchaseRequestStatus startPollingPurchaseStatus(String cacheKey) {
        try {
            return pollResultStateAsync(cacheKey).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while submitting purchase request", e);
            throw new RuntimeException(e);
        }
    }

    private void publishPurchaseRequest(FlashsalePurchaseRequestRest purchaseRequestRest) {
        // publish event to kafka
    }

    public Future<PurchaseRequestStatus> pollResultStateAsync(String cacheKey) {
        CompletableFuture<PurchaseRequestStatus> future = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final long timeoutMillis = 5000;
        final long pollingIntervalMillis = 500;

        ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
            LOG.info("Polling for purchase request state for cacheKey {}", cacheKey);
            String status = hashOps.get(cacheKey, STATUS);
            if (CONFIRMED.name().equals(status) || REJECTED.name().equals(status)) {
                future.complete(PurchaseRequestStatus.valueOf(status));
            }
        }, 0, pollingIntervalMillis, TimeUnit.MILLISECONDS);

        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.complete(PENDING);
            }
            pollingTask.cancel(true);
            scheduler.shutdown();
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        return future;
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

    private FlashsalePurchaseResponseREST mapToFlashsalePurchaseRejectionREST(FlashsalePurchaseRequestRest purchaseRequestRest,
                                                                              PurchaseRequestStatus status,
                                                                              String rejectionReason) {
        FlashsalePurchaseResponseREST response = new FlashsalePurchaseResponseREST();
        response.setPurchaseRequestId(purchaseRequestRest.getPurchaseRequestId());
        response.setQuantity(purchaseRequestRest.getQuantity());
        response.setRequestedAt(purchaseRequestRest.getRequestedAt());
        response.setSourceType(purchaseRequestRest.getSourceType());
        response.setConfirmedAt(OffsetDateTime.now());
        response.setItemId(purchaseRequestRest.getItemId());
        response.setUserId(purchaseRequestRest.getUserId());
        response.setStatus(status);
        response.setRejectionReason(rejectionReason);
        return response;
    }


}
