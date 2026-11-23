package br.com.dirceurezende.template.user.infrastructure.adapter.out.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    /** SKIP LOCKED: várias instâncias do relay pegam lotes diferentes, sem publicar a mesma linha em paralelo. */
    @Query(value = """
            select * from outbox
            where published_at is null
            order by created_at
            limit :limit
            for update skip locked""", nativeQuery = true)
    List<OutboxJpaEntity> lockPending(int limit);
}
