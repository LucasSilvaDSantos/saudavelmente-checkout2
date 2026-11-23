package br.com.saudavelmente.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {
    private long id;
    private long pedidoId;
    private String provedor;
    private String idExterno;
    private String metodo;
    private String status;
    private String qrCodeCopiaCola;
    private String qrCodeBase64;
}