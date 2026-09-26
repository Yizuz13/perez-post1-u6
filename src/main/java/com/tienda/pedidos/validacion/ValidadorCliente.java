package com.tienda.pedidos.validacion;

import com.tienda.pedidos.repository.PedidoRepository;
import java.time.Clock;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class ValidadorCliente extends ValidadorPedido {
    private static final LocalTime INICIO_CORTE = LocalTime.of(17, 0);
    private static final LocalTime FIN_CORTE = LocalTime.of(18, 0);
    private final PedidoRepository repository;
    private final Clock reloj;

    public ValidadorCliente(PedidoRepository repository, Clock reloj) {
        this.repository = repository;
        this.reloj = reloj;
    }

    @Override
    protected void validarRegla(ContextoPedido contexto) {
        String tipoCliente = repository.buscarTipoCliente(contexto.pedido().clienteId())
                .orElseThrow(() -> new ReglaPedidoException("Cliente inexistente"));
        contexto.tipoCliente(tipoCliente);
        LocalTime horaActual = LocalTime.now(reloj);
        boolean enHorarioDeCorte = !horaActual.isBefore(INICIO_CORTE) && horaActual.isBefore(FIN_CORTE);
        boolean moroso = repository.tieneDeudaPendiente(contexto.pedido().clienteId());
        if (moroso && enHorarioDeCorte && !contexto.pedido().admisionExcepcional()) {
            throw new ReglaPedidoException("Cliente moroso: pedido bloqueado durante el horario de corte");
        }
    }
}