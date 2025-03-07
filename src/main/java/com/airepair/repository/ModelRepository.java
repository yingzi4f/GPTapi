package com.airepair.repository;

import com.airepair.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ModelRepository extends JpaRepository<Model, Long> {
    boolean existsByName(String name);
    Optional<Model> findByName(String name);
}
