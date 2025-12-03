/**
 * ZTech Pro - Main Script
 * Autor: Tedesche / Versão Final Consolidada (Correção Sintaxe, Dashboard e Vendas)
 */

// =================================================================================
// 1. CONFIGURAÇÕES E UTILITÁRIOS (HELPERS)
// =================================================================================

function getAuthHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    
    if (!tokenMeta || !headerMeta) return { 'Content-Type': 'application/json' };
    
    return {
        'Content-Type': 'application/json',
        [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
    };
}

function mostrarNotificacao(mensagem, tipo = 'sucesso') {
    const corFundo = tipo === 'sucesso' ? '#84cc16' : '#dc2626';
    
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

function popularSelect(selectElement, lista, placeholder, campoId = 'id') {
    if (!selectElement) return;
    selectElement.innerHTML = ''; 
    const ph = document.createElement('option');
    ph.value = ""; 
    ph.textContent = placeholder; 
    ph.disabled = true; 
    ph.selected = true;
    selectElement.appendChild(ph);

    if (!lista || lista.length === 0) return;

    lista.forEach(item => {
        const option = document.createElement('option');
        const valorId = item[campoId] || item.id || item.idProduto || item.idCliente || item.idServico;
        option.value = valorId; 
        
        const textoNome = item.nome || item.nomeCliente || item.nomeFuncionario || "Item sem nome";
        
        if (item.valor !== undefined && item.nome) {
            option.textContent = `${textoNome} - R$ ${item.valor.toFixed(2)}`;
            option.dataset.preco = item.valor; 
            option.dataset.nome = textoNome;
        } else {
            option.textContent = textoNome;
        }
        selectElement.appendChild(option);
    });
}

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

function gerarPaginacao(data, idContainer, funcaoCallback) {
    const container = document.getElementById(idContainer);
    if (!container) return;
    container.innerHTML = ''; 
    if (data.totalPages <= 1) return;

    const btnPrev = document.createElement('button');
    btnPrev.innerHTML = '&larr; Anterior'; 
    btnPrev.className = 'action-btn btn-secondary';
    btnPrev.disabled = data.first; 
    if (!data.first) {
        btnPrev.onclick = function() { funcaoCallback(data.number - 1); };
    }
    container.appendChild(btnPrev);

    const spanInfo = document.createElement('span');
    spanInfo.style.cssText = 'font-weight: bold; margin: 0 15px;';
    spanInfo.innerText = `Página ${data.number + 1} de ${data.totalPages}`;
    container.appendChild(spanInfo);

    const btnNext = document.createElement('button');
    btnNext.innerHTML = 'Próximo &rarr;'; 
    btnNext.className = 'action-btn btn-primary';
    btnNext.disabled = data.last; 
    if (!data.last) {
        btnNext.onclick = function() { funcaoCallback(data.number + 1); };
    }
    container.appendChild(btnNext);
}

// =================================================================================
// 2. INICIALIZAÇÃO DA PÁGINA
// =================================================================================

document.addEventListener("DOMContentLoaded", function() {
    const params = new URLSearchParams(window.location.search);
    const abaAtiva = params.get('tab');
    
    if (abaAtiva && typeof showSection === 'function') {
         setTimeout(() => {
             const btn = document.querySelector(`.nav-btn[onclick*="${abaAtiva}"]`);
             if(btn) btn.click();
         }, 100);
    } else {
        const sectionEstoque = document.getElementById('estoque-section');
        if(sectionEstoque && sectionEstoque.classList.contains('active')) atualizarTabelaProdutos();
    }

    inicializarLogicaCarrinho();

    // --- CARREGA O DASHBOARD VIA JS (Para atualizar sem reload) ---
    if (typeof carregarDashboard === 'function') {
        carregarDashboard();
    }

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
    let idModal = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    if(type === 'os') idModal = 'modalOrdemServico';

    const modal = document.getElementById(idModal);

    if (modal) {
        modal.style.display = 'flex';
        const h2 = modal.querySelector('h2');
        if (h2) {
            const titulosPadrao = {
                'produto': 'Cadastrar Novo Produto',
                'OrdemServico': 'Cadastrar Nova O.S.',
                'os': 'Cadastrar Nova O.S.',
                'venda': 'Registrar Nova Venda',
                'venda2': 'Registrar Nova Venda',
                'Funcionario': 'Cadastrar Novo Funcionário',
                'funcionario': 'Cadastrar Novo Funcionário',
                'conta': 'Cadastrar Conta a Pagar',
                'Categoria': 'Cadastrar Nova Categoria',
                'Marca': 'Cadastrar Nova Marca',
                'Cliente': 'Cadastrar Novo Cliente',
                'Servico': 'Cadastrar Novo Serviço'
            };
            if (titulosPadrao[type]) h2.textContent = titulosPadrao[type];
        }

        if (type === 'OrdemServico' || type === 'os') carregarDadosOS();
        if (type === 'venda') carregarDadosVendaModal();     
        if (type === 'venda2') carregarDadosVenda2();        
        if (type === 'produto') carregarDadosProdutoModal(); 
        
    } else {
        console.error(`Erro: Modal com ID '${idModal}' não encontrado.`);
    }
}

function closeModal(type) {
    let idModal = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    if(type === 'os') idModal = 'modalOrdemServico';
    const modal = document.getElementById(idModal);
    
    if(modal) {
        modal.style.display = 'none';
        const mapIds = { 'venda': 'vendaId', 'os': 'osId', 'OrdemServico': 'osId', 'produto': 'prodId', 'Funcionario': 'funcId', 'funcionario': 'funcId' };
        const key = type === 'os' ? 'OrdemServico' : type;
        if(mapIds[key]) {
            const hiddenId = document.getElementById(mapIds[key]);
            if(hiddenId) hiddenId.value = '';
        }
        const inputs = modal.querySelectorAll('input:not([type="hidden"]), select, textarea');
        inputs.forEach(input => input.value = '');
    }
}

// =================================================================================
// 4. FUNÇÕES DE PRODUTO, ESTOQUE
// =================================================================================

async function carregarDadosProdutoModal() {
    const elCat = document.getElementById('produtoCategoriaSelect');
    const elMarca = document.getElementById('produtoMarcaSelect');
    if(!elCat || !elMarca) return; 
    if(elCat.options.length > 1) return; 

    try {
        const [rCat, rMar] = await Promise.all([ fetch('/api/categorias'), fetch('/api/marcas') ]);
        if(rCat.ok) popularSelect(elCat, await rCat.json(), "Selecione Categoria", "idCategoria");
        if(rMar.ok) popularSelect(elMarca, await rMar.json(), "Selecione Marca", "idMarca");
    } catch(e) { console.log("Erro ao carregar categorias/marcas", e); }
}

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
        idCategoria: (selectCategoria && selectCategoria.value) ? parseInt(selectCategoria.value) : null,
        idMarca: (selectMarca && selectMarca.value) ? parseInt(selectMarca.value) : null
    };

    try {
        const response = await fetch('/produto/api/produto/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(produtoDTO)
        });
        if (response.ok) {
            mostrarNotificacao("Produto salvo com sucesso!", "sucesso");
            closeModal('produto');
            atualizarTabelaProdutos(); 
            carregarDashboard(); // Atualiza dashboard
        } else {
            const erroMsg = await response.text();
            mostrarNotificacao("Erro: " + erroMsg, "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

async function deletarProduto(id) {
    if (!confirm("Tem certeza que deseja excluir este produto?")) return;
    try {
        const response = await fetch(`/produto/api/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Produto excluído!", "sucesso");
            atualizarTabelaProdutos();
            carregarDashboard();
        } else {
            mostrarNotificacao("Erro ao excluir.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

async function editarProduto(id) {
    await carregarDadosProdutoModal(); 
    try {
        const response = await fetch(`/produto/api/${id}`);
        const produto = await response.json();
        document.getElementById('prodId').value = produto.idProduto;
        document.getElementById('prodNome').value = produto.nome;
        document.getElementById('prodCusto').value = produto.custo;
        document.getElementById('prodValor').value = produto.valor;
        document.getElementById('prodQtd').value = produto.quantidade;
        document.getElementById('prodDesc').value = produto.descricao;
        if(produto.idCategoria) document.getElementById('produtoCategoriaSelect').value = produto.idCategoria;
        if(produto.idMarca) document.getElementById('produtoMarcaSelect').value = produto.idMarca;
        openModal('produto');
        const h2 = document.querySelector('#modalProduto h2');
        if(h2) h2.textContent = 'Editar Produto';
    } catch (e) { mostrarNotificacao("Erro ao carregar produto.", "erro"); }
}

async function atualizarTabelaProdutos() {
    const tbody = document.getElementById('tbody-produtos');
    if (!tbody) return;
    const termo = document.getElementById('buscaProdutoInput') ? document.getElementById('buscaProdutoInput').value : '';
    const idCat = document.getElementById('filtroCategoria') ? document.getElementById('filtroCategoria').value : '';
    const idMarca = document.getElementById('filtroMarca') ? document.getElementById('filtroMarca').value : '';

    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Carregando estoque...</td></tr>';

    try {
        const params = new URLSearchParams({ size: 20, sort: 'idProduto,desc' });
        if (termo) params.append('termo', termo);
        if (idCat) params.append('idCategoria', idCat);
        if (idMarca) params.append('idMarca', idMarca);

        const response = await fetch(`/produto/api/produto/listar?${params.toString()}`);
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data;
            tbody.innerHTML = '';
            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Nenhum produto encontrado.</td></tr>';
                return;
            }
            lista.forEach(p => { 
                const tr = document.createElement('tr');
                const valorFormatado = p.valor ? p.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : 'R$ 0,00';
                 tr.innerHTML = `<td>${p.idProduto}</td><td>${p.nome}</td><td>${valorFormatado}</td><td>${p.quantidade}</td><td>${p.descricao || ''}</td><td>${p.categoria || '-'}</td><td>${p.marca || '-'}</td><td style="display: flex; gap: 5px; justify-content: center;"><button class="table-btn edit" onclick="editarProduto(${p.idProduto})">Editar</button><button class="table-btn delete" onclick="deletarProduto(${p.idProduto})">Deletar</button></td>`;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-produtos', atualizarTabelaProdutos);
        } else { tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>'; }
    } catch (error) { tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro de conexão.</td></tr>'; }
}

document.getElementById('buscaProdutoInput')?.addEventListener('keypress', function (e) { if (e.key === 'Enter') atualizarTabelaProdutos(); });

// =================================================================================
// 5. FUNÇÕES DE ORDEM DE SERVIÇO (O.S.)
// =================================================================================

async function carregarDadosOS() {
    const elCliente = document.getElementById('osCliente');
    const elServico = document.getElementById('osServico');
    const elProduto = document.getElementById('osProduto');
    if (!elCliente || elCliente.options.length > 1) return; 
    elCliente.innerHTML = '<option>Carregando...</option>';
    try {
        const [resCli, resServ, resProd] = await Promise.all([
            fetch('/cliente/api/cliente/todos'), fetch('/servico/api/servico/todos'), fetch('/produto/api/produto/listar?size=1000') 
        ]);
        if(resCli.ok) popularSelect(elCliente, await resCli.json(), "Selecione Cliente", "idCliente");
        if(resServ.ok) popularSelect(elServico, await resServ.json(), "Selecione Serviço", "idServico");
        if(resProd.ok) {
            const dataProd = await resProd.json();
            popularSelect(elProduto, dataProd.content || dataProd, "Selecione Produto (Opcional)", "idProduto");
        }
    } catch (e) { mostrarNotificacao("Erro ao carregar listas de seleção.", "erro"); }
}

async function editarOS(id) {
    await carregarDadosOS(); 
    try {
        const response = await fetch(`/ordens/api/ordem/${id}`);
        if (!response.ok) throw new Error("Erro na API");
        const os = await response.json();
        let elId = document.getElementById('osId');
        if(!elId) {
             elId = document.createElement('input'); elId.type = 'hidden'; elId.id = 'osId';
             document.querySelector('#modalOrdemServico .modal-content').appendChild(elId);
        }
        elId.value = os.idOS || os.IdOS;
        if(document.getElementById('osCliente')) document.getElementById('osCliente').value = os.idCliente;
        if(document.getElementById('osServico')) document.getElementById('osServico').value = os.idServico;
        if(document.getElementById('osProduto')) document.getElementById('osProduto').value = os.idProduto;
        if(document.getElementById('osQuantidade')) document.getElementById('osQuantidade').value = os.quantidade;
        if(document.getElementById('osStatus')) document.getElementById('osStatus').value = os.statusOS;
        if(document.getElementById('osPreco')) document.getElementById('osPreco').value = os.valor;
        openModal('OrdemServico');
        const h2 = document.querySelector('#modalOrdemServico h2');
        if(h2) h2.textContent = 'Editar O.S.';
    } catch (e) { mostrarNotificacao("Erro ao carregar OS para edição.", "erro"); }
}

async function salvarOS() {
    let elId = document.getElementById('osId');
    if(!elId) {
         elId = document.createElement('input'); elId.type = 'hidden'; elId.id = 'osId';
         document.querySelector('#modalOrdemServico .modal-content').appendChild(elId);
    }
    const idOS = elId.value;
    const idCliente = document.getElementById('osCliente').value;
    const idServico = document.getElementById('osServico').value;
    const idProduto = document.getElementById('osProduto').value;
    const qtd = document.getElementById('osQuantidade').value;
    const status = document.getElementById('osStatus').value;
    const valor = document.getElementById('osPreco').value;

    if (!idCliente || !idServico) { mostrarNotificacao('Cliente e Serviço obrigatórios!', 'erro'); return; }

    // DATA: Se for edição, manda null para não alterar a data original
    let dataEnvio = null;
    let horaEnvio = null;
    if (!idOS) {
        dataEnvio = new Date().toISOString().split('T')[0];
        horaEnvio = new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'});
    }

    const dto = {
        idOS: idOS ? parseInt(idOS) : null,
        idCliente: parseInt(idCliente),
        idServico: parseInt(idServico),
        idProduto: idProduto ? parseInt(idProduto) : null,
        quantidade: parseInt(qtd || 1),
        statusOS: status,
        valor: valor ? parseFloat(valor) : 0.0,
        dataInicio: dataEnvio,
        horaInicio: horaEnvio
    };

    try {
        const res = await fetch('/ordens/api/ordem/salvar', {
            method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('O.S. Salva com sucesso!', 'sucesso');
            closeModal('OrdemServico');
            atualizarTabelaOrdens(); 
            carregarDashboard();
        } else {
            const txt = await res.text();
            mostrarNotificacao('Erro: ' + txt, 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro de conexão', 'erro'); }
}

async function deletarOS(id) {
    if (!confirm("Deseja deletar esta O.S.?")) return;
    try {
        const response = await fetch(`/ordens/api/ordem/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Removido com sucesso!", "sucesso");
            atualizarTabelaOrdens();
            carregarDashboard();
        } else { mostrarNotificacao("Erro ao remover.", "erro"); }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

async function atualizarTabelaOrdens() {
    const tbody = document.getElementById('tbody-ordens'); 
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">Carregando O.S...</td></tr>';
    try {
        const response = await fetch('/ordens/api/ordem/listar?size=20&sort=idOS,desc');
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data;
            tbody.innerHTML = '';
            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">Nenhuma O.S. encontrada.</td></tr>';
                return;
            }
            lista.forEach(os => {
                const tr = document.createElement('tr');
                const valorFormatado = os.valor ? os.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : 'R$ 0,00';
                let badgeClass = os.statusOS === 'CONCLUIDA' ? 'status-success' : (os.statusOS === 'CANCELADA' ? 'status-danger' : 'status-normal');
                tr.innerHTML = `<td>${os.idOS}</td><td>${os.nomeCliente || '-'}</td><td>${os.dataInicio || ''}</td><td>${os.dataFim || '-'}</td><td>${valorFormatado}</td><td>${os.nomeServico || '-'}</td><td>${os.nomeProduto || '-'}</td><td><span class="status-badge ${badgeClass}">${os.statusOS || 'Registrada'}</span></td><td style="display: flex; gap: 5px; justify-content: center;"><button class="table-btn edit" onclick="editarOS(${os.idOS})">Editar</button><button class="table-btn delete" onclick="deletarOS(${os.idOS})">Deletar</button></td>`;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-ordens', atualizarTabelaProdutos);
        } else { tbody.innerHTML = '<tr><td colspan="9" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>'; }
    } catch (error) { tbody.innerHTML = '<tr><td colspan="9" style="color:red; text-align:center;">Erro de conexão.</td></tr>'; }
}

// =================================================================================
// 6. FUNÇÕES DE VENDA
// =================================================================================

// --- 6.1 Venda Simples (Editar e Salvar) ---

async function carregarDadosVendaModal() {
    const elCliente = document.getElementById('vendaCliente');
    const elProduto = document.getElementById('vendaProduto');
    if (!elCliente) return; // Se não tiver modal de edição, aborta

    if (elCliente.options.length > 1) return; // Já carregou

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
    // 1. Carrega listas
    await carregarDadosVendaModal(); 
    
    try {
        // 2. Busca a venda
        const response = await fetch(`/vendas/api/venda/${id}`);
        if (!response.ok) throw new Error("Erro ao buscar venda");
        const venda = await response.json();

        // 3. Verifica HTML
        let elId = document.getElementById('vendaId');
        if(!elId) {
             console.error("Campo vendaId não encontrado!");
             return;
        }
        elId.value = venda.idVenda;

        // 4. Preenche Campos
        if(document.getElementById('vendaCliente')) document.getElementById('vendaCliente').value = venda.idCliente;
        if(document.getElementById('vendaProduto')) document.getElementById('vendaProduto').value = venda.idProduto;
        if(document.getElementById('vendaQuantidade')) document.getElementById('vendaQuantidade').value = venda.quantidade;

        openModal('venda');

        const h2 = document.querySelector('#modalVenda h2');
        if(h2) h2.textContent = 'Editar Venda';

    } catch (e) {
        console.error(e);
        mostrarNotificacao("Erro ao carregar venda.", "erro");
    }
}

async function salvarVenda() {
    const elId = document.getElementById('vendaId');
    const id = elId ? elId.value : null;
    
    const idCliente = document.getElementById('vendaCliente') ? document.getElementById('vendaCliente').value : null;
    const idProduto = document.getElementById('vendaProduto') ? document.getElementById('vendaProduto').value : null;
    const qtd = document.getElementById('vendaQuantidade') ? document.getElementById('vendaQuantidade').value : null;

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
            atualizarTabelaVendas(); 
            carregarDashboard();
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
            atualizarTabelaVendas(); 
            carregarDashboard();
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}


// --- 6.2 Venda PDV (Carrinho de Compras) ---

let carrinho = [];

async function carregarDadosVenda2() {
    const elProd = document.getElementById('produto-select');
    const elCli = document.getElementById('venda2ClienteSelect');
    try {
        const [resCli, resProd] = await Promise.all([
            fetch('/cliente/api/cliente/todos'), fetch('/produto/api/produto/listar?size=1000')
        ]);
        if (elCli && resCli.ok) popularSelect(elCli, await resCli.json(), "Selecione um Cliente", "idCliente");
        if (elProd && resProd.ok) {
            const data = await resProd.json();
            const listaProdutos = data.content || data;
            elProd.innerHTML = '<option value="">Selecione um produto</option>';
            listaProdutos.forEach(p => {
                const opt = document.createElement('option');
                const idFinal = p.idProduto || p.id || p.idServico;
                let valorNumerico = 0.00;
                if(p.valor !== undefined && p.valor !== null) {
                    let v = parseFloat(p.valor);
                    if(!isNaN(v)) valorNumerico = v;
                }
                const nomeFinal = p.nome || p.nomeProduto || "Produto sem nome";
                if(!idFinal) return; 
                opt.value = idFinal;
                opt.textContent = `${nomeFinal} - R$ ${valorNumerico.toFixed(2)}`;
                opt.dataset.nome = nomeFinal;
                opt.dataset.preco = valorNumerico.toString();
                elProd.appendChild(opt);
            });
        }
    } catch (e) { mostrarNotificacao("Erro ao carregar listas.", "erro"); }
}

function inicializarLogicaCarrinho() {
    const btnAdd = document.getElementById('btn-adicionar-produto');
    const btnFin = document.getElementById('btn-finalizar-venda');
    const produtoSelect = document.getElementById('produto-select');
    const quantidadeInput = document.getElementById('produto-quantidade');
    
    if(btnAdd && produtoSelect) {
        btnAdd.addEventListener('click', () => {
            const selectedOption = produtoSelect.options[produtoSelect.selectedIndex];
            if (!selectedOption.value) {
                mostrarNotificacao('Por favor, selecione um produto.', 'erro');
                return;
            }
            const id = parseInt(selectedOption.value);
            const nome = selectedOption.dataset.nome || "Produto";
            let preco = parseFloat(selectedOption.dataset.preco);
            if (isNaN(preco)) preco = 0.0;
            const quantidade = parseInt(quantidadeInput.value) || 1;

            carrinho.push({ idProduto: id, nome: nome, preco: preco, quantidade: quantidade, subtotal: preco * quantidade });
            atualizarCarrinhoDisplay();
            produtoSelect.selectedIndex = 0;
            quantidadeInput.value = 1;
        });
    }

    if(btnFin) {
        btnFin.addEventListener('click', async () => {
            if(carrinho.length === 0) { mostrarNotificacao("Carrinho vazio!", "erro"); return; }
            const cliSelect = document.getElementById('venda2ClienteSelect');
            if (!cliSelect || !cliSelect.value) { mostrarNotificacao("Selecione um cliente!", "erro"); return; }
            
            btnFin.disabled = true;
            btnFin.textContent = "Processando...";
            let sucessos = 0, erros = 0;

            for(let item of carrinho) {
                const dto = {
                    idCliente: parseInt(cliSelect.value),
                    idProduto: parseInt(item.idProduto),
                    quantidade: item.quantidade,
                    dataInicio: new Date().toISOString().split('T')[0],
                    horaInicio: new Date().toLocaleTimeString('pt-BR', {hour:'2-digit', minute:'2-digit'})
                };
                try {
                    const res = await fetch('/vendas/api/venda/salvar', { method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto) });
                    if(res.ok) sucessos++; else erros++;
                } catch(e) { erros++; }
            }
            btnFin.disabled = false;
            btnFin.textContent = "Finalizar Venda";

            if(erros === 0 && sucessos > 0) {
                mostrarNotificacao("Venda realizada!", "sucesso");
                carrinho = [];
                atualizarCarrinhoDisplay();
                closeModal('venda2');
                atualizarTabelaVendas();
                carregarDashboard();
            } else { mostrarNotificacao("Erro ao salvar venda.", "erro"); }
        });
    }
}

function atualizarCarrinhoDisplay() {
    const listaCarrinho = document.getElementById('carrinho-itens-lista');
    const totalCarrinhoSpan = document.getElementById('valor-total-carrinho');
    if(!listaCarrinho) return;
    listaCarrinho.innerHTML = '';
    if (carrinho.length === 0) {
        listaCarrinho.innerHTML = '<p class="carrinho-vazio">Nenhum item adicionado ainda.</p>';
        if(totalCarrinhoSpan) totalCarrinhoSpan.textContent = '0,00';
        return;
    }
    let valorTotal = 0;
    carrinho.forEach((item, index) => {
        valorTotal += item.subtotal;
        const itemDiv = document.createElement('div');
        itemDiv.className = 'carrinho-item';
        itemDiv.style.cssText = "display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding: 5px 0;";
        itemDiv.innerHTML = `<span>${item.quantidade}x ${item.nome}</span><span>R$ ${item.subtotal.toFixed(2).replace('.', ',')}</span><button class="btn-remover-item" data-index="${index}" style="background:none; border:none; color:red; cursor:pointer;">&times;</button>`;
        listaCarrinho.appendChild(itemDiv);
    });
    if(totalCarrinhoSpan) totalCarrinhoSpan.textContent = valorTotal.toFixed(2).replace('.', ',');
    document.querySelectorAll('.btn-remover-item').forEach(button => {
        button.addEventListener('click', (e) => {
            carrinho.splice(parseInt(e.target.dataset.index), 1);
            atualizarCarrinhoDisplay();
        });
    });
}

async function atualizarTabelaVendas() {
    const tbody = document.getElementById('tbody-vendas');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;">Carregando vendas...</td></tr>';
    try {
        const response = await fetch('/vendas/api/venda/listar?size=20&sort=idVenda,desc');
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data;
            tbody.innerHTML = '';
            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;">Nenhuma venda encontrada.</td></tr>';
                return;
            }
            lista.forEach(venda => {
                const tr = document.createElement('tr');
                const valorFormatado = venda.valor ? venda.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : 'R$ 0,00';
                const lucroFormatado = venda.lucro ? venda.lucro.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : 'R$ 0,00';
                tr.innerHTML = `<td>${venda.idVenda}</td><td>${venda.dataInicio || ''}</td><td>${venda.horaInicio || ''}</td><td>${venda.nomeCliente || '-'}</td><td>${venda.nomeProduto || '-'}</td><td>${venda.quantidade}</td><td>${valorFormatado}</td><td>${lucroFormatado}</td><td><span class="status-badge status-success">Concluída</span></td><td style="display: flex; gap: 5px; justify-content: center;"><button class="table-btn edit" onclick="editarVenda(${venda.idVenda})">Editar</button><button class="table-btn delete" onclick="deletarVenda(${venda.idVenda})">Deletar</button></td>`;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-venda', atualizarTabelaProdutos);
        } else { tbody.innerHTML = '<tr><td colspan="10" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>'; }
    } catch (error) { tbody.innerHTML = '<tr><td colspan="10" style="color:red; text-align:center;">Erro de conexão.</td></tr>'; }
}

// =================================================================================
// 7. FUNÇÕES DE FUNCIONÁRIOS
// =================================================================================

async function atualizarTabelaFuncionarios() {
    const tbody = document.getElementById('tbody-funcionarios');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Carregando...</td></tr>';
    try {
        const response = await fetch('/funcionario/api/funcionario/listar?size=20&sort=idFun,desc');
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data;
            tbody.innerHTML = '';
            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Nenhum funcionário encontrado.</td></tr>';
                return;
            }
            lista.forEach(func => {
                const tr = document.createElement('tr');
                let statusClass = (func.statusFuncionario === 'ATIVO' || func.statusFuncionario === 'EFETIVO') ? 'status-success' : 'status-danger';
                tr.innerHTML = `<td>${func.idFuncionario}</td><td>${func.nomeFuncionario}</td><td>${func.cpf}</td><td>${func.dataAdm || '-'}</td><td>${func.nivelAces || '-'}</td><td><span class="status-badge ${statusClass}">${func.statusFuncionario || 'Ativo'}</span></td><td style="display: flex; gap: 5px;"><button class="table-btn edit" onclick="editarFuncionario(${func.idFuncionario})">Editar</button><button class="table-btn delete" onclick="deletarFuncionario(${func.idFuncionario})">Deletar</button></td>`;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-funcionarios', atualizarTabelaProdutos);
        } else { tbody.innerHTML = '<tr><td colspan="7" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>'; }
    } catch (error) { tbody.innerHTML = '<tr><td colspan="7" style="color:red; text-align:center;">Erro de conexão.</td></tr>'; }
}

async function editarFuncionario(id) {
    try {
        const response = await fetch(`/funcionario/api/funcionario/${id}`);
        if (!response.ok) throw new Error("Erro");
        const func = await response.json();
        let elId = document.getElementById('funcId');
        if (!elId) { elId = document.createElement('input'); elId.type = 'hidden'; elId.id = 'funcId'; document.querySelector('#modalFuncionario .modal-content').appendChild(elId); }
        elId.value = func.idFuncionario;
        document.getElementById('funcNome').value = func.nomeFuncionario;
        document.getElementById('funcCpf').value = func.cpf;
        document.getElementById('funcEmail').value = func.endEmail;
        document.getElementById('funcTel').value = func.telefone;
        document.getElementById('funcCep').value = func.cep || '';
        document.getElementById('funcRua').value = func.rua || '';
        document.getElementById('funcBairro').value = func.bairro || '';
        document.getElementById('funcNumero').value = func.numeroCasa || '';
        document.getElementById('funcCidade').value = func.cidade || '';
        openModal('Funcionario');
        const h2 = document.querySelector('#modalFuncionario h2');
        if(h2) h2.textContent = 'Editar Funcionário';
    } catch (e) { mostrarNotificacao("Erro ao carregar funcionário.", "erro"); }
}

async function salvarFuncionario() {
    const elId = document.getElementById('funcId');
    const id = elId ? elId.value : null;
    const nome = document.getElementById('funcNome').value;
    const cpf = document.getElementById('funcCpf').value;
    // ... (restante dos campos)
    const dto = {
        idFun: id ? parseInt(id) : null,
        nomeFuncionario: nome, 
        cpf: cpf,
        endEmail: document.getElementById('funcEmail').value,
        telefone: document.getElementById('funcTel').value,
        cep: document.getElementById('funcCep').value,
        rua: document.getElementById('funcRua').value,
        bairro: document.getElementById('funcBairro').value,
        numeroCasa: parseInt(document.getElementById('funcNumero').value || 0),
        cidade: document.getElementById('funcCidade').value,
        nivelAces: document.getElementById('funcAcesso') ? document.getElementById('funcAcesso').value : null,
        salario: parseFloat(document.getElementById('funcSalario') ? document.getElementById('funcSalario').value : 0)
    };
    try {
        const res = await fetch('/funcionario/api/funcionario/salvar', { method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(dto) });
        if (res.ok) {
            mostrarNotificacao('Funcionário salvo!', 'sucesso');
            closeModal('Funcionario');
            if(elId) elId.value = '';
            atualizarTabelaFuncionarios();
            carregarDashboard();
        } else { mostrarNotificacao('Erro: ' + await res.text(), 'erro'); }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function deletarFuncionario(id) {
    if (!confirm("Tem certeza que deseja excluir?")) return;
    try {
        const response = await fetch(`/funcionario/api/funcionario/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Funcionário excluído!", "sucesso");
            atualizarTabelaFuncionarios();
            carregarDashboard();
        } else { mostrarNotificacao(await response.text() || "Erro ao excluir.", "erro"); }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

// =================================================================================
// 9. LÓGICA DO DASHBOARD (FRONT-ONLY)
// =================================================================================

async function carregarDashboard() {
    if (!document.getElementById('dash-total-produtos')) return;

    const hoje = new Date();
    const mesAtual = hoje.getMonth();
    const anoAtual = hoje.getFullYear();

    try {
        const resProd = await fetch('/produto/api/produto/listar?size=1000');
        if (resProd.ok) {
            const data = await resProd.json();
            const produtos = data.content || data;
            document.getElementById('dash-total-produtos').innerText = data.totalElements || produtos.length;
            document.getElementById('dash-estoque-baixo').innerText = produtos.filter(p => p.quantidade < 5).length;
        }

        const resVendas = await fetch('/vendas/api/venda/listar?size=1000&sort=idVenda,desc');
        if (resVendas.ok) {
            const data = await resVendas.json();
            const vendas = data.content || data;
            const vendasMes = vendas.filter(v => {
                if(!v.dataInicio) return false;
                const partes = v.dataInicio.includes('-') ? v.dataInicio.split('-') : v.dataInicio.split('/').reverse();
                const d = new Date(partes[0], partes[1]-1, partes[2]);
                return d.getMonth() === mesAtual && d.getFullYear() === anoAtual;
            });
            document.getElementById('dash-total-vendas').innerText = vendasMes.length;
            const total = vendasMes.reduce((acc, v) => acc + (v.valor || 0), 0);
            document.getElementById('dash-lucro-vendas').innerText = total.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
        }

        const resOS = await fetch('/ordens/api/ordem/listar?size=1000&sort=idOS,desc');
        if (resOS.ok) {
            const data = await resOS.json();
            const ordens = data.content || data;
            const osMes = ordens.filter(os => {
                if(!os.dataInicio) return false;
                const partes = os.dataInicio.includes('-') ? os.dataInicio.split('-') : os.dataInicio.split('/').reverse();
                const d = new Date(partes[0], partes[1]-1, partes[2]);
                return d.getMonth() === mesAtual && d.getFullYear() === anoAtual;
            });
            document.getElementById('dash-total-os').innerText = osMes.length;
            const lucro = osMes.reduce((acc, os) => acc + (os.lucro || os.valor || 0), 0);
            document.getElementById('dash-lucro-os').innerText = lucro.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
        }

        const resFunc = await fetch('/funcionario/api/funcionario/listar?size=1000');
        if (resFunc.ok) {
            const data = await resFunc.json();
            const funcs = data.content || data;
            const ativos = funcs.filter(f => f.statusFuncionario === 'ATIVO' || f.statusFuncionario === 'EFETIVO').length;
            document.getElementById('dash-funcionarios-ativos').innerText = ativos;
            
            const niver = funcs.filter(f => {
                if(!f.dataNascimento) return false;
                const mes = parseInt(f.dataNascimento.split('-')[1]) - 1;
                return mes === mesAtual;
            }).map(f => f.nomeFuncionario);
            
            document.getElementById('dash-aniversariantes').innerText = niver.length > 0 ? niver.slice(0,2).join(', ') : "Nenhum";
        }
    } catch (e) { console.error("Erro dashboard", e); }
}