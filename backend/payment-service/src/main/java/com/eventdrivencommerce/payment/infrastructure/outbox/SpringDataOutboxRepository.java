package com.eventdrivencommerce.payment.infrastructure.outbox;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity,UUID>{@Query(value="SELECT * FROM outbox_events WHERE status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT :limit",nativeQuery=true)List<OutboxJpaEntity>lockPending(@Param("limit")int limit);}
