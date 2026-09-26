package com.tienda.pedidos.dto;

import java.math.BigDecimal;

public record ItemPedido(long productoId, int cantidad, BigDecimal precioUnitario) {
}