package com.midas.reconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_records",
        indexes = {
                @Index(name = "idx_recon_txn_id",    columnList = "transactionId", unique = true),
                @Index(name = "idx_recon_status",     columnList = "status"),
                @Index(name = "idx_recon_event_type", columnList = "eventType")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String fromAccount;

    private String toAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus status;

    private String notes;

    private Instant eventOccurredAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant processedAt;
}
