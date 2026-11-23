CREATE DATABASE IF NOT EXISTS saudavelmente_checkout
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE saudavelmente_checkout;

CREATE TABLE IF NOT EXISTS produtos (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    preco_centavos INT NOT NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1
    );

CREATE TABLE IF NOT EXISTS pedidos (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       comprador VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    total_centavos INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS itens_pedido (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            pedido_id BIGINT NOT NULL,
                                            produto_id BIGINT NOT NULL,
                                            quantidade INT NOT NULL,
                                            preco_unitario_centavos INT NOT NULL,
                                            CONSTRAINT fk_itens_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
    );

CREATE TABLE IF NOT EXISTS pagamentos (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          pedido_id BIGINT NOT NULL,
                                          provedor VARCHAR(50) NOT NULL,
    id_externo VARCHAR(100) NOT NULL,
    metodo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    qr_code_copia_cola TEXT,
    qr_code_base64 LONGTEXT,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagamentos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
    );

CREATE TABLE IF NOT EXISTS eventos_pagamento (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 evento_externo_id VARCHAR(100) NOT NULL UNIQUE,
    tipo_evento VARCHAR(80) NOT NULL,
    payload JSON,
    recebido_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO produtos (nome, descricao, preco_centavos, ativo) VALUES
            ('Plano Saudavelmente Plus (Mensal)', 'Acesso ilimitado ao assistente de IA e acesso antecipado a futuros recursos sem custo adicional.', 10, 1),
            ('Acesso Antecipado — Módulo em Breve', 'Reserva de acesso antecipado a novos módulos e funcionalidades em desenvolvimento no Saudavelmente.', 50, 1)