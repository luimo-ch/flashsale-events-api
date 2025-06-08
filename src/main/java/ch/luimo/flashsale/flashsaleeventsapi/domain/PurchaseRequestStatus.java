package ch.luimo.flashsale.flashsaleeventsapi.domain;

public enum PurchaseRequestStatus {
    PENDING, CONFIRMED, REJECTED, UNKNOWN;

    public static PurchaseRequestStatus fromString(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        try {
            return PurchaseRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    public static boolean isRejected(String status) {
        return REJECTED.equals(fromString(status));
    }

    public static boolean isPending(String status) {
        return PENDING.equals(fromString(status));
    }

    public static boolean isConfirmed(String status) {
        return CONFIRMED.equals(fromString(status));
    }

    public static boolean isUnknown(String status) {
        return UNKNOWN.equals(fromString(status));
    }

    public static boolean isConfirmedOrRejected(String status) {
        return isConfirmed(status) || isRejected(status);
    }
}
