

// Em static/script/script.js
// ... (dentro do DOMContentLoaded)

const formNovaMarca = document.getElementById('formNovaMarca');
const modalNovaMarca = 'modalMarca'; // ID da div da modal
const nomeMarcaInput = document.getElementById('nomeMarcaInput');
const marcaErrorMsg = document.getElementById('marcaErrorMsg');

if (formNovaMarca) {
    formNovaMarca.addEventListener('submit', async (event) => {
        // 1. Impedir o envio tradicional que recarrega a página!
        event.preventDefault(); 
        

        // 2. Coletar os dados do input
        const nomeMarca = nomeMarcaInput.value;

        // 3. Criar o objeto de dados (JSON)
        // A estrutura deve ser idêntica à sua classe `Marca.java`
        const dadosMarca = {
            nome: nomeMarca
            // 'id' não é necessário, o banco de dados irá gerar
        };

        // 4. Pegar o Token CSRF (ESSENCIAL para Spring Security)
        // (Vou assumir que você tem os <meta> tags no seu HTML principal)
        const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        try {
            // 5. Enviar os dados para a API
            const response = await fetch('/api/marcas/salvar', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [header]: token // Envia o token CSRF no header
                },
                body: JSON.stringify(dadosMarca) // Converte o objeto JS em JSON
            });

            if (response.ok) {
                // SUCESSO!
                const marcaSalva = await response.json();
                console.log('Marca salva:', marcaSalva);
                
                // Fechar a modal
                closeModal(modalNovaMarca); // (usando sua função)
                
                // Limpar o campo
                nomeMarcaInput.value = '';
                
                // Opcional: Adicionar a nova marca na tabela dinamicamente (mais avançado)
                
                // Por enquanto, a solução mais simples é recarregar a página
                // para ver a nova marca na lista.
				// 4. ADICIONE A NOVA CHAMADA
				                // Recarrega a lista de marcas na modal de produto
				                await carregarMarcas();
                alert('Marca salva com sucesso!');
                location.reload(); 

            } else {
                // Erro (Ex: nome vazio, erro do servidor)
                const erroTexto = await response.text();
                marcaErrorMsg.textContent = erroTexto;
            }

        } catch (error) {
            // Erro de rede ou JavaScript
            console.error('Erro de rede:', error);
            marcaErrorMsg.textContent = 'Não foi possível conectar ao servidor.';
        }
    });
}



// populando modals
// Em static/script/script.js
// (dentro do 'DOMContentLoaded')

// 1. Pegue o BOTÃO que abre a modal (use o ID correto do seu botão)
const btnAbrirModalProduto = document.getElementById('btn_novoProduto'); // <-- Troque pelo ID do seu botão

// 2. Pegue os <select> de dentro da modal
const produtoCategoriaSelect = document.getElementById('produtoCategoriaSelect');
const produtoMarcaSelect = document.getElementById('produtoMarcaSelect');

// 3. Crie uma função para popular um <select>
//    (Isso evita repetir código)
/**
 * Limpa e popula um <select> com dados de uma API.
 * @param {HTMLSelectElement} selectElement
 * @param {Array} lista
 * @param {String} placeholder
 * @param {String} campoId - O NOME do campo de ID (ex: 'idCategoria', 'idMarca')
 */
function popularSelect(selectElement, lista, placeholder, campoId = 'id') {
    selectElement.innerHTML = ''; 

    const placeholderOption = document.createElement('option');
    placeholderOption.value = "";
    placeholderOption.textContent = placeholder;
    placeholderOption.disabled = true;
    placeholderOption.selected = true;
    selectElement.appendChild(placeholderOption);

    lista.forEach(item => {
        const option = document.createElement('option');
        // CORREÇÃO AQUI: Usa o nome do campo passado por parâmetro
        option.value = item[campoId]; 
        option.textContent = item.nome; 
        selectElement.appendChild(option);
    });
}

/**
 * NOVA FUNÇÃO REUTILIZÁVEL 1: Carrega apenas as categorias
 */
async function carregarCategorias() {
    try {
        const response = await fetch('/api/categorias');
        if (!response.ok) throw new Error('Falha ao buscar categorias');
        
        const categorias = await response.json();
        popularSelect(produtoCategoriaSelect, categorias, "Selecione uma categoria", "idCategoria");
    } catch (error) {
        console.error("Erro ao carregar categorias:", error);
        produtoCategoriaSelect.innerHTML = '<option value="">Erro ao carregar</option>';
    }
}

/**
 * NOVA FUNÇÃO REUTILIZÁVEL 2: Carrega apenas as marcas
 */
async function carregarMarcas() {
    try {
        const response = await fetch('/api/marcas');
        if (!response.ok) throw new Error('Falha ao buscar marcas');

        const marcas = await response.json();
        popularSelect(produtoMarcaSelect, marcas, "Selecione uma marca", "idMarca");
    } catch (error) {
        console.error("Erro ao carregar marcas:", error);
        produtoMarcaSelect.innerHTML = '<option value="">Erro ao carregar</option>';
    }
}

// 4. Adicione o "ouvinte" de clique ao botão
if (btnAbrirModalProduto) {
    btnAbrirModalProduto.addEventListener('click', async (event) => {
        event.preventDefault(); // Impede qualquer ação padrão

        // Mostra "carregando" nos dropdowns
        produtoCategoriaSelect.innerHTML = '<option>Carregando...</option>';
        produtoMarcaSelect.innerHTML = '<option>Carregando...</option>';

        try {
            // 5. Chame as duas APIs em paralelo (mais rápido)
            const [responseCategorias, responseMarcas] = await Promise.all([
                fetch('/api/categorias'),
                fetch('/api/marcas')
            ]);

            const categorias = await responseCategorias.json();
            const marcas = await responseMarcas.json();

            // 6. Use a função helper para popular os selects
            popularSelect(produtoCategoriaSelect, categorias, "Selecione uma categoria");
            popularSelect(produtoMarcaSelect, marcas, "Selecione uma marca");

            // 7. SÓ AGORA, abra a modal (pois os dados já carregaram)
            openModal('modalNovoProduto'); // (Use sua função de abrir modal)

        } catch (error) {
            console.error("Erro ao carregar dados para modal:", error);
            alert("Não foi possível carregar os dados. Tente novamente.");
        }
    });
}


// ----------- ATUALIZAR TABELAS A BAIXO ----------------------------------------------
// --- FUNÇÃO PARA ATUALIZAR TABELA DE PRODUTOS ---
async function atualizarTabelaProdutos() {
    const tbody = document.getElementById('tbody-produtos');
    if (!tbody) return; // Segurança

    // Mostra um "Carregando..."
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center">Carregando estoque atualizado...</td></tr>';

    try {
        // Chama o novo endpoint do Java
        const response = await fetch('/produto/api/produto/listar');
        if (!response.ok) throw new Error('Erro na API de produtos');
        
        const listaProdutos = await response.json();

        // Limpa a tabela
        tbody.innerHTML = '';

        if (listaProdutos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center">Nenhum produto encontrado.</td></tr>';
            return;
        }

        // Desenha as linhas
        listaProdutos.forEach(prod => {
            const tr = document.createElement('tr');
            
            // Formata o valor para R$ (segurança se for nulo)
            const valorFormatado = prod.valor 
                ? prod.valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) 
                : 'R$ 0,00';
            
            // Trata nulos de categoria/marca
            const categoria = prod.categoria ? prod.categoria : '-';
            const marca = prod.marca ? prod.marca : '-';

            tr.innerHTML = `
                <td>${prod.idProduto}</td>
                <td>${prod.nome}</td>
                <td>${valorFormatado}</td>
                <td>${prod.quantidade}</td>
                <td>${prod.descricao || ''}</td>
                <td>${categoria}</td>
                <td>${marca}</td>
                <td>
                    <button class="table-btn edit" onclick="editarProduto(${prod.idProduto})">Editar</button>
                    <button class="table-btn delete" onclick="deletarProduto(${prod.idProduto})">Excluir</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Erro:', error);
        tbody.innerHTML = '<tr><td colspan="8" style="color:red; text-align:center">Erro ao carregar dados.</td></tr>';
    }
}



// ---------- SALVAR DAS MODALS A BAIXO -------------------------------------------
// --- FUNÇÃO PARA SALVAR (CADASTRAR/EDITAR) PRODUTO ---
async function salvarProduto() {
    // 1. Coleta os dados dos inputs pelo ID que criamos
    const id = document.getElementById('prodId').value; // Vazio se for novo
    const nome = document.getElementById('prodNome').value;
    const custo = document.getElementById('prodCusto').value;
    const valor = document.getElementById('prodValor').value;
    const qtd = document.getElementById('prodQtd').value;
    const descricao = document.getElementById('prodDesc').value;
    const idCategoria = document.getElementById('produtoCategoriaSelect').value;
    const idMarca = document.getElementById('produtoMarcaSelect').value;

    // 2. Validação simples no Frontend
    if (!nome || !idCategoria || !idMarca) {
        alert("Por favor, preencha Nome, Categoria e Marca.");
        return;
    }

    // 3. Monta o Objeto DTO (igual ao Java espera)
    const produtoDTO = {
        idProduto: id ? parseInt(id) : null,
        nome: nome,
        custo: custo ? parseFloat(custo) : 0.0,
        valor: valor ? parseFloat(valor) : 0.0,
        quantidade: qtd ? parseInt(qtd) : 0,
        descricao: descricao,
        idCategoria: parseInt(idCategoria),
        idMarca: parseInt(idMarca)
        // Nota: Não precisamos mandar os nomes de categoria/marca, só os IDs
    };

    // 4. Pega o Token CSRF (Segurança do Spring)
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    try {
        const response = await fetch('/produto/api/produto/salvar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify(produtoDTO)
        });

        if (response.ok) {
            alert("Produto salvo com sucesso!");
            
            // 5. Atualiza a tabela SEM recarregar a página
            closeModal('produto');
            atualizarTabelaProdutos(); 
            
            // 6. Limpa o formulário para o próximo
            limparFormularioProduto();
        } else {
            const erroMsg = await response.text();
            alert("Erro ao salvar: " + erroMsg);
        }

    } catch (error) {
        console.error("Erro de rede:", error);
        alert("Erro ao conectar com o servidor.");
    }
}

// --- FUNÇÕES DE AÇÃO (EDITAR E DELETAR) ---

// Função chamada pelo botão "Excluir" da tabela
async function deletarProduto(id) {
    if (!confirm("Tem certeza que deseja excluir este produto?")) return;

    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    try {
        const response = await fetch(`/produto/api/deletar/${id}`, {
            method: 'DELETE', // Importante: Método DELETE
            headers: {
                [header]: token
            }
        });

        if (response.ok) {
            alert("Produto removido!");
            atualizarTabelaProdutos(); // Atualiza a tela sem recarregar
        } else {
            alert("Erro ao remover. Verifique se o produto não tem vendas associadas.");
        }
    } catch (error) {
        console.error("Erro:", error);
    }
}

// Função chamada pelo botão "Editar" da tabela
async function editarProduto(id) {
    try {
        // 1. Busca os dados do produto para preencher a modal
        const response = await fetch(`/produto/api/${id}`);
        if (!response.ok) throw new Error("Erro ao buscar produto");
        const produto = await response.json();

        // 2. Preenche os campos da modal (usando os IDs que criamos)
        document.getElementById('prodId').value = produto.idProduto;
        document.getElementById('prodNome').value = produto.nome;
        document.getElementById('prodCusto').value = produto.custo;
        document.getElementById('prodValor').value = produto.valor;
        document.getElementById('prodQtd').value = produto.quantidade;
        document.getElementById('prodDesc').value = produto.descricao;
        
        // 3. Seleciona a Categoria e Marca corretas
        // (Isso assume que os selects já foram carregados previamente)
        if(produto.idCategoria) document.getElementById('produtoCategoriaSelect').value = produto.idCategoria;
        if(produto.idMarca) document.getElementById('produtoMarcaSelect').value = produto.idMarca;

        // 4. Abre a modal
        openModal('produto');

    } catch (error) {
        console.error("Erro:", error);
        alert("Não foi possível carregar os dados para edição.");
    }
}

// Função auxiliar para limpar o form
function limparFormularioProduto() {
    document.getElementById('prodId').value = '';
    document.getElementById('prodNome').value = '';
    document.getElementById('prodCusto').value = '';
    document.getElementById('prodValor').value = '';
    document.getElementById('prodQtd').value = '';
    document.getElementById('prodDesc').value = '';
    // Reseta selects para a primeira opção
    document.getElementById('produtoCategoriaSelect').selectedIndex = 0;
    document.getElementById('produtoMarcaSelect').selectedIndex = 0;
}
