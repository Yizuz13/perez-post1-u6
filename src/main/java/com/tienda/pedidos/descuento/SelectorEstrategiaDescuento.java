package com.tienda.pedidos.descuento;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SelectorEstrategiaDescuento {
    private final List<EstrategiaDescuento> estrategias;

    public SelectorEstrategiaDescuento(List<EstrategiaDescuento> estrategias) {
        this.estrategias = List.copyOf(estrategias);
    }

    public List<EstrategiaDescuento> estrategias() {
        return estrategias;
    }
}