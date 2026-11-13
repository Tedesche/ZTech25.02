package ZtechAplication.integration;

import ZtechAplication.model.Cliente;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Venda;
import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.repository.VendaRepository;
import ZtechAplication.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional // Muito importante: o Spring apaga esta venda de teste da base de dados logo a seguir
public class DashboardIntegrationTest {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    public void deveSincronizarDashboardAposNovaVenda() {
        // 1. ESTADO ATUAL: Verificamos o total antes da inserção
        long vendasIniciais = dashboardService.contarTotalDeVendas();

        // 2. AÇÃO: Criamos a nova venda
        Venda novaVenda = new Venda();

        // --- PREENCHIMENTO DAS CHAVES ESTRANGEIRAS OBRIGATÓRIAS ---
        Cliente clienteTeste = clienteRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado na base de dados."));
        novaVenda.setCliente(clienteTeste); 

        Produto produtoTeste = produtoRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado na base de dados."));
        novaVenda.setProduto(produtoTeste);


        // --- PREENCHIMENTO DOS CAMPOS PRIMITIVOS OBRIGATÓRIOS ---
        novaVenda.setValor(new BigDecimal("200.00")); // O valor exato pedido no teste da sua tabela
        novaVenda.setLucro(new BigDecimal("50.00"));
        novaVenda.setQuantidade(1);
        
        // Guardamos a venda na base de dados
        vendaRepository.save(novaVenda);

        // 3. NOVO ESTADO: Verificamos o total depois da inserção
        long vendasAposInsercao = dashboardService.contarTotalDeVendas();

        // 4. VALIDAÇÃO: O contador tem de ter subido exatamente 1 valor
        assertEquals(vendasIniciais + 1, vendasAposInsercao, 
                "O dashboard analítico deve refletir o novo registo inserido na base de dados em tempo real.");
    }
}