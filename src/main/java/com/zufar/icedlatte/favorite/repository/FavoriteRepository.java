package com.zufar.icedlatte.favorite.repository;

import com.zufar.icedlatte.favorite.entity.FavoriteList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteList, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"favoriteItems", "favoriteItems.favoriteList"})
    Optional<FavoriteList> findByUserId(UUID userId);
}