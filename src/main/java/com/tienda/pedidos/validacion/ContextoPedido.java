package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;

public class ContextoPedido {
    private final PedidoRequest pedido;
    private String tipoCliente;

    public ContextoPedido(PedidoRequest pedido) {
        this.pedido = pedido;
    }

    public PedidoRequest pedido() {
        return pedido;
    }

    public String tipoCliente() {
        return tipoCliente;
    }

    public void tipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }
}