package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailConsolaService implements EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConsolaService.class);

    @Override
    public void enviar(String destinatario, String asunto, String mensaje) {
        LOGGER.info("Email simulado para {} | {} | {}", destinatario, asunto, mensaje);
    }
}