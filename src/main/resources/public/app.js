let pollingInterval = null;

document.addEventListener("DOMContentLoaded", carregarProdutos);

async function carregarProdutos() {
    const container = document.getElementById("produtos-container");
    try {
        const res = await fetch("/api/produtos");
        const dados = await res.json();

        if (!res.ok) {
            throw new Error(dados.erro || "Falha ao buscar produtos");
        }

        container.innerHTML = dados.map(p => {
            const centavos = p.precoCentavos ?? p.valorCentavos ?? p.preco ?? 0;
            const valorFormatado = (centavos / 100).toLocaleString("pt-BR", {
                style: "currency",
                currency: "BRL"
            });

            return `
                <div class="border border-slate-200 hover:border-emerald-500 rounded-xl p-5 flex flex-col justify-between transition">
                    <div>
                        <h2 class="text-lg font-bold text-slate-900">${p.nome}</h2>
                        <p class="text-sm text-slate-500 mt-1">${p.descricao || ""}</p>
                    </div>
                    <div class="mt-4">
                        <div class="text-2xl font-extrabold text-emerald-600 mb-3">${valorFormatado}</div>
                        <button onclick="iniciarCheckout(${p.id})"
                            class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2.5 rounded-lg transition">
                            Assinar com Pix
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    } catch (err) {
        container.innerHTML = `<p class="text-red-500 text-sm">Erro ao carregar planos: ${err.message}</p>`;
    }
}

async function iniciarCheckout(produtoId) {
    const comprador = document.getElementById("nome").value.trim();
    const email = document.getElementById("email").value.trim();

    if (!comprador || !email) {
        alert("Preencha nome e e-mail para continuar.");
        return;
    }

    try {
        const res = await fetch("/api/checkout", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                comprador: comprador,
                email: email,
                itens: [
                    { produtoId: produtoId, quantidade: 1 }
                ]
            })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.erro || "Erro ao gerar Pix");

        if (data.qrCodeBase64) {
            document.getElementById("qr-code-img").src = `data:image/png;base64,${data.qrCodeBase64}`;
        }
        document.getElementById("pix-copia-cola").value = data.qrCodeCopiaCola || "";
        document.getElementById("pix-container").classList.remove("hidden");

        if (data.pedidoId) {
            iniciarPollingStatus(data.pedidoId);
        }
    } catch (err) {
        alert("Erro no checkout: " + err.message);
    }
}

function iniciarPollingStatus(pedidoId) {
    if (pollingInterval) clearInterval(pollingInterval);

    pollingInterval = setInterval(async () => {
        try {
            const res = await fetch(`/api/pedidos/${pedidoId}`);
            if (!res.ok) return;
            const pedido = await res.json();

            const status = (pedido.status || "").toUpperCase();
            const badge = document.getElementById("status-badge");

            if (status === "APROVADO" || status === "PAGO" || status === "APPROVED") {
                clearInterval(pollingInterval);
                badge.className = "inline-flex items-center gap-2 bg-emerald-100 text-emerald-800 text-sm font-bold px-4 py-1.5 rounded-full mb-4";
                badge.innerHTML = "Pagamento Aprovado! Assinatura Liberada.";
            } else if (status === "RECUSADO" || status === "CANCELADO" || status === "EXPIRADO") {
                clearInterval(pollingInterval);
                badge.className = "inline-flex items-center gap-2 bg-red-100 text-red-800 text-sm font-bold px-4 py-1.5 rounded-full mb-4";
                badge.innerHTML = `Pagamento ${status.charAt(0) + status.slice(1).toLowerCase()}. Gere um novo Pix.`;
            }
        } catch (e) {
            console.error("Erro ao consultar status:", e);
        }
    }, 3000);
}

function copiarPix() {
    const input = document.getElementById("pix-copia-cola");
    input.select();
    navigator.clipboard.writeText(input.value);
    alert("Código Pix copiado!");
}