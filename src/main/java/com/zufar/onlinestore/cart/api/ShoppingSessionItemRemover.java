package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.RemoveItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.exception.FailedDeletingShoppingSessionItemsException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingSessionItemRemover {

    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto remove(final RemoveItemsFromShoppingSessionRequest request) throws FailedDeletingShoppingSessionItemsException {
        List<UUID> itemIds = request.shoppingSessionItemIds();

        Integer deletedItemsQuantity = shoppingSessionItemRepository.deleteShoppingSessionItemByIds(itemIds);

        if (deletedItemsQuantity < itemIds.size()) {
            log.error("Deleting the list of the shopping session items with ids = '{}' were failed.", itemIds);
            throw new FailedDeletingShoppingSessionItemsException(itemIds);
        }

        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.userId();
        return shoppingSessionProvider.getByUserId(userId);
    }
}
