package com.zufar.icedlatte.favorite.entity;

import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TemporalType;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "favorite_list")
public class FavoriteList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "favoriteList",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE,
                    CascadeType.REFRESH, CascadeType.REMOVE, CascadeType.DETACH},
            orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<FavoriteItem> favoriteItems;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public void addFavoriteProduct(FavoriteItem favoriteItem) {
        if (this.favoriteItems == null) {
            this.favoriteItems = ConcurrentHashMap.newKeySet();
        }
        this.favoriteItems.add(favoriteItem);
    }

    public boolean containsFavoriteItem(UUID favoriteItemId) {
        return this.favoriteItems != null &&
                this.favoriteItems.stream().anyMatch(item -> item.getId().equals(favoriteItemId));
    }

    public void removeFavoriteItemById(UUID favoriteItemId) {
        if (this.favoriteItems != null) {
            this.favoriteItems.removeIf(item -> item.getId().equals(favoriteItemId));
        }
    }
}