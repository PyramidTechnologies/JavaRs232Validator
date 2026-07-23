package PTI.Rs232Validator.EventListener;

import PTI.Rs232Validator.Rs232Event;

public interface BillValidatorListener {

    default void onCommunicationAttempted(
            CommunicationAttemptedEventArgs event
    ) {
    }

    default void onStateChanged(
            StateChangedEventArgs event
    ) {
    }

    default void onEventReported(
            Rs232Event event
    ) {
    }

    default void onCashboxAttached() {
    }

    default void onCashboxRemoved() {
    }

    default void onBillStacked(
            int billType
    ) {
    }

    default void onBillEscrowed(
            int billType
    ) {
    }

    default void onBarcodeDetected(
            String barcode
    ) {
    }

    default void onConnectionLost() {
    }
}