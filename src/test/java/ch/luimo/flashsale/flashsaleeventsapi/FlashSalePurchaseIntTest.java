package ch.luimo.flashsale.flashsaleeventsapi;

import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseRequestRest;
import ch.luimo.flashsale.flashsaleeventsapi.controller.FlashsalePurchaseResponseREST;
import ch.luimo.flashsale.flashsaleeventsapi.controller.SourceType;
import ch.luimo.flashsale.flashsaleeventsapi.domain.PurchaseRequestStatus;
import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseCacheService;
import ch.luimo.flashsale.flashsaleeventsapi.service.PurchaseRequestService;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

public class FlashSalePurchaseIntTest extends IntegrationTestBase {

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    @MockitoSpyBean
    private PurchaseCacheService purchaseCacheService;

    @Test
    public void testSubmitRequest_correctlySubmitsPurchaseRequest_returnsPendingStatus() {
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(createPurchaseRequestREST());

        // todo verify kafka msg was sent
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.PENDING);
    }

    @Test
    public void testSubmitRequest_requestRejected_returnsRejection() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST();
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("rejected");
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        // todo verify kafka msg was NOT sent
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
    }

    @Test
    public void testSubmitRequest_requestConfirmed_returnsConfirmation() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST();
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("confirmed");
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        // todo verify kafka msg was NOT sent
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.CONFIRMED);
    }

    @Test
    public void testSubmitRequest_requestPending_returnsPendingState() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST();
        when(purchaseCacheService.getPurchaseRequestStatus(purchaseRequest.getPurchaseRequestId()))
                .thenReturn("pending");
        FlashsalePurchaseResponseREST flashsalePurchaseResponseREST = purchaseRequestService.submitPurchase(purchaseRequest);

        // todo verify kafka msg was NOT sent
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.PENDING);
    }

    @Test
    public void testSubmitRequest_requestPending_confirmedWithinPolling_returnsConfirmedState() {
        FlashsalePurchaseRequestRest purchaseRequest = createPurchaseRequestREST();
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

        // todo verify NO kafka msg was published
        assertThat(flashsalePurchaseResponseREST.getStatus()).isEqualTo(PurchaseRequestStatus.CONFIRMED);
    }

    private FlashsalePurchaseRequestRest createPurchaseRequestREST() {
        FlashsalePurchaseRequestRest purchaseRequest = new FlashsalePurchaseRequestRest();
        purchaseRequest.setRequestedAt(OffsetDateTime.now());
        purchaseRequest.setPurchaseRequestId(UUID.randomUUID().toString());
        purchaseRequest.setQuantity(1);
        purchaseRequest.setUserId(UUID.randomUUID().toString());
        purchaseRequest.setItemId(UUID.randomUUID().toString());
        purchaseRequest.setSourceType(SourceType.WEB);
        return purchaseRequest;
    }
}
