package br.com.saudavelmente;

import br.com.saudavelmente.dao.CheckoutDAO;
import br.com.saudavelmente.dao.ProdutoDAO;
import br.com.saudavelmente.integration.MercadoPagoService;
import br.com.saudavelmente.model.Pagamento;
import br.com.saudavelmente.model.Pedido;
import br.com.saudavelmente.model.Produto;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ProdutoDAO produtoDAO = new ProdutoDAO();
        CheckoutDAO checkoutDAO = new CheckoutDAO();
        MercadoPagoService mpService = new MercadoPagoService();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(8080);

        app.get("/api/produtos", ctx -> {
            try {
                ctx.json(produtoDAO.listarAtivos());
            } catch (Exception e) {
                ctx.status(500).json(Map.of("erro", "Erro ao listar produtos: " + e.getMessage()));
            }
        });

        app.post("/api/checkout", ctx -> {
            try {
                JSONObject body = new JSONObject(ctx.body());
                String comprador = body.optString("comprador", "").trim();
                String email = body.optString("email", "").trim();
                JSONArray itensJson = body.optJSONArray("itens");

                if (comprador.isEmpty() || email.isEmpty() || itensJson == null || itensJson.isEmpty()) {
                    ctx.status(400).json(Map.of("erro", "Informe comprador, e-mail e pelo menos um item."));
                    return;
                }

                Map<Produto, Integer> itensValidados = new LinkedHashMap<>();
                for (int i = 0; i < itensJson.length(); i++) {
                    JSONObject item = itensJson.getJSONObject(i);
                    long produtoId = item.getLong("produtoId");
                    int quantidade = item.getInt("quantidade");

                    if (quantidade <= 0) continue;

                    Produto produto = produtoDAO.buscarPorId(produtoId);
                    if (produto == null) {
                        ctx.status(400).json(Map.of("erro", "Produto inválido ou inativo: ID " + produtoId));
                        return;
                    }
                    itensValidados.put(produto, quantidade);
                }

                if (itensValidados.isEmpty()) {
                    ctx.status(400).json(Map.of("erro", "Nenhum item válido selecionado."));
                    return;
                }

                Pedido pedido = checkoutDAO.criarPedido(comprador, email, itensValidados);

                Pagamento pagamento = mpService.criarPagamentoPix(
                        pedido.getId(),
                        pedido.getTotalCentavos(),
                        pedido.getEmail(),
                        pedido.getComprador()
                );
                checkoutDAO.salvarPagamento(pagamento);

                ctx.status(201).json(Map.of(
                        "pedidoId", pedido.getId(),
                        "status", pedido.getStatus(),
                        "totalCentavos", pedido.getTotalCentavos(),
                        "pagamentoIdExterno", pagamento.getIdExterno(),
                        "qrCodeCopiaCola", pagamento.getQrCodeCopiaCola(),
                        "qrCodeBase64", pagamento.getQrCodeBase64()
                ));

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("erro", "Falha ao processar checkout: " + e.getMessage()));
            }
        });

        app.get("/api/pedidos/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Pedido pedido = checkoutDAO.buscarPedido(id);
                if (pedido == null) {
                    ctx.status(404).json(Map.of("erro", "Pedido não encontrado."));
                    return;
                }
                ctx.json(pedido);
            } catch (Exception e) {
                ctx.status(500).json(Map.of("erro", "Erro ao consultar pedido: " + e.getMessage()));
            }
        });

        app.post("/api/webhooks/mercadopago", ctx -> {
            try {
                String rawPayload = ctx.body();
                String assinaturaWebhook = ctx.header("x-signature");
                if (assinaturaWebhook == null || assinaturaWebhook.isBlank()) {
                    System.out.println("[Webhook] Notificação sem header x-signature — validando saudavelmente na API oficial do Mercado Pago.");
                }
                JSONObject json = new JSONObject(rawPayload);

                String eventoId = json.has("id") ? String.valueOf(json.get("id")) : null;
                String tipo = json.optString("type", json.optString("topic", "unknown"));

                if (eventoId != null) {
                    boolean novoEvento = checkoutDAO.registrarEventoWebhook(eventoId, tipo, rawPayload);
                    if (!novoEvento) {
                        ctx.status(200).json(Map.of("status", "evento_ja_processado"));
                        return;
                    }
                }

                if (json.has("data") && json.getJSONObject("data").has("id")) {
                    String paymentIdExterno = String.valueOf(json.getJSONObject("data").get("id"));
                    String statusReal = mpService.consultarStatusPagamento(paymentIdExterno);
                    checkoutDAO.atualizarStatusPorIdExterno(paymentIdExterno, statusReal);
                }

                ctx.status(200).json(Map.of("status", "ok"));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("erro", e.getMessage()));
            }
        });
    }
}