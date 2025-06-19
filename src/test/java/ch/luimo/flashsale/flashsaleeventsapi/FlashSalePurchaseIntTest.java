package ch.luimo.flashsale.flashsaleeventsapi;

import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseRequestRest;
import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseResponseREST;
import ch.luimo.flashsale.flashsaleeventsapi.controller.SourceType;
import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import ch.luimo.flashsale.flashsaleeventsapi.service.PublishingService;
import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseCacheService;
import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseRequestService;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FlashSalePurchaseIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSalePurchaseIntTest.class);

    private static final String FLASH_SALE_ID = UUID.randomUUID().toString();

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    @MockitoSpyBean
    private PurchaseCacheService purchaseCacheService;

    @MockitoSpyBean
    private PublishingService publishingService;

    @BeforeEach
    public void setup() {
        doReturn(true).when(purchaseCacheService).isEventActive(FLASH_SALE_ID);
        doReturn(100).when(purchaseCacheService).getPerCustomerPurchaseLimit(FLASH_SALE_ID);
    }

    @Test
    public void testSubmitRequest_correctlySubmitsPurchaseRequest_returnsPendingStatus() {
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(createPurchaseRequestREST(FLASH_SALE_ID));

        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.PENDING);
        verify(publishingService).publishPurchaseRequest(any());
        assertEventPublished(flashsalePurchaseResponseREST.getPurchaseRequestId());
    }

    @Test
    public void testSubmitRequest_requestRejected_returnsRejection() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST(FLASH_SALE_ID);
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("rejected");
        when(purchaseCacheService.isEventActive(FLASH_SALE_ID)).thenReturn(true);
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        verifyNoInteractions(publishingService);
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
    }

    @Test
    public void testSubmitRequest_requestConfirmed_returnsConfirmation() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST(FLASH_SALE_ID);
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("confirmed");
        when(purchaseCacheService.isEventActive(FLASH_SALE_ID)).thenReturn(true);
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        verifyNoInteractions(publishingService);
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.CONFIRMED);
    }

    @Test
    public void testSubmitRequest_requestPending_returnsPendingStatus() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST(FLASH_SALE_ID);
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("pending");
        when(purchaseCacheService.isEventActive(FLASH_SALE_ID)).thenReturn(true);
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        verifyNoInteractions(publishingService);
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.PENDING);
    }

    @Test
    public void testSubmitRequest_requestPending_confirmedWithinPolling_returnsConfirmedStatus() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST(FLASH_SALE_ID);
        when(purchaseCacheService.isEventActive(FLASH_SALE_ID)).thenReturn(true);
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenAnswer(new Answer<String>() {
                    private int count = 0;
                    @Override
                    public String answer(InvocationOnMock invocation) {
                        count++;
                        return count < 3 ? "pending" : "confirmed";
                    }
                });

        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        verifyNoInteractions(publishingService);
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.CONFIRMED);
    }

    private FlashsalePurchaseRequestRest createPurchaseRequestREST(String flashSaleEventId) {
        FlashsalePurchaseRequestRest purchaseRequest = new FlashsalePurchaseRequestRest();
        purchaseRequest.setFlashsaleEventId(flashSaleEventId);
        purchaseRequest.setRequestedAt(OffsetDateTime.now());
        purchaseRequest.setPurchaseRequestId(UUID.randomUUID().toString());
        purchaseRequest.setQuantity(1);
        purchaseRequest.setUserId(UUID.randomUUID().toString());
        purchaseRequest.setItemId(UUID.randomUUID().toString());
        purchaseRequest.setSourceType(SourceType.WEB);
        return purchaseRequest;
    }

    private void assertEventPublished(String expectedPurchaseRequestId) {
        testConsumer.subscribe(purchaseRequestsTopic);
        LOG.info("Starting await for purchase request with ID: {}", expectedPurchaseRequestId);
        try {
            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .with().pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(500));
                        for (var record : records) {
                            LOG.info("Successfully received record: key = {}, value = {}", record.key(), record.value());
                            assertThat(record.key()).isEqualTo(expectedPurchaseRequestId);
                        }
                    });
        } finally {
            LOG.info("Closing Kafka consumer");
            testConsumer.close();
        }
        LOG.info("Await finished for event: {}", expectedPurchaseRequestId);
    }
}
