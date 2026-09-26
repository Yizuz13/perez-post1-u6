package com.tienda.pedidos.repository;

import com.tienda.pedidos.dto.ItemPedido;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepository {
    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int obtenerStock(long productoId) {
        return jdbcTemplate.query("SELECT stock FROM inventario WHERE producto_id = ?", rs ->
                rs.next() ? rs.getInt("stock") : 0, productoId);
    }

    public Optional<String> buscarTipoCliente(long clienteId) {
        List<String> tipos = jdbcTemplate.query("SELECT tipo_cliente FROM clientes WHERE id = ?",
                (rs, rowNum) -> rs.getString("tipo_cliente"), clienteId);
        return tipos.stream().findFirst();
    }

    public boolean tieneDeudaPendiente(long clienteId) {
        Boolean existe = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM facturas WHERE cliente_id = ? AND pagada = FALSE)",
                Boolean.class, clienteId);
        return Boolean.TRUE.equals(existe);
    }

    public long guardarPedido(long clienteId, BigDecimal subtotal, BigDecimal descuento,
                              BigDecimal impuesto, BigDecimal total) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator statement = connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) "
                            + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'CREADO')",
                    Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, clienteId);
            preparedStatement.setBigDecimal(2, subtotal);
            preparedStatement.setBigDecimal(3, descuento);
            preparedStatement.setBigDecimal(4, impuesto);
            preparedStatement.setBigDecimal(5, total);
            return preparedStatement;
        };
        jdbcTemplate.update(statement, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No fue posible obtener el identificador del pedido");
        }
        return key.longValue();
    }

    public void guardarDetalle(long pedidoId, ItemPedido item) {
        jdbcTemplate.update("INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                pedidoId, item.productoId(), item.cantidad());
    }

    public void descontarStock(ItemPedido item) {
        int filas = jdbcTemplate.update(
                "UPDATE inventario SET stock = stock - ? WHERE producto_id = ? AND stock >= ?",
                item.cantidad(), item.productoId(), item.cantidad());
        if (filas != 1) {
            throw new IllegalStateException("El inventario cambió y ya no alcanza para completar el pedido");
        }
    }
}