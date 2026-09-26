package com.tienda.pedidos.dto;

import java.util.List;

public record PedidoRequest(long clienteId, List<ItemPedido> items,
                            boolean campanaBlackFriday, boolean admisionExcepcional) {
}