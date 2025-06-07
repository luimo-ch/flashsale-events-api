package ch.luimo.flashsale.flashsaleeventsapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

@Service
public class PurchaseCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseCacheService.class);

    private static final String PURCHASE_CACHE_KEY_PREFIX = "purchase:";
    private static final String STATUS = "status";


    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;
    private final SetOperations<String, String> setOps;

    public PurchaseCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.setOps = redisTemplate.opsForSet();
    }

    public void submitPurchase(FlashsalePurchaseRequestRest purchaseRequestRest) {
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestRest.getPurchaseRequestId();

        hashOps.put(key, STATUS, "pending");

        LOG.info("Successfully submitted purchase for purchase request , cache entry:{}", hashOps.get(key, STATUS));
    }

}
