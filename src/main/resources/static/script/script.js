/**
 * ZTech Pro - Main Script
 * Autor: Tedesche / Versão Final Consolidada (Atualizada com AJAX)
 */

// =================================================================================
// 1. CONFIGURAÇÕES E UTILITÁRIOS (HELPERS)
// =================================================================================

/**
 * Obtém os headers para segurança (CSRF) do Spring Security.
 */
function getAuthHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    
    if (!tokenMeta || !headerMeta) return { 'Content-Type': 'application/json' };
    
    return {
        'Content-Type': 'application/json',
        [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
    };
}

/**
 * Exibe notificações na tela.
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
    
    const ph = document.createElement('option');
    ph.value = ""; 
    ph.textContent = placeholder; 
    ph.disabled = true; 
    ph.selected = true;
    selectElement.appendChild(ph);

    if (!lista || lista.length === 0) return;

    lista.forEach(item => {
        const option = document.createElement('option');
        
        // Tenta identificar o ID correto
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

/**
 * Método ÚNICO para gerar controles de paginação.
 * @param {Object} data - O objeto Page retornado pelo Spring Boot (contém content, totalPages, number, etc.)
 * @param {String} idContainer - O ID da <div> onde os botões vão aparecer (ex: 'paginacao-produtos')
 * @param {Function} funcaoCallback - A função que deve ser chamada ao clicar (ex: atualizarTabelaProdutos)
 */
function gerarPaginacao(data, idContainer, funcaoCallback) {
    const container = document.getElementById(idContainer);
    if (!container) return;

    container.innerHTML = ''; // Limpa botões antigos

    // Se não tiver páginas ou for apenas 1, não mostra nada (ou apenas o texto, se preferir)
    if (data.totalPages <= 1) return;

    // 1. Botão ANTERIOR
    const btnPrev = document.createElement('button');
    btnPrev.innerHTML = '&larr; Anterior'; // Seta esquerda
    btnPrev.className = 'action-btn btn-secondary';
    btnPrev.disabled = data.first; // Desabilita se for a 1ª página
    
    if (!data.first) {
        btnPrev.onclick = function() {
            funcaoCallback(data.number - 1); // Chama a função passando a página anterior
        };
    }
    container.appendChild(btnPrev);

    // 2. Texto Informativo (Ex: Página 1 de 5)
    const spanInfo = document.createElement('span');
    spanInfo.style.cssText = 'font-weight: bold; margin: 0 15px;';
    spanInfo.innerText = `Página ${data.number + 1} de ${data.totalPages}`;
    container.appendChild(spanInfo);

    // 3. Botão PRÓXIMO
    const btnNext = document.createElement('button');
    btnNext.innerHTML = 'Próximo &rarr;'; // Seta direita
    btnNext.className = 'action-btn btn-primary';
    btnNext.disabled = data.last; // Desabilita se for a última página
    
    if (!data.last) {
        btnNext.onclick = function() {
            funcaoCallback(data.number + 1); // Chama a função passando a próxima página
        };
    }
    container.appendChild(btnNext);
}

// =================================================================================
// 2. INICIALIZAÇÃO DA PÁGINA
// =================================================================================

document.addEventListener("DOMContentLoaded", function() {
    
    // Verifica aba ativa
    const params = new URLSearchParams(window.location.search);
    const abaAtiva = params.get('tab');
    
    if (abaAtiva && typeof showSection === 'function') {
         setTimeout(() => {
             const btn = document.querySelector(`.nav-btn[onclick*="${abaAtiva}"]`);
             if(btn) btn.click();
         }, 100);
    } else {
        // Carrega tabelas iniciais se necessário
        const sectionEstoque = document.getElementById('estoque-section');
        if(sectionEstoque && sectionEstoque.classList.contains('active')) atualizarTabelaProdutos();
    }

    // Inicializa Carrinho
    inicializarLogicaCarrinho();

    // Botões estáticos
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

        // Lazy Load dos dados
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
        
        const mapIds = {
            'venda': 'vendaId',
            'os': 'osId',
            'OrdemServico': 'osId',
            'produto': 'prodId'
        };
        
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
// 4. FUNÇÕES DE PRODUTO E ESTOQUE
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
    } catch (e) { mostrarNotificacao("Erro ao carregar produto.", "erro"); }
}

async function atualizarTabelaProdutos() {
    const tbody = document.getElementById('tbody-produtos');
    if (!tbody) return;

    // 1. Capturar valores dos filtros
    const termo = document.getElementById('buscaProdutoInput') ? document.getElementById('buscaProdutoInput').value : '';
    const idCat = document.getElementById('filtroCategoria') ? document.getElementById('filtroCategoria').value : '';
    const idMarca = document.getElementById('filtroMarca') ? document.getElementById('filtroMarca').value : '';

    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Carregando estoque...</td></tr>';

    try {
        // 2. Montar URL com parâmetros
        // URLSearchParams facilita a criação da query string (ex: ?termo=abc&idCategoria=1)
        const params = new URLSearchParams({
            size: 20,
            sort: 'idProduto,desc'
        });
        
        if (termo) params.append('termo', termo);
        if (idCat) params.append('idCategoria', idCat);
        if (idMarca) params.append('idMarca', idMarca);

        const response = await fetch(`/produto/api/produto/listar?${params.toString()}`);
        
        if (response.ok) {
            const data = await response.json();
            const lista = data.content || data;

            tbody.innerHTML = '';
            
            if (!lista || lista.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Nenhum produto encontrado com esses filtros.</td></tr>';
                return;
            }

            // ... (restante do código de renderização das linhas igual ao anterior) ...
            lista.forEach(p => { 
                // ... monta tr ...
                const tr = document.createElement('tr');
                const valorFormatado = p.valor ? p.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : 'R$ 0,00';
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
			gerarPaginacao(data, 'paginacao-produtos', atualizarTabelaProdutos);
        } else {
             tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        console.error(error);
        tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center;">Erro de conexão.</td></tr>';
    }
}

// Bônus: Fazer o ENTER no campo de busca funcionar
document.getElementById('buscaProdutoInput')?.addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        atualizarTabelaProdutos();
    }
});

// =================================================================================
// 5. FUNÇÕES DE ORDEM DE SERVIÇO (O.S.)
// =================================================================================

async function carregarDadosOS() {
    const elCliente = document.getElementById('osCliente');
    const elServico = document.getElementById('osServico');
    const elProduto = document.getElementById('osProduto');

    if (!elCliente) return;
    if (elCliente.options.length > 1) return; 

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
        // Oculto no modal, deve existir ou ser criado
        if(!elId) {
             const hiddenInput = document.createElement('input');
             hiddenInput.type = 'hidden';
             hiddenInput.id = 'osId';
             document.querySelector('#modalOrdemServico .modal-content').appendChild(hiddenInput);
        }
        document.getElementById('osId').value = os.idOS || os.IdOS;

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
    let elId = document.getElementById('osId');
    // Cria se não existir
    if(!elId) {
         elId = document.createElement('input');
         elId.type = 'hidden';
         elId.id = 'osId';
         document.querySelector('#modalOrdemServico .modal-content').appendChild(elId);
    }
    
    const idOS = elId.value;
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
            atualizarTabelaOrdens(); // Atualiza sem reload
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
            atualizarTabelaOrdens(); // Atualiza sem reload
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

async function atualizarTabelaOrdens() {
    const tbody = document.getElementById('tbody-ordens'); // Certifique-se de ter id="tbody-ordens" na sua table
    if (!tbody) {
        // Fallback se o ID não existir no HTML (tente achar pelo contexto ou avise)
        console.warn('ID tbody-ordens não encontrado. Adicione id="tbody-ordens" na tabela de OS.');
        return;
    }

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
                
                // Badge de Status
                let badgeClass = 'status-normal';
                if(os.statusOS === 'CONCLUIDA') badgeClass = 'status-success';
                if(os.statusOS === 'CANCELADA') badgeClass = 'status-danger';

                tr.innerHTML = `
                    <td>${os.idOS}</td>
                    <td>${os.nomeCliente || '-'}</td>
                    <td>${os.dataInicio || ''}</td>
                    <td>${os.dataFim || '-'}</td>
                    <td>${valorFormatado}</td>
                    <td>${os.nomeServico || '-'}</td>
                    <td>${os.nomeProduto || '-'}</td>
                    <td><span class="status-badge ${badgeClass}">${os.statusOS || 'Registrada'}</span></td>
                    <td style="display: flex; gap: 5px; justify-content: center;">
						<button class="table-btn edit" onclick="editarOS(${os.idOS})">Editar</button>
						<button class="table-btn delete" onclick="deletarOS(${os.idOS})">Deletar</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-ordens', atualizarTabelaProdutos);
        } else {
            tbody.innerHTML = '<tr><td colspan="9" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        console.error(error);
        tbody.innerHTML = '<tr><td colspan="9" style="color:red; text-align:center;">Erro de conexão.</td></tr>';
    }
}

// =================================================================================
// 6. FUNÇÕES DE VENDA (SIMPLES E CARRINHO/PDV)
// =================================================================================

// --- 6.1 Venda Simples (Um item por vez) ---
async function carregarDadosVendaModal() {
    const elCliente = document.getElementById('vendaCliente');
    const elProduto = document.getElementById('vendaProduto');
    // Verifica se os elementos existem (para modal de venda simples, se houver)
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

        // Cria input hidden se não existir
        let elId = document.getElementById('vendaId');
        if(!elId) {
             // Tenta achar um form dentro do modal de venda simples
             const modalVenda = document.getElementById('modalVenda');
             if(modalVenda) {
                 elId = document.createElement('input');
                 elId.type = 'hidden';
                 elId.id = 'vendaId';
                 modalVenda.querySelector('.modal-content').appendChild(elId);
             }
        }
        if(elId) elId.value = venda.idVenda;

        if(document.getElementById('vendaCliente')) document.getElementById('vendaCliente').value = venda.idCliente;
        if(document.getElementById('vendaProduto')) document.getElementById('vendaProduto').value = venda.idProduto;
        if(document.getElementById('vendaQuantidade')) document.getElementById('vendaQuantidade').value = venda.quantidade;

        openModal('venda');
    } catch (e) {
        mostrarNotificacao("Erro ao carregar venda", "erro");
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
            atualizarTabelaVendas(); // Atualiza sem reload
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
            atualizarTabelaVendas(); // Atualiza sem reload
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}


// --- 6.2 Venda PDV (Carrinho de Compras) ---

let carrinho = [];

async function carregarDadosVenda2() {
    const elProd = document.getElementById('produto-select'); 
    const elCli = document.getElementById('cliente-nome'); // Note: no HTML parece ser input text, vamos checar
    
    // Se no HTML "cliente-nome" for Input Text, o usuário digita. 
    // Se for Select (como sugerido para funcionar com ID), precisamos alterar o HTML ou usar datalist.
    // O código anterior usava popularSelect em 'venda2ClienteSelect', mas no HTML estava 'cliente-nome'.
    // Vou assumir a lógica do script anterior: popular o select de produtos.
    
    if(!elProd) return;

    try {
        const resProd = await fetch('/produto/api/produto/listar?size=1000');
        
        if(resProd.ok) {
            const data = await resProd.json();
            const lista = data.content || data;
            
            elProd.innerHTML = '<option value="">Selecione um produto</option>';
            lista.forEach(p => {
                const opt = document.createElement('option');
                // Value customizado: nome|preco para o front, mas precisamos do ID para salvar
                // Vamos usar: id|nome|preco
                opt.value = `${p.idProduto}|${p.nome}|${p.valor}`;
                opt.textContent = `${p.nome} - R$ ${p.valor ? p.valor.toFixed(2) : '0.00'}`;
                elProd.appendChild(opt);
            });
        }
    } catch(e) { console.error(e); }
}

function inicializarLogicaCarrinho() {
    const btnAdd = document.getElementById('btn-adicionar-produto');
    const btnFin = document.getElementById('btn-finalizar-venda');
    const produtoSelect = document.getElementById('produto-select');
    const quantidadeInput = document.getElementById('produto-quantidade');
    
    if(btnAdd && produtoSelect) {
        btnAdd.addEventListener('click', () => {
            const produtoOption = produtoSelect.options[produtoSelect.selectedIndex];
            if (!produtoOption.value) {
                mostrarNotificacao('Por favor, selecione um produto.', 'erro');
                return;
            }

            // Agora esperamos 3 partes: id|nome|preco
            const parts = produtoOption.value.split('|');
            let id, nome, preco;
            
            if(parts.length === 3) {
                [id, nome, preco] = parts;
            } else {
                // Fallback para o formato antigo (nome|preco) se não tiver ID
                [nome, preco] = parts;
                id = null; // Vai dar erro no backend se não tiver ID
            }

            const quantidade = parseInt(quantidadeInput.value);
            const precoNumerico = parseFloat(preco);

            carrinho.push({
                idProduto: id,
                nome: nome,
                preco: precoNumerico,
                quantidade: quantidade,
                subtotal: precoNumerico * quantidade
            });
            
            atualizarCarrinhoDisplay();
            
            produtoSelect.selectedIndex = 0;
            quantidadeInput.value = 1;
        });
    }

    if(btnFin) {
        btnFin.addEventListener('click', async () => {
            if(carrinho.length === 0) { mostrarNotificacao("Carrinho vazio!", "erro"); return; }
            
            // Tenta pegar cliente pelo nome ou ID.
            // Nota: O backend precisa do ID do cliente. Se 'cliente-nome' for texto, 
            // precisaria buscar o ID. Para simplificar, vamos assumir que existe um cliente padrão ou que o campo foi alterado para select.
            // Para este script funcionar 100%, o campo de cliente no HTML deveria ser um <select> populado com IDs.
            // Vou usar um ID fixo ou tentar ler o value se for select.
            
            const cliInput = document.getElementById('cliente-nome');
            let cliId = 1; // ID Default para teste se não for select
            if(cliInput && cliInput.tagName === 'SELECT') cliId = cliInput.value;
            
            // Loop para salvar itens
            let sucessos = 0;
            let erros = 0;

            btnFin.disabled = true;
            btnFin.textContent = "Processando...";

            for(let item of carrinho) {
                if(!item.idProduto) {
                    // Pula se não tiver ID (produtos hardcoded antigos)
                    continue; 
                }

                const dto = {
                    idCliente: parseInt(cliId), // Ajuste conforme seu HTML
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
            btnFin.textContent = "Finalizar Venda";

            if(erros === 0 && sucessos > 0) {
                mostrarNotificacao("Venda realizada com sucesso!", "sucesso");
                carrinho = [];
                atualizarCarrinhoDisplay();
                closeModal('venda2');
                atualizarTabelaVendas(); // Atualiza tabela
            } else if (sucessos > 0) {
                 mostrarNotificacao(`Parcial: ${sucessos} itens salvos, ${erros} erros.`, "erro");
                 atualizarTabelaVendas();
            } else {
                mostrarNotificacao("Erro ao salvar venda. Verifique os dados.", "erro");
            }
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
        
        itemDiv.innerHTML = `
            <span>${item.quantidade}x ${item.nome}</span>
            <span>R$ ${item.subtotal.toFixed(2).replace('.', ',')}</span>
            <button class="btn-remover-item" data-index="${index}" style="background:none; border:none; color:red; cursor:pointer;">&times;</button>`;
        listaCarrinho.appendChild(itemDiv);
    });

    if(totalCarrinhoSpan) totalCarrinhoSpan.textContent = valorTotal.toFixed(2).replace('.', ',');

    document.querySelectorAll('.btn-remover-item').forEach(button => {
        button.addEventListener('click', (e) => {
            const indexParaRemover = parseInt(e.target.dataset.index);
            carrinho.splice(indexParaRemover, 1);
            atualizarCarrinhoDisplay();
        });
    });
}

async function atualizarTabelaVendas() {
    const tbody = document.getElementById('tbody-vendas'); // Adicione id="tbody-vendas" no seu HTML
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

                tr.innerHTML = `
                    <td>${venda.idVenda}</td>
                    <td>${venda.dataInicio || ''}</td>
                    <td>${venda.horaInicio || ''}</td>
                    <td>${venda.nomeCliente || '-'}</td>
                    <td>${venda.nomeProduto || '-'}</td>
                    <td>${venda.quantidade}</td>
                    <td>${valorFormatado}</td>
                    <td>${lucroFormatado}</td>
                    <td><span class="status-badge status-success">Concluída</span></td>
                    <td style="display: flex; gap: 5px; justify-content: center;">
						<button class="table-btn edit" onclick="editarVenda(${venda.idVenda})">Editar</button>
						<button class="table-btn delete" onclick="deletarVenda(${venda.idVenda})">Deletar</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-venda', atualizarTabelaProdutos);
        } else {
            tbody.innerHTML = '<tr><td colspan="10" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        console.error(error);
        tbody.innerHTML = '<tr><td colspan="10" style="color:red; text-align:center;">Erro de conexão.</td></tr>';
    }
}

// =================================================================================
// 7. FUNÇÕES DE CLIENTE, FUNCIONÁRIO E OUTROS CADASTROS
// =================================================================================

async function salvarCliente() {
    const nome = document.getElementById('cliNome').value;
    const cpf = document.getElementById('cliCPF').value;
    const email = document.getElementById('cliEmail').value;
    const telefone = document.getElementById('cliTel').value;
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

// --- CARREGAR DADOS NO MODAL PARA EDIÇÃO ---
async function editarOS(id) {
    await carregarDadosOS(); // Garante que as listas (clientes, serviços) estão carregadas
    try {
        // 1. Busca os dados da O.S. pelo ID
        const response = await fetch(`/ordens/api/ordem/${id}`);
        if (!response.ok) throw new Error("Erro na API");
        const os = await response.json();

        // 2. Garante que o campo oculto de ID existe
        const elId = document.getElementById('osId');
        if(!elId) {
             const hiddenInput = document.createElement('input');
             hiddenInput.type = 'hidden';
             hiddenInput.id = 'osId';
             document.querySelector('#modalOrdemServico .modal-content').appendChild(hiddenInput);
        }
        document.getElementById('osId').value = os.idOS || os.IdOS;

        // 3. Preenche os campos do formulário com os dados recebidos
        if(document.getElementById('osCliente')) document.getElementById('osCliente').value = os.idCliente;
        if(document.getElementById('osServico')) document.getElementById('osServico').value = os.idServico;
        if(document.getElementById('osProduto')) document.getElementById('osProduto').value = os.idProduto;
        if(document.getElementById('osQuantidade')) document.getElementById('osQuantidade').value = os.quantidade;
        if(document.getElementById('osStatus')) document.getElementById('osStatus').value = os.statusOS;
        if(document.getElementById('osPreco')) document.getElementById('osPreco').value = os.valor;

        // 4. Abre o modal
        openModal('OrdemServico');
    } catch (e) {
        console.error(e);
        mostrarNotificacao("Erro ao carregar OS para edição.", "erro");
    }
}

// --- DELETAR O.S. ---
async function deletarOS(id) {
    if (!confirm("Deseja deletar esta O.S.?")) return;
    try {
        const response = await fetch(`/ordens/api/ordem/deletar/${id}`, { 
            method: 'DELETE', 
            headers: getAuthHeaders() 
        });
        if (response.ok) {
            mostrarNotificacao("Removido com sucesso!", "sucesso");
            // Atualiza a tabela visualmente sem recarregar a página
            atualizarTabelaOrdens(); 
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

// --- CARREGAR DADOS NO MODAL PARA EDIÇÃO ---
async function editarVenda(id) {
    await carregarDadosVendaModal(); // Carrega lista de clientes/produtos
    try {
        const response = await fetch(`/vendas/api/venda/${id}`);
        const venda = await response.json();

        // Cria ou recupera o input hidden para o ID da venda
        let elId = document.getElementById('vendaId');
        if(!elId) {
             const modalVenda = document.getElementById('modalVenda');
             if(modalVenda) {
                 elId = document.createElement('input');
                 elId.type = 'hidden';
                 elId.id = 'vendaId';
                 modalVenda.querySelector('.modal-content').appendChild(elId);
             }
        }
        if(elId) elId.value = venda.idVenda;

        // Preenche o formulário
        if(document.getElementById('vendaCliente')) document.getElementById('vendaCliente').value = venda.idCliente;
        if(document.getElementById('vendaProduto')) document.getElementById('vendaProduto').value = venda.idProduto;
        if(document.getElementById('vendaQuantidade')) document.getElementById('vendaQuantidade').value = venda.quantidade;

        openModal('venda');
    } catch (e) {
        mostrarNotificacao("Erro ao carregar venda", "erro");
    }
}

// --- DELETAR VENDA ---
async function deletarVenda(id) {
    if (!confirm("Deletar venda?")) return;
    try {
        const response = await fetch(`/vendas/api/venda/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Venda removida!", "sucesso");
            // Atualiza a tabela visualmente
            atualizarTabelaVendas(); 
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
        }
    } catch (error) { mostrarNotificacao("Erro de conexão.", "erro"); }
}

// =================================================================================
// 8. FUNÇÕES DE FUNCIONÁRIOS
// =================================================================================

/**
 * Atualiza a tabela de Funcionários dinamicamente
 */
async function atualizarTabelaFuncionarios() {
    const tbody = document.getElementById('tbody-funcionarios');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Carregando funcionários...</td></tr>';

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
                
                // Define a cor do badge baseado no status (Exemplo)
                let statusClass = 'status-normal';
                if (func.statusFuncionario === 'ATIVO' || func.statusFuncionario === 'EFETIVO') statusClass = 'status-success';
                if (func.statusFuncionario === 'DEMITIDO' || func.statusFuncionario === 'INATIVO') statusClass = 'status-danger';

                tr.innerHTML = `
                    <td>${func.idFuncionario}</td>
                    <td>${func.nomeFuncionario}</td>
                    <td>${func.cpf}</td>
                    <td>${func.dataAdm || '-'}</td>
                    <td>${func.nivelAces || '-'}</td>
                    <td><span class="status-badge ${statusClass}">${func.statusFuncionario || 'Ativo'}</span></td>
                    <td style="display: flex; gap: 5px;">
                        <button class="table-btn edit" onclick="editarFuncionario(${func.idFuncionario})">Editar</button>
                        <button class="table-btn delete" onclick="deletarFuncionario(${func.idFuncionario})">Deletar</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
			gerarPaginacao(data, 'paginacao-funcionarios', atualizarTabelaProdutos);
        } else {
            tbody.innerHTML = '<tr><td colspan="7" style="color:red; text-align:center;">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        console.error(error);
        tbody.innerHTML = '<tr><td colspan="7" style="color:red; text-align:center;">Erro de conexão.</td></tr>';
    }
}

/**
 * Carrega dados para edição no Modal
 */
async function editarFuncionario(id) {
    try {
        const response = await fetch(`/funcionario/api/funcionario/${id}`);
        if (!response.ok) throw new Error("Erro ao buscar funcionário");
        
        const func = await response.json();

        // Garante que existe um campo hidden para o ID
        let elId = document.getElementById('funcId');
        if (!elId) {
            elId = document.createElement('input');
            elId.type = 'hidden';
            elId.id = 'funcId';
            document.querySelector('#modalFuncionario .modal-content').appendChild(elId);
        }
        elId.value = func.idFuncionario;

        // Preenche os campos
        document.getElementById('funcNome').value = func.nomeFuncionario;
        document.getElementById('funcCpf').value = func.cpf;
        document.getElementById('funcEmail').value = func.endEmail;
        document.getElementById('funcTel').value = func.telefone;
        
        // Endereço
        document.getElementById('funcCep').value = func.cep || '';
        document.getElementById('funcRua').value = func.rua || '';
        document.getElementById('funcBairro').value = func.bairro || '';
        document.getElementById('funcNumero').value = func.numeroCasa || '';
        document.getElementById('funcCidade').value = func.cidade || '';
        
        // Se houver campos de data/nível no modal, preencha aqui
        // document.getElementById('funcAcesso').value = func.nivelAces;

        openModal('Funcionario');
    } catch (e) {
        mostrarNotificacao("Erro ao carregar funcionário.", "erro");
    }
}

/**
 * Salva (Cria ou Edita) Funcionário
 */
async function salvarFuncionario() {
    const elId = document.getElementById('funcId');
    const id = elId ? elId.value : null;

    const nome = document.getElementById('funcNome').value;
    const cpf = document.getElementById('funcCpf').value;
    const email = document.getElementById('funcEmail').value;
    const telefone = document.getElementById('funcTel').value;
    
    // Endereço
    const cep = document.getElementById('funcCep').value;
    const rua = document.getElementById('funcRua').value;
    const bairro = document.getElementById('funcBairro').value;
    const numero = document.getElementById('funcNumero').value;
    const cidade = document.getElementById('funcCidade').value;
    
    // Outros dados (Acesso, Salário)
    const acesso = document.getElementById('funcAcesso') ? document.getElementById('funcAcesso').value : null;
    const salario = document.getElementById('funcSalario') ? document.getElementById('funcSalario').value : 0;

    if (!nome || !cpf) { 
        mostrarNotificacao('Nome e CPF são obrigatórios!', 'erro'); 
        return; 
    }

    const dto = {
        idFun: id ? parseInt(id) : null, // Backend espera idFun ou idFuncionario? O controller verifica idFun
        nomeFuncionario: nome, 
        cpf: cpf,
        endEmail: email,
        telefone: telefone,
        cep: cep,
        rua: rua,
        bairro: bairro,
        numeroCasa: parseInt(numero || 0),
        cidade: cidade,
        nivelAces: acesso,
        // Adicione outros campos conforme seu DTO
    };

    try {
        const res = await fetch('/funcionario/api/funcionario/salvar', {
            method: 'POST', 
            headers: getAuthHeaders(), 
            body: JSON.stringify(dto)
        });

        if (res.ok) {
            mostrarNotificacao('Funcionário salvo com sucesso!', 'sucesso');
            closeModal('Funcionario');
            
            // Limpa o ID oculto para evitar edições acidentais depois
            if(elId) elId.value = '';
            
            atualizarTabelaFuncionarios(); // Atualiza a tabela sem reload
        } else {
            const erro = await res.text();
            mostrarNotificacao('Erro: ' + erro, 'erro');
        }
    } catch (e) { 
        console.error(e);
        mostrarNotificacao('Erro de conexão ao salvar.', 'erro'); 
    }
}

/**
 * Deleta Funcionário
 */
async function deletarFuncionario(id) {
    if (!confirm("Tem certeza que deseja excluir este funcionário?")) return;
    
    try {
        const response = await fetch(`/funcionario/api/funcionario/deletar/${id}`, { 
            method: 'DELETE', 
            headers: getAuthHeaders() 
        });
        
        if (response.ok) {
            mostrarNotificacao("Funcionário excluído!", "sucesso");
            atualizarTabelaFuncionarios();
        } else {
            const err = await response.text(); // Tenta pegar mensagem de erro do backend (vínculos)
            mostrarNotificacao(err || "Erro ao excluir.", "erro");
        }
    } catch (error) { 
        mostrarNotificacao("Erro de conexão.", "erro"); 
    }
}




