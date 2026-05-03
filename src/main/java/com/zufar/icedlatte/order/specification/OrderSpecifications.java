package com.zufar.icedlatte.order.specification;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class OrderSpecifications {

    public static Specification<Order> belongsToUser(UUID userId) {
        return (root, _, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, _, _) -> root.get("status").in(statuses);
    }

    public static Specification<Order> createdInYear(Integer year) {
        if (year == null) {
            return null;
        }
        OffsetDateTime start = LocalDate.of(year, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return (root, _, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    public static Specification<Order> createdAfter(LocalDate dateFrom) {
        if (dateFrom == null) {
            return null;
        }
        OffsetDateTime from = dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC);
        return (root, _, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdBefore(LocalDate dateTo) {
        if (dateTo == null) {
            return null;
        }
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return (root, _, cb) -> cb.lessThan(root.get("createdAt"), to);
    }
}
