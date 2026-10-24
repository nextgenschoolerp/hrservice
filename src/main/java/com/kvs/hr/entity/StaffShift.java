package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "staff_shifts", indexes = {
        @Index(name = "idx_shift_tenant_code", columnList = "tenant_id, shift_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffShift extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_id", updatable = false, nullable = false)
    private UUID shiftId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "shift_code", nullable = false, length = 50)
    private String shiftCode;

    @Column(name = "shift_name", nullable = false, length = 100)
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "grace_minutes")
    @Builder.Default
    private Integer graceMinutes = 15;

    @Column(name = "half_day_threshold_minutes")
    @Builder.Default
    private Integer halfDayThresholdMinutes = 240;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
