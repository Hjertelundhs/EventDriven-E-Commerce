package com.eventdrivencommerce.inventory.infrastructure.persistence;

import com.eventdrivencommerce.inventory.application.exception.ConcurrentInventoryModificationException;
import com.eventdrivencommerce.inventory.application.port.out.InventoryRepository;
import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.Sku;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class InventoryPersistenceAdapter implements InventoryRepository {

    private final SpringDataInventoryRepository repository;

    InventoryPersistenceAdapter(SpringDataInventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemJpaEntity entity = repository.findById(item.id())
                .orElseGet(() -> InventoryItemJpaEntity.create(item.id()));
        InventoryPersistenceMapper.copyToEntity(item, entity);
        try {
            return InventoryPersistenceMapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ConcurrentInventoryModificationException(item.sku().value());
        }
    }

    @Override
    public Optional<InventoryItem> findById(UUID inventoryItemId) {
        return repository.findById(inventoryItemId).map(InventoryPersistenceMapper::toDomain);
    }

    @Override
    public Optional<InventoryItem> findBySku(Sku sku) {
        return repository.findBySku(sku.value()).map(InventoryPersistenceMapper::toDomain);
    }
}
