package ZtechAplication.integration;

import ZtechAplication.model.Produto;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.service.CompraService;
import ZtechAplication.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class EstoqueIntegrationTest {

    @Autowired
    private ProdutoService produtoService;

    // INSERÇÃO: Trazemos o repositório para podermos manipular o banco no teste
    @Autowired
    private ProdutoRepository produtoRepository;

    @MockBean
    private CompraService compraService;

    @Test
    public void deveBaixarEstoqueEAcionarComprasQuandoAtingirMinimo() {
        // 1. PREPARAÇÃO: Forçamos os valores exatos na memória do banco antes de testar
        Produto produto = produtoRepository.findById(1).orElseThrow();
        produto.setQuantidade(15);
        produto.setQuantidadeMinima(10);
        produtoRepository.save(produto);

        // 2. AÇÃO: Registramos a saída de 5 itens (15 - 5 = 10, que aciona o alerta crítico)
        produtoService.registrarSaidaDeEstoque(1, 5);

        // 3. VALIDAÇÃO: O módulo de compras DEVE ser acionado
        verify(compraService, times(1)).solicitarReposicao(any());
    }

    @Test
    public void deveBaixarEstoqueSemAcionarComprasQuandoHouverFolga() {
        // 1. PREPARAÇÃO: Criamos um cenário de folga no estoque
        Produto produto = produtoRepository.findById(2).orElseThrow();
        produto.setQuantidade(50);
        produto.setQuantidadeMinima(10);
        produtoRepository.save(produto);

        // 2. AÇÃO: Saída de apenas 5 itens (50 - 5 = 45, muita folga)
        produtoService.registrarSaidaDeEstoque(2, 5);

        // 3. VALIDAÇÃO: O módulo de compras NÃO DEVE ser acionado
        verify(compraService, never()).solicitarReposicao(any());
    }
}