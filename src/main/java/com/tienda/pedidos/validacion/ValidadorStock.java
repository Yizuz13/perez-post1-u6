package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Component;

@Component
public class ValidadorStock extends ValidadorPedido {
    private final PedidoRepository repository;

    public ValidadorStock(PedidoRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void validarRegla(ContextoPedido contexto) {
        if (contexto.pedido().items() == null || contexto.pedido().items().isEmpty()) {
            throw new ReglaPedidoException("El pedido debe incluir al menos un producto");
        }
        for (ItemPedido item : contexto.pedido().items()) {
            if (item == null || item.cantidad() <= 0 || item.precioUnitario() == null
                    || item.precioUnitario().signum() <= 0) {
                throw new ReglaPedidoException("Cada producto requiere cantidad y precio positivos");
            }
            int stock = repository.obtenerStock(item.productoId());
            if (stock < item.cantidad()) {
                throw new ReglaPedidoException("Stock insuficiente para el producto " + item.productoId());
            }
        }
    }
}