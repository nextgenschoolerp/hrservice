package com.kvs.hr.entity;

import com.kvs.core.entity.BaseAuditEntity;
import com.kvs.hr.enums.TaxRegime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "staff_tax_declarations", indexes = {
        @Index(name = "idx_tax_decl_staff_year", columnList = "staff_id, financial_year", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclaration extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "declaration_id", updatable = false, nullable = false)
    private UUID declarationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 10)
    @Builder.Default
    private TaxRegime taxRegime = TaxRegime.NEW;

    @Column(name = "section_80c_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal section80cAmount = BigDecimal.ZERO;

    @Column(name = "section_80d_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal section80dAmount = BigDecimal.ZERO;

    @Column(name = "hra_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hraAmount = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "projected_annual_tax", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal projectedAnnualTax = BigDecimal.ZERO;

    @Column(name = "monthly_tds", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyTds = BigDecimal.ZERO;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
}
