package com.midas.notification.service;

import com.midas.common.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void notify(TransactionEvent event) {
        String subject = buildSubject(event);
        String body = buildBody(event);

        // TODO: replace with real email/push dispatch via Spring Mail or FCM
        log.info("[NOTIFICATION] To: {} | Subject: {} | Body: {}",
                event.getInitiatedBy(), subject, body);
    }

    private String buildSubject(TransactionEvent event) {
        return switch (event.getEventType()) {
            case TRANSACTION_CREATED    -> "Transaction Initiated";
            case TRANSACTION_COMPLETED  -> "Transaction Successful";
            case TRANSACTION_FAILED     -> "Transaction Failed";
            case TRANSACTION_REVERSED   -> "Transaction Reversed";
            default                     -> "Transaction Update";
        };
    }

    private String buildBody(TransactionEvent event) {
        return String.format(
                "Transaction %s of %s %s from %s to %s is now %s.",
                event.getTransactionId(),
                event.getAmount(), event.getCurrency(),
                event.getFromAccount(), event.getToAccount(),
                event.getEventType().name()
        );
    }
}
