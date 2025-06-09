package ch.luimo.flashsale.flashsaleeventsapi;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseCacheService;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSaleEventsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Autowired
    PurchaseCacheService cacheService;

    @Test
    public void testPublishFlashSaleEvent_startAndEndEvent_shouldAddAndRemoveFromCache() {
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();
        assertFalse(cacheService.isEventActive(avroFlashSaleEvent.getId()));

        testProducer.publishEvent(avroFlashSaleEvent);

        LOG.info("Test event published: {}", avroFlashSaleEvent);
        assertEventReceivedAndCached(avroFlashSaleEvent.getId());

        avroFlashSaleEvent.setEventStatus(AvroEventStatus.ENDED);
        testProducer.publishEvent(avroFlashSaleEvent);
        LOG.info("Test event published: {}", avroFlashSaleEvent);
        assertEventReceivedAndRemoved(avroFlashSaleEvent.getId());
    }

    private void assertEventReceivedAndCached(Long expectedEventId) {
        assertFalse(cacheService.isEventActive(expectedEventId));

        LOG.info("Starting assertEventReceivedAndCached for event with ID: {}", expectedEventId);
        pollAndAssert(() -> {
            LOG.info("Checking cache added event with ID: {}", expectedEventId);
            cacheService.printEvent(expectedEventId);
            assertTrue(cacheService.isEventActive(expectedEventId));
        });
        LOG.info("Finished assertEventReceivedAndCached for event: {}", expectedEventId);
    }

    private void assertEventReceivedAndRemoved(Long expectedEventId) {
        LOG.info("Starting assertEventReceivedAndRemoved for event with ID: {}", expectedEventId);
        pollAndAssert(() -> {
            LOG.info("Checking cache removed event ID: {}", expectedEventId);
            cacheService.printEvent(expectedEventId);
            assertFalse(cacheService.isEventActive(expectedEventId));
        });
        LOG.info("Finished assertEventReceivedAndRemoved for event: {}", expectedEventId);
    }

    private void pollAndAssert(ThrowingRunnable assertion) {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .with().pollInterval(Duration.ofMillis(500))
                .untilAsserted(assertion);
    }

    private AvroFlashSaleEvent flashSaleEventOf() {
        return AvroFlashSaleEvent.newBuilder()
                .setId(1L)
                .setEventName("test event")
                .setStartTime(Instant.now())
                .setDuration(3600)
                .setProductId(UUID.randomUUID().toString())
                .setSellerId(UUID.randomUUID().toString())
                .setStockQuantity(1000)
                .setMaxPerCustomer(10)
                .setEventStatus(AvroEventStatus.STARTED)
                .build();
    }
}
