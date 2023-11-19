package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.repository.ShoppingSessionItemRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.openapi.dto.UserDto;
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
public class ShoppingSessionItemsDeleter {

    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto delete(final DeleteItemsFromShoppingSessionRequest request) {
        List<UUID> itemIds = request.getShoppingSessionItemIds();
        shoppingSessionItemRepository.deleteAllByIdInBatch(itemIds);

        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.getId();

        return shoppingSessionProvider.getByUserId(userId);
    }
}
