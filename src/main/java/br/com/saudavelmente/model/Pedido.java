package br.com.saudavelmente.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    private long id;
    private String comprador;
    private String email;
    private int totalCentavos;
    private String status;
}