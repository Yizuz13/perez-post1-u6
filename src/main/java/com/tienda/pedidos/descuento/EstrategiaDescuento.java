package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import java.math.BigDecimal;

public interface EstrategiaDescuento {
    boolean aplica(ContextoPedido contexto);
    BigDecimal porcentaje();
}