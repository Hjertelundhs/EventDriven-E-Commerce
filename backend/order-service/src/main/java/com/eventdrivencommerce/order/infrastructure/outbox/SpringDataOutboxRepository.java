package com.eventdrivencommerce.order.infrastructure.outbox;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity,UUID>{@Query(value="SELECT * FROM outbox_events WHERE status='PENDING' AND next_attempt_at<=now() ORDER BY created_at LIMIT :size FOR UPDATE SKIP LOCKED",nativeQuery=true)List<OutboxJpaEntity> lockPending(@Param("size")int size);}
