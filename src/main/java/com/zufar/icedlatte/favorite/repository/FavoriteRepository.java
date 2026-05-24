package com.zufar.icedlatte.favorite.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteListEntity, UUID> {

    @EntityGraph(
            type = EntityGraph.EntityGraphType.FETCH,
            attributePaths = {"favoriteItems"})
    Optional<FavoriteListEntity> findByUserId(UUID userId);
}
