package br.com.saudavelmente.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produto {
    private long id;
    private String nome;
    private String descricao;
    private int precoCentavos;
    private boolean ativo;
}