package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_payout_batches", indexes = {
        @Index(name = "idx_bank_batch_ref", columnList = "batch_reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankPayoutBatch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 100)
    private String batchReference;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "staff_count", nullable = false)
    private Integer staffCount;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "export_format", length = 30)
    @Builder.Default
    private String exportFormat = "NEFT_CSV";

    @Column(name = "batch_status", length = 20)
    @Builder.Default
    private String batchStatus = "EXPORTED";
}
