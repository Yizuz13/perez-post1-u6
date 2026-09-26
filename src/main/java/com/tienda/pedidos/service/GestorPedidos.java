package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ReglaPedidoException;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorStock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestorPedidos {
    private static final BigDecimal TASA_IMPUESTO = new BigDecimal("0.19");
    private final ValidadorStock validadorStock;
    private final ValidadorCliente validadorCliente;
    private final CalculadorDescuentoFinal calculadorDescuento;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacionService;

    public GestorPedidos(ValidadorStock validadorStock, ValidadorCliente validadorCliente,
                         CalculadorDescuentoFinal calculadorDescuento, PedidoRepository repository,
                         NotificacionPedidoService notificacionService) {
        this.validadorStock = validadorStock;
        this.validadorCliente = validadorCliente;
        this.calculadorDescuento = calculadorDescuento;
        this.repository = repository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ResultadoPedido crearPedido(PedidoRequest pedido) {
        ContextoPedido contexto = new ContextoPedido(pedido);
        validadorStock.enlazar(validadorCliente);
        try {
            validadorStock.validar(contexto);
        } catch (ReglaPedidoException exception) {
            return ResultadoPedido.rechazado(exception.getMessage());
        }
        BigDecimal subtotal = pedido.items().stream()
                .map(item -> item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuento = calculadorDescuento.calcular(contexto, subtotal);
        BigDecimal impuesto = subtotal.subtract(descuento).multiply(TASA_IMPUESTO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuento).add(impuesto).setScale(2, RoundingMode.HALF_UP);
        long pedidoId = repository.guardarPedido(pedido.clienteId(), subtotal, descuento, impuesto, total);
        for (ItemPedido item : pedido.items()) {
            repository.guardarDetalle(pedidoId, item);
            repository.descontarStock(item);
        }
        notificacionService.notificarPedidoCreado(pedido.clienteId(), pedidoId);
        return new ResultadoPedido(true, "Pedido creado correctamente", pedidoId,
                subtotal, descuento, impuesto, total);
    }
}