package com.midas.ledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_txn_id",    columnList = "transactionId"),
                @Index(name = "idx_ledger_account_id", columnList = "accountId"),
                @Index(name = "idx_ledger_created_at", columnList = "createdAt")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String eventType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
