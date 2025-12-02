/**
 * ZTech Pro - Main Script
 * Autor: Tedesche / Versão Final Consolidada
 */

// =================================================================================
// 1. CONFIGURAÇÕES E HELPERS
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
            text: mensagem, duration: 3000, close: true, gravity: "top", position: "right",
            style: { background: corFundo, borderRadius: "8px" }
        }).showToast();
    } else { alert(mensagem); }
}

function popularSelect(selectElement, lista, placeholder, campoId = 'id') {
    if (!selectElement) return;
    selectElement.innerHTML = ''; 
    const ph = document.createElement('option');
    ph.value = ""; ph.textContent = placeholder; ph.disabled = true; ph.selected = true;
    selectElement.appendChild(ph);

    lista.forEach(item => {
        const option = document.createElement('option');
        option.value = item[campoId] || item.id; 
        option.textContent = item.nome || item.nomeCliente || "Item sem nome"; 
        selectElement.appendChild(option);
    });
}

// Nova função genérica para busca de CEP (usada por Cliente e Funcionário)
function buscarCep(prefixo) {
    const cepInput = document.getElementById(prefixo + 'Cep');
    if (!cepInput) return;

    let cep = cepInput.value.replace(/\D/g, '');

    if (cep.length !== 8) return; // Só busca se tiver 8 dígitos

    // Feedback visual
    document.getElementById(prefixo + 'Rua').placeholder = "Buscando...";

    fetch(`https://viacep.com.br/ws/${cep}/json/`)
        .then(response => response.json())
        .then(data => {
            if (data.erro) {
                mostrarNotificacao("CEP não encontrado!", "erro");
                document.getElementById(prefixo + 'Rua').value = "";
                document.getElementById(prefixo + 'Bairro').value = "";
                document.getElementById(prefixo + 'Cidade').value = "";
            } else {
                document.getElementById(prefixo + 'Rua').value = data.logradouro;
                document.getElementById(prefixo + 'Bairro').value = data.bairro;
                document.getElementById(prefixo + 'Cidade').value = data.localidade;
                // Foca no número
                const numInput = document.getElementById(prefixo + 'Numero');
                if(numInput) numInput.focus();
            }
        }).catch(error => {
            console.error(error);
            mostrarNotificacao("Erro ao buscar CEP.", "erro");
        }).finally(() => {
             document.getElementById(prefixo + 'Rua').placeholder = "Rua";
        });
}

// =================================================================================
// 2. INICIALIZAÇÃO
// =================================================================================

document.addEventListener("DOMContentLoaded", function() {
    // Carrega tabela se estiver na aba estoque
    const params = new URLSearchParams(window.location.search);
    if(params.get('tab') === 'estoque') atualizarTabelaProdutos();

    // Configura botões de modal
    const btnProduto = document.getElementById('btn_novoProduto');
    if (btnProduto) {
        btnProduto.addEventListener('click', async (e) => {
            e.preventDefault();
            await carregarDadosProdutoModal();
            openModal('produto');
        });
    }
    
    // Listener para forms secundários (se houver)
    const formNovaMarca = document.getElementById('formNovaMarca');
    if (formNovaMarca) {
        formNovaMarca.addEventListener('submit', async (e) => { e.preventDefault(); await salvarMarca(); });
    }
});

// =================================================================================
// 3. TABELA DE PRODUTOS
// =================================================================================

async function atualizarTabelaProdutos(pagina = 0) {
    const tbody = document.getElementById('tbody-produtos');
    const pagContainer = document.getElementById('paginacao-container');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Carregando...</td></tr>';

    try {
        const response = await fetch(`/produto/api/produto/listar?page=${pagina}`);
        if (!response.ok) throw new Error('Erro ao buscar produtos');
        
        const pageData = await response.json();
        const produtos = pageData.content;

        tbody.innerHTML = '';

        if (!produtos || produtos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Nenhum produto encontrado.</td></tr>';
            return;
        }

        produtos.forEach(prod => {
            const tr = document.createElement('tr');
            const preco = prod.valor ? `R$ ${prod.valor.toFixed(2)}` : 'R$ 0.00';
            const cat = prod.categoria || '-';
            const marca = prod.marca || '-';

            tr.innerHTML = `
                <td>${prod.idProduto}</td>
                <td>${prod.nome}</td>
                <td>${preco}</td>
                <td>${prod.quantidade}</td>
                <td>${prod.descricao || ''}</td>
                <td>${cat}</td>
                <td>${marca}</td>
                <td>
                    <button class="table-btn edit" onclick="editarProduto(${prod.idProduto})">✏️</button>
                    <button class="table-btn delete" onclick="deletarProduto(${prod.idProduto})">🗑️</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        if(pagContainer) {
            pagContainer.innerHTML = '';
            if(!pageData.first) pagContainer.innerHTML += `<button class="action-btn btn-secondary" onclick="atualizarTabelaProdutos(${pageData.number - 1})">Anterior</button>`;
            pagContainer.innerHTML += `<span style="margin:0 10px">Página ${pageData.number + 1} de ${pageData.totalPages}</span>`;
            if(!pageData.last) pagContainer.innerHTML += `<button class="action-btn btn-secondary" onclick="atualizarTabelaProdutos(${pageData.number + 1})">Próximo</button>`;
        }

    } catch (e) {
        console.error(e);
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:red;">Erro ao carregar tabela.</td></tr>';
    }
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
    
    const idCategoria = selectCategoria ? selectCategoria.value : null;
    const idMarca = selectMarca ? selectMarca.value : null;

    if (!nome || !idCategoria || !idMarca) {
        mostrarNotificacao("Preencha Nome, Categoria e Marca.", "erro");
        return;
    }

    const produtoDTO = {
        idProduto: id ? parseInt(id) : null,
        nome: nome,
        custo: custo ? parseFloat(custo) : 0.0,
        valor: valor ? parseFloat(valor) : 0.0,
        quantidade: qtd ? parseInt(qtd) : 0,
        descricao: descricao,
        idCategoria: parseInt(idCategoria),
        idMarca: parseInt(idMarca)
    };

    try {
        const response = await fetch('/produto/api/produto/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(produtoDTO)
        });

        if (response.ok) {
            mostrarNotificacao("Produto salvo!", "sucesso");
            closeModal('produto');
            atualizarTabelaProdutos(); 
            // Limpa form
            document.getElementById('prodId').value = '';
            document.getElementById('prodNome').value = '';
        } else {
            const erroMsg = await response.text();
            mostrarNotificacao("Erro: " + erroMsg, "erro");
        }
    } catch (error) { mostrarNotificacao("Erro ao conectar.", "erro"); }
}

async function deletarProduto(id) {
    if (!confirm("Excluir este produto?")) return;
    try {
        const response = await fetch(`/produto/api/deletar/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (response.ok) {
            mostrarNotificacao("Produto removido!", "sucesso");
            atualizarTabelaProdutos();
        } else {
            mostrarNotificacao("Erro ao remover.", "erro");
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
    } catch (e) { mostrarNotificacao("Erro ao carregar edição", "erro"); }
}

// =================================================================================
// 4. FUNÇÕES DE ORDEM DE SERVIÇO (O.S.)
// =================================================================================

async function carregarDadosOS() {
    const elCliente = document.getElementById('osCliente');
    const elServico = document.getElementById('osServico');
    const elProduto = document.getElementById('osProduto');

    if (!elCliente || elCliente.options.length > 1) return;

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
            popularSelect(elProduto, dataProd.content, "Selecione Produto", "idProduto");
        }
    } catch (e) { console.error("Erro loading OS", e); }
}

async function salvarOS() {
    const idCliente = document.getElementById('osCliente').value;
    const idServico = document.getElementById('osServico').value;
    const idProduto = document.getElementById('osProduto').value;
    const qtd = document.getElementById('osQuantidade').value;
    const preco = document.getElementById('osPreco').value;
    const status = document.getElementById('osStatus').value;

    if (!idCliente || !idServico || !idProduto) {
        mostrarNotificacao('Cliente, Serviço e Produto são obrigatórios!', 'erro');
        return;
    }

    const dto = {
        idCliente: parseInt(idCliente),
        idServico: parseInt(idServico),
        idProduto: parseInt(idProduto),
        quantidade: parseInt(qtd || 1),
        valor: parseFloat(preco || 0),
        statusOS: status,
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
            mostrarNotificacao('OS Salva!', 'sucesso');
            closeModal('OrdemServico');
            setTimeout(() => location.reload(), 1000);
        } else {
            const txt = await res.text();
            mostrarNotificacao('Erro: ' + txt, 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro de conexão', 'erro'); }
}

// =================================================================================
// 5. FUNÇÕES DE CLIENTE, FUNCIONÁRIO E SERVIÇO
// =================================================================================

async function salvarCliente() {
    // Usando IDs exclusivos 'cli...'
    const nome = document.getElementById('cliNome').value;
    const cpf = document.getElementById('cliCPF').value;
    const email = document.getElementById('cliEmail').value;
    const telefone = document.getElementById('cliTel').value;
    
    // IDs de endereço exclusivos
    const cep = document.getElementById('cliCep').value;
    const rua = document.getElementById('cliRua').value;
    const bairro = document.getElementById('cliBairro').value;
    const numero = document.getElementById('cliNumero').value;
    const cidade = document.getElementById('cliCidade').value;

    if (!nome || !cpf) { mostrarNotificacao('Nome e CPF obrigatórios!', 'erro'); return; }

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
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('Cliente salvo!', 'sucesso');
            closeModal('Cliente');
            // Limpa form
            document.querySelectorAll('#modalCliente input').forEach(i => i.value = '');
        } else {
            mostrarNotificacao('Erro: ' + await res.text(), 'erro');
        }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

async function salvarFuncionario() {
    // Usando IDs exclusivos 'func...'
    const nome = document.getElementById('funcNome').value;
    const cpf = document.getElementById('funcCpf').value;
    const acesso = document.getElementById('funcAcesso').value;
    const salario = document.getElementById('funcSalario').value;
    const dataNasc = document.getElementById('funcDataNasc').value;
    
    // IDs de endereço exclusivos
    const cep = document.getElementById('funcCep').value;
    const rua = document.getElementById('funcRua').value;
    const bairro = document.getElementById('funcBairro').value;
    const numero = document.getElementById('funcNumero').value;
    const cidade = document.getElementById('funcCidade').value;

    if (!nome || !cpf) { mostrarNotificacao('Nome e CPF obrigatórios!', 'erro'); return; }

    const dto = {
        nome: nome,
        cpf: cpf,
        dataNascimento: dataNasc,
        nivelAcesso: acesso,
        salario: parseFloat(salario || 0),
        // Se o backend esperar endereço plano ou aninhado, ajuste aqui. Assumindo plano:
        cep: cep,
        rua: rua,
        bairro: bairro,
        numeroCasa: parseInt(numero || 0),
        cidade: cidade
    };

    try {
        const res = await fetch('/api/funcionarios/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(dto)
        });
        if (res.ok) {
            mostrarNotificacao('Funcionário salvo!', 'sucesso');
            closeModal('Funcionario');
            document.querySelectorAll('#modalFuncionario input').forEach(i => i.value = '');
        } else {
            mostrarNotificacao('Erro ao salvar.', 'erro');
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
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(dto)
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
    if (!nome) { mostrarNotificacao('Nome obrigatório!', 'erro'); return; }

    try {
        const res = await fetch('/api/marcas/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ nome: nome })
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
    if (!nome) { mostrarNotificacao('Nome obrigatório!', 'erro'); return; }

    try {
        const res = await fetch('/api/categorias/cadastrar', { 
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ nome: nome })
        });
        if (res.ok) {
            mostrarNotificacao('Categoria salva!', 'sucesso');
            input.value = '';
            closeModal('Categoria');
        } else { mostrarNotificacao('Erro ao salvar.', 'erro'); }
    } catch (e) { mostrarNotificacao('Erro conexão', 'erro'); }
}

// =================================================================================
// 6. UTILITÁRIOS DE MODAL
// =================================================================================

function openModal(type) {
    const id = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    const modal = document.getElementById(id);
    if (modal) {
        modal.style.display = 'flex';
        if (type === 'OrdemServico' || type === 'os') carregarDadosOS();
    }
}

function closeModal(type) {
    const id = 'modal' + type.charAt(0).toUpperCase() + type.slice(1);
    const modal = document.getElementById(id);
    if(modal) modal.style.display = 'none';
}

async function carregarDadosProdutoModal() {
    const elCat = document.getElementById('produtoCategoriaSelect');
    const elMarca = document.getElementById('produtoMarcaSelect');
    if(!elCat) return;
    
    try {
        const [rCat, rMar] = await Promise.all([ fetch('/api/categorias'), fetch('/api/marcas') ]);
        if(rCat.ok) popularSelect(elCat, await rCat.json(), "Categoria", "idCategoria");
        if(rMar.ok) popularSelect(elMarca, await rMar.json(), "Marca", "idMarca");
    } catch(e) { console.log(e); }
}