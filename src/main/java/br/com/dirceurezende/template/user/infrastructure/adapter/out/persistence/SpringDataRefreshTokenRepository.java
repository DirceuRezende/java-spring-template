package br.com.dirceurezende.template.user.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    @Modifying
    @Query("delete from RefreshTokenJpaEntity t where t.userId = :userId")
    void deleteAllByUserId(UUID userId);
}
