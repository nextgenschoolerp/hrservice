package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_cpd_records", indexes = {
        @Index(name = "idx_cpd_staff", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CpdRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cpd_id", updatable = false, nullable = false)
    private UUID cpdId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "course_title", nullable = false, length = 200)
    private String courseTitle;

    @Column(name = "conducting_agency", nullable = false, length = 150)
    private String conductingAgency;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "training_hours", nullable = false, precision = 5, scale = 1)
    private BigDecimal trainingHours;

    @Column(name = "credits_earned", nullable = false, precision = 4, scale = 1)
    private BigDecimal creditsEarned;

    @Column(name = "certificate_reference", length = 250)
    private String certificateReference;
}
