package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DescuentoEstandar implements EstrategiaDescuento {
    @Override
    public boolean aplica(ContextoPedido contexto) {
        return "ESTANDAR".equals(contexto.tipoCliente());
    }

    @Override
    public BigDecimal porcentaje() {
        return BigDecimal.ZERO;
    }
}