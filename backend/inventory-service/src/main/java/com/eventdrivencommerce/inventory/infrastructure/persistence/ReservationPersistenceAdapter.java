package com.eventdrivencommerce.inventory.infrastructure.persistence;

import com.eventdrivencommerce.inventory.application.exception.DuplicateReservationException;
import com.eventdrivencommerce.inventory.application.port.out.ReservationRepository;
import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.Sku;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class ReservationPersistenceAdapter implements ReservationRepository {

    private final SpringDataReservationRepository repository;

    ReservationPersistenceAdapter(SpringDataReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        InventoryReservationJpaEntity entity = repository.findById(reservation.id())
                .orElseGet(() -> InventoryReservationJpaEntity.create(reservation.id()));
        ReservationPersistenceMapper.copyToEntity(reservation, entity);
        try {
            return ReservationPersistenceMapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReservationException(reservation.orderId(), reservation.sku().value());
        }
    }

    @Override
    public Optional<InventoryReservation> findById(UUID reservationId) {
        return repository.findById(reservationId).map(ReservationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<InventoryReservation> findByOrderIdAndSku(UUID orderId, Sku sku) {
        return repository.findByOrderIdAndSku(orderId, sku.value()).map(ReservationPersistenceMapper::toDomain);
    }
}
