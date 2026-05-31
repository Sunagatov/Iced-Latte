package com.zufar.icedlatte.favorite.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteListEntity, UUID> {

    @EntityGraph(
            type = EntityGraph.EntityGraphType.FETCH,
            attributePaths = {"favoriteItems"})
    Optional<FavoriteListEntity> findByUserId(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    INSERT INTO public.favorite_list (id, user_id, updated_at)
                    VALUES (:id, :userId, current_timestamp)
                    ON CONFLICT (user_id) DO NOTHING
                    """, nativeQuery = true)
    void insertFavoriteListIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    INSERT INTO public.favorite_item (id, favorite_id, product_id, version)
                    VALUES (:id, :favoriteListId, :productId, 0)
                    ON CONFLICT (favorite_id, product_id) DO NOTHING
                    """, nativeQuery = true)
    void insertFavoriteItemIfAbsent(
            @Param("id") UUID id, @Param("favoriteListId") UUID favoriteListId, @Param("productId") UUID productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = "UPDATE public.favorite_list SET updated_at = current_timestamp WHERE id = :favoriteListId",
            nativeQuery = true)
    void touchFavoriteList(@Param("favoriteListId") UUID favoriteListId);
}
