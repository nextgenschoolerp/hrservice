package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payslip_distributions", indexes = {
        @Index(name = "idx_payslip_payroll", columnList = "payroll_id"),
        @Index(name = "idx_payslip_staff", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayslipDistribution extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "distribution_id", updatable = false, nullable = false)
    private UUID distributionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payroll_id", nullable = false)
    private UUID payrollId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Column(name = "password_hint", length = 100)
    private String passwordHint;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(name = "sent_timestamp")
    private Instant sentTimestamp;

    @Column(name = "document_reference", length = 200)
    private String documentReference;
}
