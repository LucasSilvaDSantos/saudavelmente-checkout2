package br.com.saudavelmente.dao;

import br.com.saudavelmente.config.DatabaseConfig;
import br.com.saudavelmente.model.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public List<Produto> listarAtivos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT id, nome, descricao, preco_centavos, ativo FROM produtos WHERE ativo = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto p = new Produto(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("descricao"),
                        rs.getInt("preco_centavos"),
                        rs.getBoolean("ativo")
                );
                produtos.add(p);
            }
        }
        return produtos;
    }

    public Produto buscarPorId(long id) throws SQLException {
        String sql = "SELECT id, nome, descricao, preco_centavos, ativo FROM produtos WHERE id = ? AND ativo = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Produto(
                            rs.getLong("id"),
                            rs.getString("nome"),
                            rs.getString("descricao"),
                            rs.getInt("preco_centavos"),
                            rs.getBoolean("ativo")
                    );
                }
            }
        }
        return null;
    }
}