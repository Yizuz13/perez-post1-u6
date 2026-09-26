package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DescuentoFrecuente implements EstrategiaDescuento {
    @Override
    public boolean aplica(ContextoPedido contexto) {
        return "FRECUENTE".equals(contexto.tipoCliente());
    }

    @Override
    public BigDecimal porcentaje() {
        return new BigDecimal("0.08");
    }
}