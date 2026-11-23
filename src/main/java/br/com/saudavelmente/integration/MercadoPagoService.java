package br.com.saudavelmente.integration;

import br.com.saudavelmente.model.Pagamento;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MercadoPagoService {

    private static final String API_URL = "https://api.mercadopago.com/v1/payments";
    private final OkHttpClient client;
    private final String accessToken;

    public MercadoPagoService() {
        this.accessToken = System.getenv("MP_ACCESS_TOKEN");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public Pagamento criarPagamentoPix(long pedidoId, int totalCentavos, String emailComprador, String nomeComprador) throws IOException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Variável de ambiente MP_ACCESS_TOKEN não configurada.");
        }

        BigDecimal valorReais = BigDecimal.valueOf(totalCentavos)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        JSONObject payer = new JSONObject();
        payer.put("email", emailComprador);
        payer.put("first_name", nomeComprador);

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("transaction_amount", valorReais);
        bodyJson.put("description", "Pedido #" + pedidoId + " - Saudavelmente");
        bodyJson.put("payment_method_id", "pix");
        bodyJson.put("external_reference", String.valueOf(pedidoId));
        bodyJson.put("payer", payer);

        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Idempotency-Key", UUID.randomUUID().toString())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Erro Mercado Pago (" + response.code() + "): " + responseStr);
            }

            JSONObject jsonResp = new JSONObject(responseStr);
            JSONObject txData = jsonResp.getJSONObject("point_of_interaction").getJSONObject("transaction_data");

            return Pagamento.builder()
                    .pedidoId(pedidoId)
                    .provedor("MERCADO_PAGO")
                    .idExterno(String.valueOf(jsonResp.getLong("id")))
                    .metodo("PIX")
                    .status(traduzirStatus(jsonResp.getString("status")))
                    .qrCodeCopiaCola(txData.getString("qr_code"))
                    .qrCodeBase64(txData.getString("qr_code_base64"))
                    .build();
        }
    }

    public String consultarStatusPagamento(String idExterno) throws IOException {
        Request request = new Request.Builder()
                .url(API_URL + "/" + idExterno)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Falha ao consultar pagamento " + idExterno);
            }
            JSONObject json = new JSONObject(response.body().string());
            return traduzirStatus(json.getString("status"));
        }
    }

    private String traduzirStatus(String mpStatus) {
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> "APROVADO";
            case "rejected" -> "RECUSADO";
            case "cancelled" -> "CANCELADO";
            case "expired" -> "EXPIRADO";
            default -> "PENDENTE";
        };
    }
}