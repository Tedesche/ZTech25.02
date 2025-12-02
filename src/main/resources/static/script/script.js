/**
 * ZTech Pro - Main Script
 * Autor: Tedesche / Versão Final Consolidada
 */

// =================================================================================
// 1. CONFIGURAÇÕES E UTILITÁRIOS (HELPERS)
// =================================================================================

/**
 * Obtém os headers para segurança (CSRF) do Spring Security.
 * Necessário para POST, PUT e DELETE funcionarem.
 */
function getAuthHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    
    // Se não houver segurança ativada (dev local), retorna headers básicos
    if (!tokenMeta || !headerMeta) return { 'Content-Type': 'application/json' };
    
    return {
        'Content-Type': 'application/json',
        [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
    };
}

/**
 * Exibe notificações na tela. Usa Toastify se disponível, senão usa alert.
 */
function mostrarNotificacao(mensagem, tipo = 'sucesso') {
    const corFundo = tipo === 'sucesso' ? '#84cc16' : '#dc2626'; // Verde ou Vermelho
    
    if (typeof Toastify === 'function') {
        Toastify({
            text: mensagem,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: corFundo, borderRadius: "8px" }
        }).showToast();
    } else { 
        alert(mensagem); 
    }
}

/**
 * Preenche um <select> HTML com dados vindos da API.
 */
function popularSelect(selectElement, lista, placeholder, campoId = 'id') {
    if (!selectElement) return;
    
    selectElement.innerHTML = ''; 
    
    // Opção padrão (placeholder)
    const ph = document.createElement('option');
    ph.value = ""; 
    ph.textContent = placeholder; 
    ph.disabled = true; 
    ph.selected = true;
    selectElement.appendChild(ph);

    if (!lista || lista.length === 0) return;

    lista.forEach(item => {
        const option = document.createElement('option');
        
        // Tenta identificar o ID correto baseado no objeto recebido
        const valorId = item[campoId] || item.id || item.idProduto || item.idCliente || item.idServico;
        option.value = valorId; 
        
        // Monta o texto de exibição
        const textoNome = item.nome || item.nomeCliente || item.nomeFuncionario || "Item sem nome";
        
        // Se tiver preço, exibe junto (útil para produtos)
        if (item.valor !== undefined && item.nome) {
            option.textContent = `${textoNome} - R$ ${item.valor.toFixed(2)}`;
            option.dataset.preco = item.valor; // Guarda preço no dataset para uso rápido
            option.dataset.nome = textoNome;
        } else {
            option.textContent = textoNome;
        }

        selectElement.appendChild(option);
    });
}

/**
 * Busca endereço via API do ViaCEP.
 */
function buscarCep(prefixo) {
    const cepInput = document.getElementById(prefixo + 'Cep');
    if (!cepInput) return;

    let cep = cepInput.value.replace(/\D/g, '');
    if (cep.length !== 8) {
        mostrarNotificacao("CEP inválido (deve ter 8 dígitos).", "erro");
        return;
    }

    const ruaInput = document.getElementById(prefixo + 'Rua');
    if(ruaInput) ruaInput.placeholder = "Buscando...";

    fetch(`https://viacep.com.br/ws/${cep}/json/`)
        .then(response => response.json())
        .then(data => {
            if (data.erro) {
                mostrarNotificacao("CEP não encontrado!", "erro");
                ['Rua', 'Bairro', 'Cidade'].forEach(campo => {
                    const el = document.getElementById(prefixo + campo);
                    if(el) el.value = "";
                });
            } else {
                const elRua = document.getElementById(prefixo + 'Rua');
                const elBairro = document.getElementById(prefixo + 'Bairro');
                const elCidade = document.getElementById(prefixo + 'Cidade');
                
                if(elRua) elRua.value = data.logradouro;
                if(elBairro) elBairro.value = data.bairro;
                if(elCidade) elCidade.value = data.localidade;
                
                // Foca no número para o usuário digitar
                const numInput = document.getElementById(prefixo + 'Numero');
                if(numInput) numInput.focus();
            }
        }).catch(error => {
            console.error(error);
            mostrarNotificacao("Erro de conexão ao buscar CEP.", "erro");
        }).finally(() => {
             if(ruaInput && ruaInput.value === "") ruaInput.placeholder = "Rua";
        });
}

// =================================================================================
// 2. INICIALIZAÇÃO DA PÁGINA
// =================================================================================

document.addEventListener("DOMContentLoaded", function() {
    
    // 2.1. Verifica se há uma aba ativa na URL (ex: ?tab=estoque)
    const params = new URLSearchParams(window.location.search);
    const abaAtiva = params.get('tab');
    
    if (abaAtiva && typeof showSection === 'function') {
         // Pequeno delay para garantir que o HTML esteja pronto
         setTimeout(() => {
             const btn = document.querySelector(`.nav-btn[onclick*="${abaAtiva}"]`);
             if(btn) btn.click();
         }, 100);
    } else {
        // Se não houver aba, carrega a tabela de produtos se estiver visível,
        // ou deixa para quando o usuário clicar.
        const sectionEstoque = document.getElementById('estoque-section');
        if(sectionEstoque && sectionEstoque.classList.contains('active')) {
            atualizarTabelaProdutos();
        }
    }

    // 2.2. Inicializa Lógica do Carrinho (PDV)
    inicializarLogicaCarrinho();

    // 2.3. Configura botões estáticos se existirem (ex: botão novo produto na tabela)
    const btnProduto = document.getElementById('btn_novoProduto');
    if (btnProduto) {
        btnProduto.addEventListener('click', async (e) => {
            e.preventDefault();
            await carregarDadosProdutoModal(); 
            openModal('produto');
        });
    }
});

// =================================================================================
// 3. GERENCIAMENTO DE MODAIS
// =================================================================================

function openModal(type) {
    // Normaliza o ID: 'produto' vira 'modalProduto', 'os' vira 'modalOrdemServico'
    let idModal = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    
    // Ajuste específico para OS que as vezes é chamada de 'os' ou 'OrdemServico'
    if(type === 'os') idModal = 'modalOrdemServico';

    const modal = document.getElementById(idModal);

    if (modal) {
        modal.style.display = 'flex';

        // Carregamento de dados (Lazy Load) para os selects quando abre o modal
        if (type === 'OrdemServico' || type === 'os') carregarDadosOS();
        if (type === 'venda') carregarDadosVendaModal();     // Venda Simples
        if (type === 'venda2') carregarDadosVenda2();        // Venda PDV (Carrinho)
        if (type === 'produto') carregarDadosProdutoModal(); // Categorias/Marcas
        
    } else {
        console.error(`Erro: Modal com ID '${idModal}' não encontrado no HTML.`);
    }
}

function closeModal(type) {
    let idModal = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    if(type === 'os') idModal = 'modalOrdemServico';
    
    const modal = document.getElementById(idModal);
    
    if(modal) {
        modal.style.display = 'none';
        
        // Limpa IDs ocultos para evitar que um clique em "Novo" abra uma "Edição" antiga
        const mapIds = {
            'venda': 'vendaId',
            'os': 'osId',
            'OrdemServico': 'osId',
            'produto': 'prodId'
        };
        
        // Reseta o campo hidden de ID
        const key = type === 'os' ? 'OrdemServico' : type;
        if(mapIds[key]) {
            const hiddenId = document.getElementById(mapIds[key]);
            if(hiddenId) hiddenId.value = '';
        }

        // Limpa formulários (opcional, mas recomendado)
        const inputs = modal.querySelectorAll('input:not([type="hidden"]), select, textarea');
        inputs.forEach(input => input.value = '');
    }
}

// =================================================================================
// 4. FUNÇÕES DE PRODUTO E ESTOQUE
// =================================================================================

/**
 * Carrega Categorias e Marcas no Modal de Produto
 */
async function carregarDadosProdutoModal() {
    const elCat = document.getElementById('produtoCategoriaSelect');
    const elMarca = document.getElementById('produtoMarcaSelect');
    
    if(!elCat || !elMarca) return; 
    if(elCat.options.length > 1) return; // Evita recarregar se já tiver dados

    try {
        const [rCat, rMar] = await Promise.all([ fetch('/api/categorias'), fetch('/api/marcas') ]);
        if(rCat.ok) popularSelect(elCat, await rCat.json(), "Selecione Categoria", "idCategoria");
        if(rMar.ok) popularSelect(elMarca, await rMar.json(), "Selecione Marca", "idMarca");
    } catch(e) { console.log("Erro ao carregar categorias/marcas", e); }
}

/**
 * Salva ou Atualiza um Produto
 */
async function salvarProduto() {
    const id = document.getElementById('prodId').value;
    const nome = document.getElementById('prodNome').value;
    const custo = document.getElementById('prodCusto').value;
    const valor = document.getElementById('prodValor').value;
    const qtd = document.getElementById('prodQtd').value;
    const descricao = document.getElementById('prodDesc').value;
    
    const selectCategoria = document.getElementById('produtoCategoriaSelect');
    const selectMarca = document.getElementById('produtoMarcaSelect');
    
    if (!nome) { mostrarNotificacao("Nome do produto é obrigatório.", "erro"); return; }

    const produtoDTO = {
        idProduto: id ? parseInt(id) : null,
        nome: nome,
        custo: custo ? parseFloat(custo) : 0.0,
        valor: valor ? parseFloat(valor) : 0.0,
        quantidade: qtd ? parseInt(qtd) : 0,
        descricao: descricao,
        // Envia null se não estiver selecionado
        idCategoria: (selectCategoria && selectCategoria.value) ? parseInt(selectCategoria.value) : null,
        idMarca: (selectMarca && selectMarca.value) ? parseInt(selectMarca.value) : null
    };

    try {
        const response = await fetch('/produto/api/produto/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(produtoDTO)
        });

        if (response.ok) {
            mostrarNotificacao("Produto salvo com sucesso!", "sucesso");
            closeModal('produto');
            atualizarTabelaProdutos(); // Atualiza a tabela via JS sem reload
        } else {
            const erroMsg = await response.text();
            mostrarNotificacao("Erro: " + erroMsg, "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

/**
 * Deleta um Produto
 */
async function deletarProduto(id) {
    if (!confirm("Tem certeza que deseja excluir este produto?")) return;
    try {
        const response = await fetch(`/produto/api/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Produto excluído!", "sucesso");
            atualizarTabelaProdutos();
        } else {
            mostrarNotificacao("Erro ao excluir.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

/**
 * Carrega dados para edição de Produto
 */
async function editarProduto(id) {
    await carregarDadosProdutoModal(); // Garante selects preenchidos
    try {
        const response = await fetch(`/produto/api/${id}`);
        const produto = await response.json();
        
        document.getElementById('prodId').value = produto.idProduto;
        document.getElementById('prodNome').value = produto.nome;
        document.getElementById('prodCusto').value = produto.custo;
        document.getElementById('prodValor').value = produto.valor;
        document.getElementById('prodQtd').value = produto.quantidade;
        document.getElementById('prodDesc').value = produto.descricao;
        
        // Seleciona Categoria e Marca se existirem
        if(produto.idCategoria) document.getElementById('produtoCategoriaSelect').value = produto.idCategoria;
        if(produto.idMarca) document.getElementById('produtoMarcaSelect').value = produto.idMarca;
        
        openModal('produto');
    } catch (e) { mostrarNotificacao("Erro ao carregar produto.", "erro"); }
}

/**
 * FUNÇÃO PRINCIPAL: Atualiza a tabela de estoque via JavaScript
 * Chama a API e redesenha o HTML da tabela.
 */
async function atualizarTabelaProdutos() {
    const tbody = document.getElementById('tbody-produtos');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Carregando estoque...</td></tr>';

    try {
        const response = await fetch('/produto/api/produto/listar?size=100&sort=idProduto,desc');
        
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data; // Suporta Page<> ou List<>

            tbody.innerHTML = ''; 

            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Nenhum produto encontrado.</td></tr>';
                return;
            }

            lista.forEach(p => {
                const tr = document.createElement('tr');
                const valorFormatado = p.valor 
                    ? p.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) 
                    : 'R$ 0,00';

                // Mapeia os campos do DTO para a tabela
                tr.innerHTML = `
                    <td>${p.idProduto}</td>
                    <td>${p.nome}</td>
                    <td>${valorFormatado}</td>
                    <td>${p.quantidade}</td>
                    <td>${p.descricao || ''}</td>
                    <td>${p.categoria || '-'}</td>
                    <td>${p.marca || '-'}</td>
                    <td style="display: flex; gap: 5px; justify-content: center;">
                        <button class="table-btn edit" onclick="editarProduto(${p.idProduto})">Editar</button>
                        <button class="table-btn delete" onclick="deletarProduto(${p.idProduto})">Deletar</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            console.error("Erro API:", await response.text());
            tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        console.error("Erro Conexão:", error);
        tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro de conexão.</td></tr>';
    }
}

// =================================================================================
// 5. FUNÇÕES DE ORDEM DE SERVIÇO (O.S.)
// =================================================================================

async function carregarDadosOS() {
    const elCliente = document.getElementById('osCliente');
    const elServico = document.getElementById('osServico');
    const elProduto = document.getElementById('osProduto');

    if (!elCliente) return;
    if (elCliente.options.length > 1) return; // Já carregado

    elCliente.innerHTML = '<option>Carregando...</option>';
    
    try {
        const [resCli, resServ, resProd] = await Promise.all([
            fetch('/cliente/api/cliente/todos'),
            fetch('/servico/api/servico/todos'),
            fetch('/produto/api/produto/listar?size=1000') 
        ]);

        if(resCli.ok) popularSelect(elCliente, await resCli.json(), "Selecione Cliente", "idCliente");
        if(resServ.ok) popularSelect(elServico, await resServ.json(), "Selecione Serviço", "idServico");
        if(resProd.ok) {
            const dataProd = await resProd.json();
            popularSelect(elProduto, dataProd.content || dataProd, "Selecione Produto (Opcional)", "idProduto");
        }
    } catch (e) { 
        console.error("Erro ao carregar dados OS", e); 
        mostrarNotificacao("Erro ao carregar listas de seleção.", "erro");
    }
}

async function editarOS(id) {
    await carregarDadosOS(); 
    try {
        const response = await fetch(`/ordens/api/ordem/${id}`);
        if (!response.ok) throw new Error("Erro na API");
        const os = await response.json();

        const elId = document.getElementById('osId');
        if(elId) elId.value = os.idOS || os.IdOS;

        if(document.getElementById('osCliente')) document.getElementById('osCliente').value = os.idCliente;
        if(document.getElementById('osServico')) document.getElementById('osServico').value = os.idServico;
        if(document.getElementById('osProduto')) document.getElementById('osProduto').value = os.idProduto;
        if(document.getElementById('osQuantidade')) document.getElementById('osQuantidade').value = os.quantidade;
        if(document.getElementById('osStatus')) document.getElementById('osStatus').value = os.statusOS;
        if(document.getElementById('osPreco')) document.getElementById('osPreco').value = os.valor;

        openModal('OrdemServico');
    } catch (e) {
        console.error(e);
        mostrarNotificacao("Erro ao carregar OS para edição.", "erro");
    }
}

async function salvarOS() {
    const idOS = document.getElementById('osId').value;
    const idCliente = document.getElementById('osCliente').value;
    const idServico = document.getElementById('osServico').value;
    const idProduto = document.getElementById('osProduto').value;
    const qtd = document.getElementById('osQuantidade').value;
    const status = document.getElementById('osStatus').value;
    const valor = document.getElementById('osPreco').value;

    if (!idCliente || !idServico) {
        mostrarNotificacao('Cliente e Serviço são obrigatórios!', 'erro');
        return;
    }

    const dto = {
        idOS: idOS ? parseInt(idOS) : null,
        idCliente: parseInt(idCliente),
        idServico: parseInt(idServico),
        idProduto: idProduto ? parseInt(idProduto) : null,
        quantidade: parseInt(qtd || 1),
        statusOS: status,
        valor: valor ? parseFloat(valor) : 0.0,
        dataInicio: new Date().toISOString().split('T')[0],
        horaInicio: new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'})
    };

    try {
        const res = await fetch('/ordens/api/ordem/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(dto)
        });

        if (res.ok) {
            mostrarNotificacao('O.S. Salva com sucesso!', 'sucesso');
            closeModal('OrdemServico');
            setTimeout(() => location.reload(), 500); // Recarrega para atualizar tabela de OS
        } else {
            const txt = await res.text();
            mostrarNotificacao('Erro: ' + txt, 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro de conexão', 'erro'); }
}

async function deletarOS(id) {
    if (!confirm("Deseja deletar esta O.S.?")) return;
    try {
        const response = await fetch(`/ordens/api/ordem/deletar/${id}`, { 
            method: 'DELETE', 
            headers: getAuthHeaders() 
        });
        if (response.ok) {
            mostrarNotificacao("Removido com sucesso!", "sucesso");
            setTimeout(() => location.reload(), 500);
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

// =================================================================================
// 6. FUNÇÕES DE VENDA (SIMPLES E CARRINHO/PDV)
// =================================================================================

// --- 6.1 Venda Simples (Um item por vez) ---
async function carregarDadosVendaModal() {
    const elCliente = document.getElementById('vendaCliente');
    const elProduto = document.getElementById('vendaProduto');
    if (!elCliente) return;

    if (elCliente.options.length > 1) return;

    try {
        const [resCli, resProd] = await Promise.all([
            fetch('/cliente/api/cliente/todos'),
            fetch('/produto/api/produto/listar?size=1000') 
        ]);
        if(resCli.ok) popularSelect(elCliente, await resCli.json(), "Selecione Cliente", "idCliente");
        if(resProd.ok) {
            const d = await resProd.json();
            popularSelect(elProduto, d.content || d, "Selecione Produto", "idProduto");
        }
    } catch (e) { console.error(e); }
}

async function editarVenda(id) {
    await carregarDadosVendaModal(); 
    try {
        const response = await fetch(`/vendas/api/venda/${id}`);
        const venda = await response.json();

        if(document.getElementById('vendaId')) document.getElementById('vendaId').value = venda.idVenda;
        if(document.getElementById('vendaCliente')) document.getElementById('vendaCliente').value = venda.idCliente;
        if(document.getElementById('vendaProduto')) document.getElementById('vendaProduto').value = venda.idProduto;
        if(document.getElementById('vendaQuantidade')) document.getElementById('vendaQuantidade').value = venda.quantidade;

        openModal('venda');
    } catch (e) {
        mostrarNotificacao("Erro ao carregar venda", "erro");
    }
}

async function salvarVenda() {
    const id = document.getElementById('vendaId').value;
    const idCliente = document.getElementById('vendaCliente').value;
    const idProduto = document.getElementById('vendaProduto').value;
    const qtd = document.getElementById('vendaQuantidade').value;

    if (!idCliente || !idProduto || !qtd) {
        mostrarNotificacao("Preencha todos os campos.", "erro");
        return;
    }

    const vendaDTO = {
        idVenda: id ? parseInt(id) : null,
        idCliente: parseInt(idCliente),
        idProduto: parseInt(idProduto),
        quantidade: parseInt(qtd),
        dataInicio: new Date().toISOString().split('T')[0], 
        horaInicio: new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'})
    };

    try {
        const response = await fetch('/vendas/api/venda/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(vendaDTO)
        });

        if (response.ok) {
            mostrarNotificacao("Venda salva!", "sucesso");
            closeModal('venda');
            setTimeout(() => location.reload(), 500); 
        } else {
            const erro = await response.text();
            mostrarNotificacao("Erro: " + erro, "erro");
        }
    } catch (e) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

async function deletarVenda(id) {
    if (!confirm("Deletar venda?")) return;
    try {
        const response = await fetch(`/vendas/api/venda/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Venda removida!", "sucesso");
            setTimeout(() => location.reload(), 500); 
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}


// --- 6.2 Venda PDV (Carrinho de Compras) ---

let carrinho = [];

async function carregarDadosVenda2() {
    const elProd = document.getElementById('venda2ProdutoSelect'); 
    const elCli = document.getElementById('venda2ClienteSelect');
    
    if(!elProd) return;

    try {
        const [resCli, resProd] = await Promise.all([
            fetch('/cliente/api/cliente/todos'),
            fetch('/produto/api/produto/listar?size=1000') 
        ]);

        if(resCli.ok) popularSelect(elCli, await resCli.json(), "Selecione o Cliente", "idCliente");

        if(resProd.ok) {
            const data = await resProd.json();
            const lista = data.content || data;
            
            elProd.innerHTML = '<option value="">Selecione...</option>';
            lista.forEach(p => {
                const opt = document.createElement('option');
                // Value customizado: id|nome|preco para uso interno
                opt.value = `${p.idProduto}|${p.nome}|${p.valor}`;
                opt.textContent = `${p.nome} - R$ ${p.valor ? p.valor.toFixed(2) : '0.00'}`;
                elProd.appendChild(opt);
            });
        }
    } catch(e) { console.error(e); }
}

function inicializarLogicaCarrinho() {
    const btnAdd = document.getElementById('btn-adicionar-produto-carrinho');
    const btnFin = document.getElementById('btn-finalizar-venda-carrinho');
    
    if(btnAdd) {
        btnAdd.addEventListener('click', () => {
            const select = document.getElementById('venda2ProdutoSelect');
            const qtdInput = document.getElementById('venda2Quantidade');
            
            if(!select.value) { mostrarNotificacao("Selecione um produto", "erro"); return; }
            
            const [id, nome, precoStr] = select.value.split('|');
            const preco = parseFloat(precoStr) || 0;
            const qtd = parseInt(qtdInput.value);
            
            if(qtd < 1) return;

            carrinho.push({
                idProduto: id,
                nome: nome,
                preco: preco,
                quantidade: qtd,
                subtotal: preco * qtd
            });
            
            atualizarCarrinhoDisplay();
            
            qtdInput.value = 1;
            select.value = "";
        });
    }

    if(btnFin) {
        btnFin.addEventListener('click', async () => {
            if(carrinho.length === 0) { mostrarNotificacao("Carrinho vazio!", "erro"); return; }
            
            const cliId = document.getElementById('venda2ClienteSelect').value;
            if(!cliId) { mostrarNotificacao("Selecione o cliente", "erro"); return; }

            // Salva itens em loop (conforme Backend atual)
            let sucessos = 0;
            let erros = 0;

            btnFin.disabled = true;
            btnFin.textContent = "Processando...";

            for(let item of carrinho) {
                const dto = {
                    idCliente: parseInt(cliId),
                    idProduto: parseInt(item.idProduto),
                    quantidade: item.quantidade,
                    dataInicio: new Date().toISOString().split('T')[0],
                    horaInicio: new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'})
                };
                
                try {
                    const res = await fetch('/vendas/api/venda/salvar', {
                        method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto)
                    });
                    if(res.ok) sucessos++;
                    else erros++;
                } catch(e) { erros++; }
            }
            
            btnFin.disabled = false;
            btnFin.textContent = "Confirmar Venda";

            if(erros === 0) {
                mostrarNotificacao("Venda realizada com sucesso!", "sucesso");
                carrinho = [];
                atualizarCarrinhoDisplay();
                closeModal('venda2');
                setTimeout(() => location.reload(), 800);
            } else {
                mostrarNotificacao(`Concluído: ${sucessos} itens salvos, ${erros} erros. Verifique o estoque.`, "erro");
                carrinho = [];
                atualizarCarrinhoDisplay();
            }
        });
    }
}

function atualizarCarrinhoDisplay() {
    const lista = document.getElementById('carrinho-itens-lista');
    const totalSpan = document.getElementById('valor-total-carrinho');
    if(!lista) return;

    lista.innerHTML = '';
    let total = 0;

    if(carrinho.length === 0) {
        lista.innerHTML = '<p style="color: #888; text-align: center; margin-top: 10px;">Nenhum item adicionado.</p>';
        if(totalSpan) totalSpan.textContent = '0.00';
        return;
    }

    carrinho.forEach((item, idx) => {
        total += item.subtotal;
        
        const div = document.createElement('div');
        div.className = 'carrinho-item'; 
        div.style.cssText = "display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding: 8px 0;";
        
        div.innerHTML = `
            <span><strong>${item.quantidade}x</strong> ${item.nome}</span>
            <span>R$ ${item.subtotal.toFixed(2)} 
                <button onclick="removerDoCarrinho(${idx})" style="color:red; border:none; background:none; cursor:pointer; font-weight:bold; margin-left: 10px;">&times;</button>
            </span>
        `;
        lista.appendChild(div);
    });

    if(totalSpan) totalSpan.textContent = total.toFixed(2);
}

function removerDoCarrinho(idx) {
    carrinho.splice(idx, 1);
    atualizarCarrinhoDisplay();
}

// =================================================================================
// 7. FUNÇÕES DE CLIENTE, FUNCIONÁRIO E OUTROS CADASTROS
// =================================================================================

async function salvarCliente() {
    const nome = document.getElementById('cliNome').value;
    const cpf = document.getElementById('cliCPF').value;
    const email = document.getElementById('cliEmail').value;
    const telefone = document.getElementById('cliTel').value;
    
    // Endereço
    const cep = document.getElementById('cliCep').value;
    const rua = document.getElementById('cliRua').value;
    const bairro = document.getElementById('cliBairro').value;
    const numero = document.getElementById('cliNumero').value;
    const cidade = document.getElementById('cliCidade').value;

    if (!nome || !cpf) { mostrarNotificacao('Nome e CPF são obrigatórios!', 'erro'); return; }

    const dto = {
        nomeCliente: nome,
        cpf: cpf,
        endEmail: email,
        telefone: telefone,
        cep: cep,
        rua: rua,
        bairro: bairro,
        numeroCasa: parseInt(numero || 0),
        cidade: cidade
    };

    try {
        const res = await fetch('/cliente/api/cliente/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('Cliente salvo!', 'sucesso');
            closeModal('Cliente');
        } else {
            mostrarNotificacao('Erro: ' + await res.text(), 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function salvarFuncionario() {
    const nome = document.getElementById('funcNome').value;
    const cpf = document.getElementById('funcCpf').value;
    const acesso = document.getElementById('funcAcesso').value;
    const salario = document.getElementById('funcSalario').value;
    const dataNasc = document.getElementById('funcDataNasc').value;
    
    const cep = document.getElementById('funcCep').value;
    const rua = document.getElementById('funcRua').value;
    const bairro = document.getElementById('funcBairro').value;
    const numero = document.getElementById('funcNumero').value;
    const cidade = document.getElementById('funcCidade').value;

    if (!nome || !cpf) { mostrarNotificacao('Nome e CPF obrigatórios!', 'erro'); return; }

    const dto = {
        nomeFuncionario: nome, 
        cpf: cpf,
        dataNascimento: dataNasc,
        nivelAcesso: acesso,
        salario: parseFloat(salario || 0),
        cep: cep,
        rua: rua,
        bairro: bairro,
        numeroCasa: parseInt(numero || 0),
        cidade: cidade
    };

    try {
        const res = await fetch('/api/funcionarios/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('Funcionário salvo!', 'sucesso');
            closeModal('Funcionario');
        } else {
            mostrarNotificacao('Erro ao salvar funcionário.', 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function salvarServico() {
    const nome = document.getElementById('servNome').value;
    const desc = document.getElementById('servDescricao').value;
    const preco = document.getElementById('servPreco').value;

    if(!nome) { mostrarNotificacao('Nome é obrigatório', 'erro'); return; }

    const dto = {
        nome: nome,
        descricaoServico: desc,
        valor: parseFloat(preco || 0)
    };

    try {
        const res = await fetch('/servico/api/servico/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('Serviço Salvo!', 'sucesso');
            closeModal('Servico');
            document.getElementById('servNome').value = '';
        } else {
            mostrarNotificacao('Erro: ' + await res.text(), 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function salvarMarca() {
    const input = document.getElementById('nomeMarcaInput');
    const nome = input ? input.value : '';
    if (!nome) { mostrarNotificacao('Nome da marca obrigatório!', 'erro'); return; }

    try {
        const res = await fetch('/api/marcas/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify({ nome: nome })
        });
        if (res.ok) {
            mostrarNotificacao('Marca salva!', 'sucesso');
            input.value = '';
            closeModal('Marca');
        } else { mostrarNotificacao('Erro ao salvar.', 'erro'); }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function salvarCategoria() {
    const input = document.getElementById('inputNomeCategoria');
    const nome = input ? input.value : '';
    if (!nome) { mostrarNotificacao('Nome da categoria obrigatório!', 'erro'); return; }

    try {
        const res = await fetch('/api/categorias/cadastrar', { 
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify({ nome: nome })
        });
        if (res.ok) {
            mostrarNotificacao('Categoria salva!', 'sucesso');
            input.value = '';
            closeModal('Categoria');
        } else { mostrarNotificacao('Erro ao salvar.', 'erro'); }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}