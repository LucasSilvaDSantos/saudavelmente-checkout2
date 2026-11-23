# Saudavelmente Checkout — Integração Pix com Mercado Pago

Aplicação web desenvolvida em **Java 17 + Javalin + MySQL** para processamento de checkout via **Pix**, integrada à API oficial do **Mercado Pago** com confirmação via **Webhooks**, idempotência e atualização reativa na interface.

---

## 1. Arquitetura e Fluxo de Pagamento

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant Front as Frontend (HTML/Tailwind/JS)
    participant API as Backend Javalin (Java 17)
    participant DB as MySQL
    participant MP as API Mercado Pago

    Cliente->>Front: Seleciona plano e informa nome/e-mail
    Front->>API: POST /api/checkout
    API->>DB: Busca preço real do produto (evita manipulação)
    API->>DB: Grava pedido e itens (status: PENDENTE)
    API->>MP: POST /v1/payments (Pix + X-Idempotency-Key)
    MP-->>API: Retorna ID externo, QR Code Base64 e Copia e Cola
    API->>DB: Salva pagamento vinculado ao pedido
    API-->>Front: HTTP 201 + dados do QR Code
    Front-->>Cliente: Exibe QR Code e inicia polling (GET /api/pedidos/{id})

    Cliente->>MP: Realiza pagamento Pix no app bancário
    MP->>API: POST /api/webhooks/mercadopago (payment.updated)
    API->>DB: INSERT IGNORE em eventos_pagamento (Idempotência - RF07)
    API->>MP: GET /v1/payments/{id} (Validação ativa de autenticidade)
    MP-->>API: status: approved
    API->>DB: Atualiza status para APROVADO em pagamentos e pedidos
    Front->>API: GET /api/pedidos/{id} (Polling)
    API-->>Front: status: APROVADO
    Front-->>Cliente: Exibe "Pagamento Aprovado! Assinatura Liberada."