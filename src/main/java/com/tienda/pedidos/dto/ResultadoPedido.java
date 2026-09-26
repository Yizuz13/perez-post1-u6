package com.tienda.pedidos.dto;

import java.math.BigDecimal;

public record ResultadoPedido(boolean exitoso, String mensaje, Long pedidoId,
                              BigDecimal subtotal, BigDecimal descuento,
                              BigDecimal impuesto, BigDecimal total) {
    public static ResultadoPedido rechazado(String mensaje) {
        return new ResultadoPedido(false, mensaje, null, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}