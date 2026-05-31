package com.zufar.icedlatte.favorite.entity;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "favorite_item")
public class FavoriteItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_id", nullable = false, referencedColumnName = "id")
    private FavoriteListEntity favoriteListEntity;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteItemEntity that)) return false;
        UUID favoriteListId = favoriteListId();
        UUID thatFavoriteListId = that.favoriteListId();
        if (favoriteListId == null || productId == null || thatFavoriteListId == null || that.productId == null)
            return false;
        return Objects.equals(favoriteListId, thatFavoriteListId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(favoriteListId(), productId);
    }

    private UUID favoriteListId() {
        return favoriteListEntity == null ? null : favoriteListEntity.getId();
    }
}
