package ZtechAplication.e2e;

import ZtechAplication.model.Produto;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.service.OtpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ZtechSistemaE2ETest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:8080";

    @Autowired
    private OtpService otpService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    public void abrirNavegador() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(6));
    }

    @AfterEach
    public void fecharNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    // 1. RF-Geral: Login via 2FA (admin / 1234)
    @Test
    public void deveRealizarLoginComDuplaAutenticacao() {
        realizarLoginCompleto();
        WebElement dashboard = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard-section")));
        assertTrue(dashboard.isDisplayed(), "O painel principal deve estar visível após o login com 2FA.");
    }

    // 2. RF-OS: Jornada de Ordem de Serviço
    @Test
    public void deveCriarEEvoluirStatusOrdemDeServico() {
        realizarLoginCompleto();

        // Abre a aba O.S. e clica em "+ Nova O. S."
        driver.findElement(By.cssSelector("button.nav-btn[onclick*=\"showSection('os')\"]")).click();
        WebElement btnNovaOs = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#os-section button[onclick=\"openModal('OrdemServico')\"]")
        ));
        btnNovaOs.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOrdemServico")));

        // Aguarda o fetch() popular os selects do modal
        aguardarOpcoesCarregarem(By.id("osCliente"));
        aguardarOpcoesCarregarem(By.id("osServico"));
        aguardarOpcoesCarregarem(By.id("osProduto"));

        new Select(driver.findElement(By.id("osCliente"))).selectByIndex(1);
        new Select(driver.findElement(By.id("osServico"))).selectByIndex(1);
        new Select(driver.findElement(By.id("osProduto"))).selectByIndex(1);

        WebElement campoPreco = driver.findElement(By.id("osPreco"));
        campoPreco.clear();
        campoPreco.sendKeys("350.00");

        // Define/evolui status para "Em Andamento"
        Select selectStatus = new Select(driver.findElement(By.id("osStatus")));
        selectStatus.selectByValue("EM ANDAMENTO");
        assertEquals("Em Andamento", selectStatus.getFirstSelectedOption().getText());

        driver.findElement(By.cssSelector("#modalOrdemServico button[onclick='salvarOS()']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("modalOrdemServico")));
    }

    // 3. RNF-UX: Validação visual de formulário vazio
    @Test
    public void deveExibirErroVisualAoSalvarClienteComFormularioVazio() {
        realizarLoginCompleto();

        // Navega para aba O.S. e abre o modal de Novo Cliente
        driver.findElement(By.cssSelector("button.nav-btn[onclick*=\"showSection('os')\"]")).click();
        WebElement btnNovoCliente = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[onclick=\"openModal('Cliente')\"]")
        ));
        btnNovoCliente.click();

        WebElement modalCliente = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalCliente")));
        driver.findElement(By.cssSelector("#modalCliente button[onclick='salvarCliente()']")).click();

        // Valida que o modal permaneceu aberto e bloqueou o envio vazio
        assertTrue(modalCliente.isDisplayed(), "O modal de cliente não deve fechar quando campos obrigatórios estiverem vazios.");
    }

    // 4. RF-Vendas: Jornada de Vendas + Dashboard em tempo real
    @Test
    public void deveRealizarVendaNoPdvEAtualizarDashboard() {
        realizarLoginCompleto();

        WebElement totalVendasEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dash-total-vendas")));
        int vendasIniciais = Integer.parseInt(totalVendasEl.getText().trim());

        // Abre o modal de Nova Venda diretamente do card do Dashboard
        driver.findElement(By.cssSelector("#dashboard-section button[onclick=\"openModal('venda2')\"]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalVenda2")));

        aguardarOpcoesCarregarem(By.id("venda2ClienteSelect"));
        aguardarOpcoesCarregarem(By.id("produto-select"));

        new Select(driver.findElement(By.id("venda2ClienteSelect"))).selectByIndex(1);
        new Select(driver.findElement(By.id("produto-select"))).selectByIndex(1);

        driver.findElement(By.id("btn-adicionar-produto")).click();
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElementLocated(By.id("carrinho-itens-lista"), "Nenhum item adicionado ainda.")
        ));

        driver.findElement(By.id("btn-finalizar-venda")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("modalVenda2")));

        // Recarrega o Dashboard para validar o contador sincronizado com o banco
        driver.navigate().refresh();
        WebElement totalVendasAtualizado = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dash-total-vendas")));
        int vendasFinais = Integer.parseInt(totalVendasAtualizado.getText().trim());

        assertEquals(vendasIniciais + 1, vendasFinais, "O indicador de pedidos no Dashboard deve incrementar após a venda.");
    }

    // 5. RNF-API: Auto-preenchimento de endereço via CEP
    @Test
    public void devePreencherEnderecoAutomaticamenteAoDigitarCep() {
        realizarLoginCompleto();

        driver.findElement(By.cssSelector("button.nav-btn[onclick*=\"showSection('os')\"]")).click();
        WebElement btnNovoCliente = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[onclick=\"openModal('Cliente')\"]")
        ));
        btnNovoCliente.click();

        WebElement campoCep = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cliCep")));
        campoCep.sendKeys("01001-000");
        campoCep.sendKeys(Keys.TAB); // Dispara onblur="buscarCep('cli')"

        WebElement campoRua = driver.findElement(By.id("cliRua"));
        wait.until(ExpectedConditions.attributeContains(campoRua, "value", "Praça da Sé"));

        assertEquals("Praça da Sé", campoRua.getAttribute("value"));
        assertEquals("Sé", driver.findElement(By.id("cliBairro")).getAttribute("value"));
        assertEquals("São Paulo", driver.findElement(By.id("cliCidade")).getAttribute("value"));
    }

    // 6. RF-Estoque: Alerta visual de estoque crítico na Home/Dashboard
    @Test
    public void deveExibirNotificacaoDeEstoqueCriticoNaHome() {
        Produto produto = produtoRepository.findById(10).orElseThrow();
        int quantidadeOriginal = produto.getQuantidade();

        try {
            produto.setQuantidade(0); // Força estoque crítico abaixo do mínimo (1)
            produtoRepository.save(produto);

            realizarLoginCompleto();

            WebElement indicadorEstoqueBaixo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dash-estoque-baixo")));
            int qtdCritica = Integer.parseInt(indicadorEstoqueBaixo.getText().trim());

            assertTrue(indicadorEstoqueBaixo.isDisplayed() && qtdCritica >= 1,
                    "O indicador de estoque baixo no painel inicial deve alertar ao menos 1 produto crítico.");
        } finally {
            produto.setQuantidade(quantidadeOriginal);
            produtoRepository.save(produto);
        }
    }

    private void aguardarOpcoesCarregarem(By selectLocator) {
        wait.until(d -> d.findElement(selectLocator).findElements(By.tagName("option")).size() > 1);
    }

    private void realizarLoginCompleto() {
        driver.get(BASE_URL + "/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("1234");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(d -> !d.getCurrentUrl().endsWith("/login"));

        if (driver.findElements(By.id("dashboard-section")).isEmpty()) {
            String tokenValido = otpService.generateAndSaveOtp("admin");
            WebElement campoToken = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[name='token'], input[name='otp'], input[name='codigo'], input[type='text']")
            ));
            campoToken.sendKeys(tokenValido);
            driver.findElement(By.cssSelector("button[type='submit']")).click();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard-section")));
    }
}