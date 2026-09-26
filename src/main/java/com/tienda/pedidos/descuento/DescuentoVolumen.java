package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DescuentoVolumen implements EstrategiaDescuento {
    @Override
    public boolean aplica(ContextoPedido contexto) {
        int unidades = contexto.pedido().items().stream().mapToInt(ItemPedido::cantidad).sum();
        return unidades > 20;
    }

    @Override
    public BigDecimal porcentaje() {
        return new BigDecimal("0.12");
    }
}