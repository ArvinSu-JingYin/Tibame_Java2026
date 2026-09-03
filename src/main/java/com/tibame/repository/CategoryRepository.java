package com.tibame.repository;

import com.tibame.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE (c.isSystem = true OR c.userId = :userId) ORDER BY c.sortOrder ASC, c.id ASC")
    List<Category> findAvailableCategories(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.isSystem = true OR c.userId = :userId) AND c.type = :type ORDER BY c.sortOrder ASC, c.id ASC")
    List<Category> findAvailableCategoriesByType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.isSystem = true OR c.userId = :userId)")
    Optional<Category> findAvailableById(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndTypeAndName(Long userId, String type, String name);

    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
