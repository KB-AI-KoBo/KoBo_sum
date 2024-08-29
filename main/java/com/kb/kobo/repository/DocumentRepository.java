package com.kb.kobo.repository;

import com.kb.kobo.entity.User;
import com.kb.kobo.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUser(User user);
    Optional<Document> findById(Long id);
}