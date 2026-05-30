package com.midas.reconciliation.scheduler;

import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    /** Flag RECEIVED records older than 1 hour as UNMATCHED — runs every 30 minutes. */
    @Scheduled(fixedDelayString = "${reconciliation.job.fixed-delay-ms:1800000}")
    public void flagStaleRecords() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        List<ReconciliationRecord> stale = reconciliationService.findUnmatched(cutoff);

        if (stale.isEmpty()) {
            log.debug("No stale reconciliation records found");
            return;
        }

        log.warn("Found {} stale reconciliation records — flagging as UNMATCHED", stale.size());
        stale.forEach(reconciliationService::markAsUnmatched);
    }
}
