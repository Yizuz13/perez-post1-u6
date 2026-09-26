package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CalculadorDescuentoFinal {
    private final SelectorEstrategiaDescuento selector;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selector) {
        this.selector = selector;
    }

    public BigDecimal calcular(ContextoPedido contexto, BigDecimal subtotal) {
        BigDecimal mayorPorcentaje = selector.estrategias().stream()
                .filter(estrategia -> estrategia.aplica(contexto))
                .map(EstrategiaDescuento::porcentaje)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        return subtotal.multiply(mayorPorcentaje).setScale(2, RoundingMode.HALF_UP);
    }
}