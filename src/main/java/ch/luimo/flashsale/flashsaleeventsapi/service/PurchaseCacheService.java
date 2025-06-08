package ch.luimo.flashsale.flashsaleeventsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import static ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus.PENDING;

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

    public String getPurchaseRequestStatus(String purchaseRequestId) {
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
        return hashOps.get(key, STATUS);
    }

    public String getRejectionReason(String purchaseRequestId) {
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
        return hashOps.get(key, REASON);
    }

    public void setPendingStatus(String purchaseRequestId) {
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
        hashOps.put(key, STATUS, PENDING.name().toLowerCase());
    }


}
