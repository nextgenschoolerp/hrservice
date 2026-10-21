package com.kvs.hr.repo;

import com.kvs.hr.entity.TaxDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxDeclarationRepository extends JpaRepository<TaxDeclaration, UUID> {
    Optional<TaxDeclaration> findByDeclarationIdAndTenantId(UUID declarationId, UUID tenantId);
    Optional<TaxDeclaration> findByStaffIdAndFinancialYear(UUID staffId, String financialYear);
    List<TaxDeclaration> findByTenantIdAndFinancialYear(UUID tenantId, String financialYear);
    List<TaxDeclaration> findByStaffId(UUID staffId);
}
