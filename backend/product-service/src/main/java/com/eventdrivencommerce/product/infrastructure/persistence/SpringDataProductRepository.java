package com.eventdrivencommerce.product.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID>,
        JpaSpecificationExecutor<ProductJpaEntity> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
}
