package com.zufar.icedlatte.favorite.repository;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteListEntity, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"favoriteItems", "favoriteItems.favoriteListEntity"})
    Optional<FavoriteListEntity> findByUserId(UUID userId);

    @Query("""
            SELECT fi FROM FavoriteItemEntity fi 
            WHERE fi.favoriteListEntity.user.id = :userId
                    """)
    List<FavoriteItemEntity> findFavoriteItemsByUserIdWithPagination(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT COUNT(fi) FROM FavoriteItemEntity fi 
            WHERE fi.favoriteListEntity.user.id = :userId
            """)
    Integer getPageQuantity(@Param("userId") UUID userId);
}