/**
 * TODO: Consider making this package internal in the future.
 * Currently exposed because cart's ShoppingCartItemDtoConverter uses ProductInfoDtoConverter
 * via MapStruct's "uses" mechanism. This is a cross-module converter dependency.
 * To make internal:
 * 1. Move product DTO conversion responsibility into ProductService (return DTOs directly).
 * 2. Cart would call ProductService.getProductsByIds() and map the response itself.
 * This becomes natural to fix after product.entity is made internal (same effort).
 */
@org.springframework.modulith.NamedInterface("converter")
package com.zufar.icedlatte.product.converter;
