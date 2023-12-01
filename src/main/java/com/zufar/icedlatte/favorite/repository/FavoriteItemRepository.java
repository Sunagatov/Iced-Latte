package com.zufar.icedlatte.favorite.repository;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItemEntity, UUID> {
}
