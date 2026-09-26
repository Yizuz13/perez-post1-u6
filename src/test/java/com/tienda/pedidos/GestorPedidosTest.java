package com.tienda.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(GestorPedidosTest.RelojPruebaConfig.class)
class GestorPedidosTest {

    private static final BigDecimal PRECIO = new BigDecimal("100.00");

    @Autowired
    private GestorPedidos gestorPedidos;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararBaseDeDatos() {
        jdbcTemplate.update("DELETE FROM detalle_pedido");
        jdbcTemplate.update("DELETE FROM pedidos");
        jdbcTemplate.update("UPDATE inventario SET stock = 100 WHERE producto_id = 1");
        jdbcTemplate.update("UPDATE inventario SET stock = 1 WHERE producto_id = 2");
        jdbcTemplate.update("UPDATE inventario SET stock = 50 WHERE producto_id = 3");
        jdbcTemplate.update("UPDATE inventario SET stock = 100 WHERE producto_id = 4");
    }

    @Test
    void rechazaItemsVaciosYStockInsuficiente() {
        ResultadoPedido vacio = gestorPedidos.crearPedido(new PedidoRequest(3, List.of(), false, false));
        ResultadoPedido sinStock = gestorPedidos.crearPedido(
                solicitud(3, 2, 2, PRECIO, false, false));

        assertThat(vacio.exitoso()).isFalse();
        assertThat(vacio.mensaje()).contains("al menos un producto");
        assertThat(sinStock.exitoso()).isFalse();
        assertThat(sinStock.mensaje()).contains("Stock insuficiente");
        assertThat(contarPedidos()).isZero();
    }

    @Test
    void rechazaClienteInexistente() {
        ResultadoPedido resultado = gestorPedidos.crearPedido(
                solicitud(999, 1, 1, PRECIO, false, false));

        assertThat(resultado.exitoso()).isFalse();
        assertThat(resultado.mensaje()).isEqualTo("Cliente inexistente");
        assertThat(contarPedidos()).isZero();
    }

    @Test
    void bloqueaMorosoEnHorarioDeCorteYPermiteAdmisionExcepcional() {
        ResultadoPedido bloqueado = gestorPedidos.crearPedido(
                solicitud(4, 1, 1, PRECIO, false, false));
        ResultadoPedido admitido = gestorPedidos.crearPedido(
                solicitud(4, 1, 1, PRECIO, false, true));

        assertThat(bloqueado.exitoso()).isFalse();
        assertThat(bloqueado.mensaje()).contains("horario de corte");
        assertThat(admitido.exitoso()).isTrue();
        assertThat(contarPedidos()).isEqualTo(1);
    }

    @Test
    void aplicaDescuentosVipYFrecuente() {
        ResultadoPedido vip = gestorPedidos.crearPedido(
                solicitud(1, 1, 1, PRECIO, false, false));
        ResultadoPedido frecuente = gestorPedidos.crearPedido(
                solicitud(2, 1, 1, PRECIO, false, false));

        assertThat(vip.descuento()).isEqualByComparingTo("15.00");
        assertThat(frecuente.descuento()).isEqualByComparingTo("8.00");
    }

    @Test
    void seleccionaLaMejorCampanaCorporativoOVolumen() {
        ResultadoPedido blackFriday = gestorPedidos.crearPedido(
                solicitud(3, 1, 1, PRECIO, true, false));
        ResultadoPedido corporativo = gestorPedidos.crearPedido(
                solicitud(5, 1, 1, PRECIO, false, false));
        ResultadoPedido volumen = gestorPedidos.crearPedido(
                solicitud(3, 1, 21, new BigDecimal("10.00"), false, false));
        ResultadoPedido combinacion = gestorPedidos.crearPedido(
                solicitud(3, 1, 21, new BigDecimal("10.00"), true, false));

        assertThat(blackFriday.descuento()).isEqualByComparingTo("25.00");
        assertThat(corporativo.descuento()).isEqualByComparingTo("10.00");
        assertThat(volumen.descuento()).isEqualByComparingTo("25.20");
        assertThat(combinacion.descuento()).isEqualByComparingTo("52.50");
    }

    private PedidoRequest solicitud(long clienteId, long productoId, int cantidad,
                                    BigDecimal precio, boolean blackFriday, boolean excepcional) {
        return new PedidoRequest(clienteId,
                List.of(new ItemPedido(productoId, cantidad, precio)), blackFriday, excepcional);
    }

    private int contarPedidos() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class);
    }

    @TestConfiguration
    static class RelojPruebaConfig {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(Instant.parse("2026-09-26T17:30:00Z"), ZoneOffset.UTC);
        }
    }
}