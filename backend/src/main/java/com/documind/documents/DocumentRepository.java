package com.documind.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    @Transactional
    long deleteByIdAndUserId(UUID id, UUID userId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update Document d
               set d.status = 'READY', d.pageCount = :pages, d.chunkCount = :chunks, d.errorMessage = null
             where d.id = :id
            """)
    int markReady(@Param("id") UUID id, @Param("pages") int pages, @Param("chunks") int chunks);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Document d set d.status = 'FAILED', d.errorMessage = :message where d.id = :id")
    int markFailed(@Param("id") UUID id, @Param("message") String message);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Document d set d.status = 'FAILED', d.errorMessage = :message where d.status = 'PROCESSING'")
    int failAllProcessing(@Param("message") String message);
}
