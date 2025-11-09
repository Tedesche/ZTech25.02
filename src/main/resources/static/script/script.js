

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
 * @param {HTMLSelectElement} selectElement - O elemento <select>
 * @param {Array} lista - A lista de dados (ex: [{id: 1, nome: 'Categoria 1'}])
 * @param {String} placeholder - O texto inicial (ex: "Selecione uma categoria")
 */
function popularSelect(selectElement, lista, placeholder) {
    // Limpa opções antigas (exceto a primeira, se for o placeholder)
    selectElement.innerHTML = ''; 

    // Adiciona o placeholder
    const placeholderOption = document.createElement('option');
    placeholderOption.value = "";
    placeholderOption.textContent = placeholder;
    placeholderOption.disabled = true; // Opcional: faz não ser selecionável
    placeholderOption.selected = true; // Opcional: deixa selecionado por padrão
    selectElement.appendChild(placeholderOption);

    // Adiciona as opções da lista
    lista.forEach(item => {
        const option = document.createElement('option');
        // O 'value' deve ser o ID (chave estrangeira)
        option.value = item.id; 
        // O texto visível é o nome
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
        popularSelect(produtoCategoriaSelect, categorias, "Selecione uma categoria");
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
        popularSelect(produtoMarcaSelect, marcas, "Selecione uma marca");
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