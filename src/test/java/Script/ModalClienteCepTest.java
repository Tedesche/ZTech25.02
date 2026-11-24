package Script;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ModalClienteCepTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    // Seu HTML exato + seu script JS
    private static final String HTML_MODAL_CLIENTE = """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <body>
            <div id="modalCliente" class="modal">
                <div class="modal-content" style="max-width: 750px;">
                    <span class="close" onclick="closeModal('Cliente')">&times;</span>
                    <h2 style="margin-bottom: 25px;">Cadastrar Novo Cliente</h2>
                    <section>
                        <div style="width: 50%;">
                            <div class="form-group">
                                <label>Nome:</label>
                                <input type="text" id="cliNome" placeholder="Digite o Nome Completo">
                            </div>
                            <div class="form-group">
                                <label>CPF:</label>
                                <input type="text" id="cliCPF" placeholder="Digite o CPF">
                            </div>
                            <div class="form-group">
                                <label>Email:</label>
                                <input type="email" id="cliEmail" placeholder="exemplo@exemplo.com">
                            </div>
                            <div class="form-group">
                                <label>Telefone:</label>
                                <input type="tel" id="cliTel" placeholder="(11) 99999-9999">
                            </div>
                        </div>
                        <div style="width: 50%;">
                            <div style="display: flex; gap: 5px;">
                                <div class="form-group" style="width: 100%;">
                                    <label>CEP:</label>
                                    <input type="text" id="cliCep" placeholder="Digite o CEP" onblur="buscarCep('cli')">
                                </div>
                                <button class="action-btn btn-primary" onclick="buscarCep('cli')"
                                    style="width: 14%; height: 40px; margin-top: auto; margin-bottom: 22px; padding: 10px 0; float: inline-end;"
                                    title="Buscar CEP">
                                    🔍
                                </button>
                            </div>
                            <div class="form-group">
                                <label>Rua:</label>
                                <input type="text" id="cliRua" placeholder="Rua">
                            </div>
                            <div style="display: flex; gap: 5px;">
                                <div class="form-group" style="width: 70%;">
                                    <label>Bairro:</label>
                                    <input type="text" id="cliBairro" placeholder="Bairro">
                                </div>
                                <div class="form-group" style="width: 30%;">
                                    <label>Numero:</label>
                                    <input type="number" id="cliNumero" placeholder="Nº">
                                </div>
                            </div>
                            <div class="form-group">
                                <label>Cidade:</label>
                                <input type="text" id="cliCidade" placeholder="Cidade">
                            </div>
                        </div>
                    </section>
                    <div style="display: flex; gap: 10px; margin-top: 25px;">
                        <button class="action-btn btn-primary" onclick="salvarCliente()" style="flex: 1;">Salvar Cliente</button>
                        <button class="action-btn btn-secondary" onclick="closeModal('Cliente')">Cancelar</button>
                    </div>
                </div>
            </div>

            <script>
                // Dublês (Mocks) das funções externas do seu sistema para isolar o teste
                window.notificacoes = [];
                function mostrarNotificacao(mensagem, tipo) {
                    window.notificacoes.push({ mensagem, tipo });
                }
                function closeModal(nome) {}
                function salvarCliente() {}

                // Seu script original (atenção ao \\D escapado para o Text Block do Java)
                function buscarCep(prefixo) {
                    const cepInput = document.getElementById(prefixo + 'Cep');
                    if (!cepInput) return;

                    let cep = cepInput.value.replace(/\\D/g, '');
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
            </script>
        </body>
        </html>
        """;

    @BeforeAll
    static void iniciarNavegador() {
        playwright = Playwright.create();
     // Em iniciarNavegador() (linha 144), substitua o launch atual por:
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(false)
        );
    }

    @AfterAll
    static void fecharNavegador() {
        playwright.close();
    }

    @BeforeEach
    void prepararPagina() {
        context = browser.newContext();
        page = context.newPage();
        page.setContent(HTML_MODAL_CLIENTE);
    }

    @AfterEach
    void limparContexto() {
        context.close();
    }

    @Test
    @DisplayName("Deve preencher endereço, remover máscara do CEP e focar no campo Número ao clicar na lupa")
    void devePreencherEnderecoEFocarNoNumero() {
        // Intercepta a URL já sem o hífen (validando o replace(/\D/g, ''))
        page.route("https://viacep.com.br/ws/01001000/json/", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("""
                    {
                        "logradouro": "Praça da Sé",
                        "bairro": "Sé",
                        "localidade": "São Paulo"
                    }
                    """));
        });

        // Preenche com hífen e clica no botão da lupa
        page.fill("#cliCep", "01001-000");
        page.click("button[title='Buscar CEP']");

        // Aguarda a resposta assíncrona do fetch atualizar o DOM
        page.waitForFunction("document.getElementById('cliRua').value === 'Praça da Sé'");

        assertEquals("Praça da Sé", page.inputValue("#cliRua"));
        assertEquals("Sé", page.inputValue("#cliBairro"));
        assertEquals("São Paulo", page.inputValue("#cliCidade"));

        // Verifica se o foco foi movido automaticamente para o input #cliNumero
        String elementoFocado = (String) page.evaluate("document.activeElement.id");
        assertEquals("cliNumero", elementoFocado);
    }

    @Test
    @DisplayName("Deve limpar campos anteriores e exibir notificação quando o ViaCEP retornar erro: true")
    void deveLimparCamposQuandoCepNaoForEncontrado() {
        // Simula campos que já estavam preenchidos antes
        page.fill("#cliRua", "Rua Antiga");
        page.fill("#cliBairro", "Bairro Antigo");
        page.fill("#cliCidade", "Cidade Antiga");

        page.route("https://viacep.com.br/ws/99999999/json/", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("{\"erro\": true}"));
        });

        // Desta vez dispara via onblur (saindo do campo com Tab)
        page.fill("#cliCep", "99999999");
        page.locator("#cliCep").blur();

        page.waitForFunction("window.notificacoes.length > 0");

        assertEquals("", page.inputValue("#cliRua"));
        assertEquals("", page.inputValue("#cliBairro"));
        assertEquals("", page.inputValue("#cliCidade"));
        assertEquals("CEP não encontrado!", page.evaluate("window.notificacoes[0].mensagem"));
        assertEquals("erro", page.evaluate("window.notificacoes[0].tipo"));
    }

    @Test
    @DisplayName("Deve notificar erro de validação se o CEP tiver quantidade diferente de 8 dígitos")
    void deveValidarQuantidadeDeDigitosDoCep() {
        page.fill("#cliCep", "12345");
        page.locator("#cliCep").blur(); // Dispara o onblur

        // Valida a notificação gerada pelo onblur
        assertEquals(1, page.evaluate("window.notificacoes.length"));
        assertEquals("CEP inválido (deve ter 8 dígitos).", page.evaluate("window.notificacoes[0].mensagem"));
        assertEquals("erro", page.evaluate("window.notificacoes[0].tipo"));

        // Limpa o histórico e valida também o clique direto no botão da lupa
        page.evaluate("window.notificacoes = []");
        page.click("button[title='Buscar CEP']");

        assertEquals(1, page.evaluate("window.notificacoes.length"));
        assertEquals("CEP inválido (deve ter 8 dígitos).", page.evaluate("window.notificacoes[0].mensagem"));
    }

    @Test
    @DisplayName("Deve notificar erro de conexão e restaurar placeholder no finally quando a rede falhar")
    void deveTratarFalhaDeRedeERestaurarPlaceholder() {
        page.route("https://viacep.com.br/ws/01001000/json/", Route::abort);

        page.fill("#cliCep", "01001000");
        page.click("button[title='Buscar CEP']");

        page.waitForFunction("window.notificacoes.length > 0");

     // 2. Atualize a linha 255 para:
        PlaywrightAssertions.assertThat(page.locator("#cliRua")).hasAttribute("placeholder", "Rua");
        assertThat(page.locator("#cliRua")).hasAttribute("placeholder", "Rua");
    }
}