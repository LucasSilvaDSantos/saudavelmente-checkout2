package br.com.saudavelmente.dao;

import br.com.saudavelmente.config.DatabaseConfig;
import br.com.saudavelmente.model.Pagamento;
import br.com.saudavelmente.model.Pedido;
import br.com.saudavelmente.model.Produto;

import java.sql.*;
import java.util.Map;

public class CheckoutDAO {

    public Pedido criarPedido(String comprador, String email, Map<Produto, Integer> itens) throws SQLException {
        int totalCentavos = 0;
        for (Map.Entry<Produto, Integer> entry : itens.entrySet()) {
            totalCentavos += entry.getKey().getPrecoCentavos() * entry.getValue();
        }

        String sqlPedido = "INSERT INTO pedidos (comprador, email, total_centavos, status) VALUES (?, ?, ?, 'PENDENTE')";
        String sqlItem = "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario_centavos) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtPedido = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                stmtPedido.setString(1, comprador);
                stmtPedido.setString(2, email);
                stmtPedido.setInt(3, totalCentavos);
                stmtPedido.executeUpdate();

                long pedidoId;
                try (ResultSet rs = stmtPedido.getGeneratedKeys()) {
                    rs.next();
                    pedidoId = rs.getLong(1);
                }

                try (PreparedStatement stmtItem = conn.prepareStatement(sqlItem)) {
                    for (Map.Entry<Produto, Integer> entry : itens.entrySet()) {
                        stmtItem.setLong(1, pedidoId);
                        stmtItem.setLong(2, entry.getKey().getId());
                        stmtItem.setInt(3, entry.getValue());
                        stmtItem.setInt(4, entry.getKey().getPrecoCentavos());
                        stmtItem.addBatch();
                    }
                    stmtItem.executeBatch();
                }

                conn.commit();
                return new Pedido(pedidoId, comprador, email, totalCentavos, "PENDENTE");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void salvarPagamento(Pagamento pag) throws SQLException {
        String sql = "INSERT INTO pagamentos (pedido_id, provedor, id_externo, metodo, status, qr_code_copia_cola, qr_code_base64) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pag.getPedidoId());
            stmt.setString(2, pag.getProvedor());
            stmt.setString(3, pag.getIdExterno());
            stmt.setString(4, pag.getMetodo());
            stmt.setString(5, pag.getStatus());
            stmt.setString(6, pag.getQrCodeCopiaCola());
            stmt.setString(7, pag.getQrCodeBase64());
            stmt.executeUpdate();
        }
    }

    public Pedido buscarPedido(long pedidoId) throws SQLException {
        String sql = "SELECT id, comprador, email, total_centavos, status FROM pedidos WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Pedido(
                            rs.getLong("id"),
                            rs.getString("comprador"),
                            rs.getString("email"),
                            rs.getInt("total_centavos"),
                            rs.getString("status")
                    );
                }
            }
        }
        return null;
    }

    // RF07: Registra evento de webhook garantindo idempotência
    public boolean registrarEventoWebhook(String eventoId, String tipo, String payload) throws SQLException {
        String sql = "INSERT IGNORE INTO eventos_pagamento (evento_externo_id, tipo_evento, payload) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eventoId);
            stmt.setString(2, tipo);
            stmt.setString(3, payload);
            return stmt.executeUpdate() > 0; // retorna false se o evento já foi processado antes
        }
    }

    public void atualizarStatusPorIdExterno(String idExterno, String novoStatus) throws SQLException {
        String sqlPag = "UPDATE pagamentos SET status = ? WHERE id_externo = ?";
        String sqlPed = "UPDATE pedidos p JOIN pagamentos pg ON p.id = pg.pedido_id SET p.status = ? WHERE pg.id_externo = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement s1 = conn.prepareStatement(sqlPag);
                 PreparedStatement s2 = conn.prepareStatement(sqlPed)) {
                s1.setString(1, novoStatus);
                s1.setString(2, idExterno);
                s1.executeUpdate();

                s2.setString(1, novoStatus);
                s2.setString(2, idExterno);
                s2.executeUpdate();
            }
        }
    }
}