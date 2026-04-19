package com.rummikub.backend.repositories;

import com.rummikub.backend.models.TableSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TableSetRepository extends JpaRepository<TableSet, UUID> {
    List<TableSet> findByGameId(UUID gameId);
    void deleteByGameId(UUID gameId);
}
