/**
 * ZTech Pro - Main Script
 * Autor: Tedesche / Refatorado
 */

// =================================================================================
// 1. CONFIGURAÇÕES E HELPERS (Globais)
// =================================================================================

// Função auxiliar para pegar os headers de autenticação (CSRF)
function getAuthHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    
    // Verifica se as tags existem (segurança para não quebrar o JS se faltar meta)
    if (!tokenMeta || !headerMeta) {
        console.warn("Meta tags CSRF não encontradas.");
        return { 'Content-Type': 'application/json' };
    }

    return {
        'Content-Type': 'application/json',
        [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
    };
}

/**
 * Exibe uma notificação usando Toastify
 * @param {string} mensagem - O texto a ser exibido.
 * @param {string} tipo - 'sucesso' ou 'erro'.
 */
function mostrarNotificacao(mensagem, tipo = 'sucesso') {
    const corFundo = tipo === 'sucesso' ? '#84cc16' : '#dc2626'; // Verde ou Vermelho

    // Verifica se a biblioteca Toastify foi carregada
    if (typeof Toastify === 'function') {
        Toastify({
            text: mensagem,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: {
                background: corFundo,
                borderRadius: "8px",
                boxShadow: "0 3px 6px rgba(0,0,0,0.16)"
            }
        }).showToast();
    } else {
        // Fallback caso o CSS/JS do Toastify falhe
        alert(mensagem);
    }
}

/**
 * Limpa e popula um <select> com dados de uma lista.
 */
function popularSelect(selectElement, lista, placeholder, campoId = 'id') {
    if (!selectElement) return;

    selectElement.innerHTML = ''; 

    const placeholderOption = document.createElement('option');
    placeholderOption.value = "";
    placeholderOption.textContent = placeholder;
    placeholderOption.disabled = true;
    placeholderOption.selected = true;
    selectElement.appendChild(placeholderOption);

    lista.forEach(item => {
        const option = document.createElement('option');
        // Usa colchetes para acessar a propriedade dinamicamente (ex: item['idCategoria'])
        option.value = item[campoId] || item.id; 
        option.textContent = item.nome; 
        selectElement.appendChild(option);
    });
}

// =================================================================================
// 2. DOMContentLoaded - INICIALIZAÇÃO DA PÁGINA
// =================================================================================

document.addEventListener("DOMContentLoaded", function() {
    
    // A. Verifica mensagem de sucesso pendente (Reload)
    const msgSucesso = sessionStorage.getItem('mensagemSucesso');
    if (msgSucesso) {
        mostrarNotificacao(msgSucesso, 'sucesso');
        sessionStorage.removeItem('mensagemSucesso');
    }

    // B. Configuração do Botão "Novo Produto" para carregar selects
    const btnAbrirModalProduto = document.getElementById('btn_novoProduto');
    const produtoCategoriaSelect = document.getElementById('produtoCategoriaSelect');
    const produtoMarcaSelect = document.getElementById('produtoMarcaSelect');

    if (btnAbrirModalProduto) {
        btnAbrirModalProduto.addEventListener('click', async (event) => {
            event.preventDefault(); // Impede comportamento padrão

            // Feedback visual
            if(produtoCategoriaSelect) produtoCategoriaSelect.innerHTML = '<option>Carregando...</option>';
            if(produtoMarcaSelect) produtoMarcaSelect.innerHTML = '<option>Carregando...</option>';

            try {
                // Chama APIs em paralelo
                const [responseCategorias, responseMarcas] = await Promise.all([
                    fetch('/api/categorias'), // Verifique se essa URL bate com seu Controller
                    fetch('/api/marcas')      // Verifique se essa URL bate com seu Controller
                ]);

                if (!responseCategorias.ok || !responseMarcas.ok) throw new Error("Erro ao buscar dados auxiliares");

                const categorias = await responseCategorias.json();
                const marcas = await responseMarcas.json();

                // Popula os selects
                // OBS: Verifique no seu DTO se os IDs são 'idCategoria' e 'idMarca' ou apenas 'id'
                popularSelect(produtoCategoriaSelect, categorias, "Selecione uma categoria", "idCategoria");
                popularSelect(produtoMarcaSelect, marcas, "Selecione uma marca", "idMarca");

                // Abre a modal (função global definida no HTML ou abaixo)
                if (typeof openModal === 'function') {
                    openModal('produto');
                } else {
                    document.getElementById('modalProduto').style.display = 'flex';
                }

            } catch (error) {
                console.error("Erro ao carregar dados para modal:", error);
                mostrarNotificacao("Erro ao carregar Categorias ou Marcas. Verifique o console.", "erro");
            }
        });
    }

    // C. Listener para Formulário de Marca (se existir na página e não for via botão onclick)
    const formNovaMarca = document.getElementById('formNovaMarca');
    if (formNovaMarca) {
        formNovaMarca.addEventListener('submit', async (event) => {
            event.preventDefault();
            await salvarMarca(); // Reutiliza a função global
        });
    }
});


// =================================================================================
// 3. FUNÇÕES DE CRUD (PRODUTO)
// =================================================================================

let paginaAtualProdutos = 0;

// Atualizar tabela de produtos (Paginação)
async function atualizarTabelaProdutos(pagina = 0) {
    const tbody = document.getElementById('tbody-produtos');
    if (!tbody) return;

    paginaAtualProdutos = pagina;
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Carregando...</td></tr>';

    try {
        const response = await fetch(`/produto/api/produto/listar?page=${pagina}`);
        
        if (!response.ok) throw new Error('Erro na API de listagem');
        
        const pageData = await response.json(); 
        const listaProdutos = pageData.content; 

        tbody.innerHTML = '';

        if (!listaProdutos || listaProdutos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Nenhum produto encontrado.</td></tr>';
            atualizarBotoesPaginacao(pageData, 'atualizarTabelaProdutos');
            return;
        }

        listaProdutos.forEach(prod => {
            const tr = document.createElement('tr');
            
            const valorFormatado = prod.valor 
                ? prod.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) 
                : 'R$ 0,00';
            
            // Trata objetos aninhados ou strings
            const categoriaNome = prod.categoria && prod.categoria.nome ? prod.categoria.nome : (prod.categoria || '-');
            const marcaNome = prod.marca && prod.marca.nome ? prod.marca.nome : (prod.marca || '-');

            tr.innerHTML = `
                <td>${prod.idProduto}</td>
                <td>${prod.nome}</td>
                <td>${valorFormatado}</td>
                <td>${prod.quantidade}</td>
                <td>${prod.descricao || ''}</td>
                <td>${categoriaNome}</td>
                <td>${marcaNome}</td>
                <td>
                    <button class="table-btn edit" onclick="editarProduto(${prod.idProduto})">Editar</button>
                    <button class="table-btn delete" onclick="deletarProduto(${prod.idProduto})">Excluir</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        atualizarBotoesPaginacao(pageData, 'atualizarTabelaProdutos');

    } catch (error) {
        console.error('Erro:', error);
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:red;">Erro ao carregar dados.</td></tr>';
    }
}

async function salvarProduto() {
    // 1. Coleta Dados
    const id = document.getElementById('prodId').value;
    const nome = document.getElementById('prodNome').value;
    const custo = document.getElementById('prodCusto').value;
    const valor = document.getElementById('prodValor').value;
    const qtd = document.getElementById('prodQtd').value;
    const descricao = document.getElementById('prodDesc').value;
    
    const selectCategoria = document.getElementById('produtoCategoriaSelect');
    const selectMarca = document.getElementById('produtoMarcaSelect');
    
    // Pega apenas os valores (IDs)
    const idCategoria = selectCategoria ? selectCategoria.value : null;
    const idMarca = selectMarca ? selectMarca.value : null;

    // 2. Validação
    if (!nome || !idCategoria || !idMarca) {
        mostrarNotificacao("Por favor, preencha Nome, Categoria e Marca.", "erro");
        return;
    }

    // 3. Monta o Objeto DTO (CORRIGIDO)
    // A estrutura agora envia 'idCategoria' e 'idMarca' como inteiros planos,
    // e NÃO envia os campos 'categoria'/'marca' como objetos.
    const produtoDTO = {
        idProduto: id ? parseInt(id) : null,
        nome: nome,
        custo: custo ? parseFloat(custo) : 0.0,
        valor: valor ? parseFloat(valor) : 0.0,
        quantidade: qtd ? parseInt(qtd) : 0,
        descricao: descricao,
        
        // CORREÇÃO AQUI: Envia direto para os campos Integer do DTO
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
            mostrarNotificacao("Produto salvo com sucesso!", "sucesso");
            
            if(typeof closeModal === 'function') closeModal('produto');
            else document.getElementById('modalProduto').style.display = 'none';

            atualizarTabelaProdutos(paginaAtualProdutos); 
            limparFormularioProduto();
        } else {
            const erroMsg = await response.text();
            mostrarNotificacao("Erro ao salvar: " + erroMsg, "erro");
        }

    } catch (error) {
        console.error("Erro de rede:", error);
        mostrarNotificacao("Erro ao conectar com o servidor.", "erro");
    }
}

async function deletarProduto(id) {
    if (!confirm("Tem certeza que deseja excluir este produto?")) return;

    try {
        const response = await fetch(`/produto/api/deletar/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            mostrarNotificacao("Produto removido!", "sucesso");
            atualizarTabelaProdutos(paginaAtualProdutos);
        } else {
            mostrarNotificacao("Erro ao remover. Verifique associações.", "erro");
        }
    } catch (error) {
        console.error("Erro:", error);
        mostrarNotificacao("Erro de conexão.", "erro");
    }
}

async function editarProduto(id) {
    try {
        // Primeiro, precisamos garantir que os selects estão populados
        // Se a modal nunca foi aberta, eles podem estar vazios.
        // Chamada rápida para carregar (poderia ser otimizado)
        await Promise.all([
             fetch('/api/categorias').then(r => r.json()).then(d => popularSelect(document.getElementById('produtoCategoriaSelect'), d, "Selecione", "idCategoria")),
             fetch('/api/marcas').then(r => r.json()).then(d => popularSelect(document.getElementById('produtoMarcaSelect'), d, "Selecione", "idMarca"))
        ]).catch(e => console.log("Erro ao pré-carregar selects na edição"));


        const response = await fetch(`/produto/api/${id}`);
        if (!response.ok) throw new Error("Erro ao buscar produto");
        const produto = await response.json();

        document.getElementById('prodId').value = produto.idProduto;
        document.getElementById('prodNome').value = produto.nome;
        document.getElementById('prodCusto').value = produto.custo;
        document.getElementById('prodValor').value = produto.valor;
        document.getElementById('prodQtd').value = produto.quantidade;
        document.getElementById('prodDesc').value = produto.descricao;
        
        // Tenta setar os selects
        if(produto.categoria && produto.categoria.idCategoria) {
            document.getElementById('produtoCategoriaSelect').value = produto.categoria.idCategoria;
        } else if (produto.idCategoria) {
            document.getElementById('produtoCategoriaSelect').value = produto.idCategoria;
        }

        if(produto.marca && produto.marca.idMarca) {
            document.getElementById('produtoMarcaSelect').value = produto.marca.idMarca;
        } else if (produto.idMarca) {
             document.getElementById('produtoMarcaSelect').value = produto.idMarca;
        }

        if(typeof openModal === 'function') openModal('produto');
        else document.getElementById('modalProduto').style.display = 'flex';

    } catch (error) {
        console.error("Erro:", error);
        mostrarNotificacao("Não foi possível carregar os dados para edição.", "erro");
    }
}

function limparFormularioProduto() {
    const inputs = ['prodId', 'prodNome', 'prodCusto', 'prodValor', 'prodQtd', 'prodDesc'];
    inputs.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.value = '';
    });
    
    const catSelect = document.getElementById('produtoCategoriaSelect');
    if(catSelect) catSelect.selectedIndex = 0;
    
    const marcSelect = document.getElementById('produtoMarcaSelect');
    if(marcSelect) marcSelect.selectedIndex = 0;
}


// =================================================================================
// 4. OUTRAS ENTIDADES (Cliente, OS, Marca, Categoria, Funcionario)
// =================================================================================

async function salvarMarca() {
    const input = document.getElementById('nomeMarcaInput');
    const nome = input ? input.value : '';

    if (!nome) {
        mostrarNotificacao('Digite o nome da marca!', 'erro');
        return;
    }

    const marcaDTO = { nome: nome };

    try {
        const response = await fetch('/api/marcas/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(marcaDTO)
        });

        if (response.ok) {
            mostrarNotificacao('Marca salva com sucesso!', 'sucesso');
            if(input) input.value = '';
            if(typeof closeModal === 'function') closeModal('Marca');
            
            // Reload para atualizar selects que dependem disso
            // Ou poderia recarregar via AJAX sem reload
            setTimeout(() => location.reload(), 1000); 
        } else {
            const txt = await response.text();
            mostrarNotificacao('Erro: ' + txt, 'erro');
        }
    } catch (e) {
        mostrarNotificacao('Erro ao conectar.', 'erro');
    }
}

async function salvarCategoria() {
    const input = document.getElementById('inputNomeCategoria');
    const nome = input ? input.value : '';

    if (!nome) {
        mostrarNotificacao('O nome da categoria é obrigatório!', 'erro');
        return;
    }

    const categoriaDTO = { nome: nome };

    try {
        // Ajuste a URL '/api/categorias/cadastrar' conforme seu controller
        const response = await fetch('/api/categorias/cadastrar', { 
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(categoriaDTO)
        });

        if (response.ok) {
            mostrarNotificacao('Categoria salva com sucesso!', 'sucesso');
            if(input) input.value = '';
            if(typeof closeModal === 'function') closeModal('Categoria');
            setTimeout(() => location.reload(), 1000); 
        } else {
            mostrarNotificacao('Erro ao salvar categoria.', 'erro');
        }
    } catch (error) {
        console.error(error);
        mostrarNotificacao('Erro de conexão com o servidor.', 'erro');
    }
}

async function salvarCliente() {
    // 1. Coleta os dados dos inputs
    const nome = document.getElementById('cliNome').value;
    const cpf = document.getElementById('cliCPF').value;
    const email = document.getElementById('cliEmail').value; 
    const telefone = document.getElementById('cliTel').value; // Verifique se o ID no HTML é 'cliTel' ou 'cliTelefone'
    
    // Dados do Endereço
    const cep = document.getElementById('cep').value;
    const rua = document.getElementById('rua').value;
    const bairro = document.getElementById('bairro').value;
    const numero = document.getElementById('numero').value;
    const cidade = document.getElementById('cidade').value;

    // Validação Simples
    if (!nome || !cpf) {
        mostrarNotificacao('Nome e CPF são obrigatórios!', 'erro');
        return;
    }

    // 2. Monta o Objeto DTO (CORRIGIDO)
    // A estrutura deve ser PLANA para bater com o ClienteDTO.java
    const clienteDTO = {
        id: null,
        nomeCliente: nome,   // Java: nomeCliente | JS Antigo: nome
        cpf: cpf,
        endEmail: email,     // Java: endEmail    | JS Antigo: email (estava comentado)
        telefone: telefone,  // Java: telefone
        
        // Campos de endereço diretos (sem objeto aninhado 'endereco: {}')
        cep: cep,
        rua: rua,
        bairro: bairro,
        numeroCasa: parseInt(numero || 0), // Convertendo para inteiro
        cidade: cidade
    };

    try {
        // 3. Envia para a API correta
        // ATENÇÃO: A URL combina o @RequestMapping da classe (/cliente) + o do método (/api/cliente/salvar)
        const response = await fetch('/cliente/api/cliente/salvar', { 
            method: 'POST',
            headers: getAuthHeaders(), // Usa nossa função auxiliar de segurança
            body: JSON.stringify(clienteDTO)
        });

        if (response.ok) {
            mostrarNotificacao('Cliente salvo com sucesso!', 'sucesso');
            if(typeof closeModal === 'function') closeModal('Cliente');
            
            // Limpa o formulário
            document.getElementById('cliNome').value = '';
            document.getElementById('cliCPF').value = '';
            document.getElementById('cliEmail').value = '';
            document.getElementById('cliTel').value = '';
            document.getElementById('cep').value = '';
            document.getElementById('rua').value = '';
            document.getElementById('bairro').value = '';
            document.getElementById('numero').value = '';
            document.getElementById('cidade').value = '';
            
            // Opcional: Atualizar tabela se existir
            // atualizarTabelaClientes(); 
        } else {
            const erroTexto = await response.text();
            mostrarNotificacao('Erro: ' + erroTexto, 'erro');
        }
    } catch (e) {
        console.error(e);
        mostrarNotificacao('Erro de conexão ao salvar cliente.', 'erro');
    }
}

async function salvarOS() {
    const cliente = document.getElementById('osCliente').value;
    const preco = document.getElementById('osPreco').value;
    const status = document.getElementById('osStatus').value;
    const idServico = document.getElementById('osServico').value; 

    const osDTO = {
        nomeCliente: cliente, // Verifique se o backend aceita String ou precisa de ID do cliente
        valor: parseFloat(preco || 0),
        statusOS: status,
        // servico: { id: idServico } 
    };

    try {
        const response = await fetch('/api/ordens/salvar', { 
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(osDTO)
        });

        if (response.ok) {
            mostrarNotificacao('O.S. salva com sucesso!', 'sucesso');
            if(typeof closeModal === 'function') closeModal('OrdemServico');
        } else {
            mostrarNotificacao('Erro ao salvar O.S.', 'erro');
        }
    } catch (e) {
        mostrarNotificacao('Erro de conexão.', 'erro');
    }
}

async function salvarServico() {
    const nome = document.getElementById('servNome').value;
    const descricao = document.getElementById('servDescricao').value;
    const preco = document.getElementById('servPreco').value;

    if (!nome || !preco) {
        mostrarNotificacao('Nome e Preço são obrigatórios!', 'erro');
        return;
    }

    const servicoDTO = {
        nome: nome,
        descricao: descricao,
        valor: parseFloat(preco)
    };

    try {
        const response = await fetch('/api/servicos/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(servicoDTO)
        });

        if (response.ok) {
            mostrarNotificacao('Serviço salvo!', 'sucesso');
            if(typeof closeModal === 'function') closeModal('Servico');
            document.getElementById('servNome').value = '';
            document.getElementById('servPreco').value = '';
        } else {
            mostrarNotificacao('Erro ao salvar serviço.', 'erro');
        }
    } catch (e) {
        mostrarNotificacao('Erro de conexão.', 'erro');
    }
}

async function salvarFuncionario() {
    const nome = document.getElementById('funcNome').value;
    const cpf = document.getElementById('funcCpf').value;
    const nivelAcesso = document.getElementById('funcAcesso').value;
    const salario = document.getElementById('funcSalario').value;

    if (!nome || !cpf) {
        mostrarNotificacao('Nome e CPF são obrigatórios!', 'erro');
        return;
    }

    const funcionarioDTO = {
        nome: nome,
        cpf: cpf,
        dataNascimento: document.getElementById('funcDataNasc').value,
        // Ajustar conforme DTO do backend
        // email: document.getElementById('funcEmail').value,
        // telefone: document.getElementById('funcTel').value,
        endereco: {
            cep: document.getElementById('cep').value, // Cuidado: IDs de endereço podem estar duplicados no HTML (modal Cliente vs Funcionario)
            rua: document.getElementById('rua').value,
            bairro: document.getElementById('bairro').value,
            numero: document.getElementById('numero').value,
            cidade: document.getElementById('cidade').value
        },
        nivelAcesso: nivelAcesso,
        salario: parseFloat(salario || 0)
    };

    try {
        const response = await fetch('/api/funcionarios/salvar', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(funcionarioDTO)
        });

        if (response.ok) {
            mostrarNotificacao('Funcionário salvo com sucesso!', 'sucesso');
            if(typeof closeModal === 'function') closeModal('Funcionario');
        } else {
            mostrarNotificacao('Erro ao salvar funcionário.', 'erro');
        }
    } catch (e) {
        mostrarNotificacao('Erro de conexão.', 'erro');
    }
}


// =================================================================================
// 5. UTILS (Paginação e Outros)
// =================================================================================

function atualizarBotoesPaginacao(pageData, nomeFuncaoCallback) {
    const container = document.getElementById('paginacao-container');
    if (!container) return;

    let html = '';

    // Botão Anterior
    if (!pageData.first) {
        html += `<button class="page-btn" onclick="${nomeFuncaoCallback}(${pageData.number - 1})"> < Anterior </button>`;
    }

    // Texto informativo
    html += ` <span class="page-info" style="margin: 0 10px;">Página ${pageData.number + 1} de ${pageData.totalPages}</span> `;

    // Botão Próximo
    if (!pageData.last) {
        html += `<button class="page-btn" onclick="${nomeFuncaoCallback}(${pageData.number + 1})"> Próximo > </button>`;
    }

    container.innerHTML = html;
}