package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

@Service
public class NotificacionPedidoService {
    private final EmailService emailService;

    public NotificacionPedidoService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void notificarPedidoCreado(long clienteId, long pedidoId) {
        emailService.enviar("cliente-" + clienteId, "Pedido creado",
                "El pedido " + pedidoId + " fue registrado correctamente");
    }
}