package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeliveryAddressDtoConverter")
class DeliveryAddressDtoConverterTest {

    private final DeliveryAddressDtoConverter converter = Mappers.getMapper(DeliveryAddressDtoConverter.class);

    @Test
    @DisplayName("maps entity to dto including default flag")
    void toDto_mapsEntityFields() {
        UUID id = UUID.randomUUID();
        DeliveryAddressEntity entity = DeliveryAddressEntity.builder()
                .id(id)
                .user(new UserEntity())
                .label("Home")
                .line("123 Coffee St")
                .city("London")
                .country("United Kingdom")
                .postcode("E1 6AN")
                .isDefault(true)
                .build();

        DeliveryAddressDto dto = converter.toDto(entity);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getLabel()).isEqualTo("Home");
        assertThat(dto.getLine()).isEqualTo("123 Coffee St");
        assertThat(dto.getCity()).isEqualTo("London");
        assertThat(dto.getCountry()).isEqualTo("United Kingdom");
        assertThat(dto.getPostcode()).isEqualTo("E1 6AN");
        assertThat(dto.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("maps request to entity without copying generated fields")
    void toEntity_mapsRequestAndIgnoresManagedFields() {
        DeliveryAddressRequest request = new DeliveryAddressRequest(
                "Office",
                "5 Bean Road",
                "Manchester",
                "United Kingdom",
                "M1 1AA"
        );

        DeliveryAddressEntity entity = converter.toEntity(request);

        assertThat(entity.getLabel()).isEqualTo("Office");
        assertThat(entity.getLine()).isEqualTo("5 Bean Road");
        assertThat(entity.getCity()).isEqualTo("Manchester");
        assertThat(entity.getCountry()).isEqualTo("United Kingdom");
        assertThat(entity.getPostcode()).isEqualTo("M1 1AA");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getUser()).isNull();
        assertThat(entity.isDefault()).isFalse();
    }
}
