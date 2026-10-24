package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.AnomalyType;
import com.kvs.hr.enums.RegularisationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "biometric_anomalies", indexes = {
        @Index(name = "idx_anomaly_staff_date", columnList = "staff_id, anomaly_date"),
        @Index(name = "idx_anomaly_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricAnomaly extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "anomaly_id", updatable = false, nullable = false)
    private UUID anomalyId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "anomaly_date", nullable = false)
    private LocalDate anomalyDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 30)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RegularisationStatus status = RegularisationStatus.PENDING;

    @Column(name = "regularisation_reason", length = 500)
    private String regularisationReason;

    @Column(name = "corrected_in_time")
    private LocalTime correctedInTime;

    @Column(name = "corrected_out_time")
    private LocalTime correctedOutTime;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;
}
