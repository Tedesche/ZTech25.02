package ZtechAplication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ZtechAplication.model.Produto;
import ZtechAplication.repository.ProdutoRepository;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    // Injetamos o módulo de compras para criar a integração
    @Autowired
    private CompraService compraService;

    public void registrarSaidaDeEstoque(Integer idProduto, int quantidadeVendida) {
        // 1. Busca o produto direto do banco H2
        Produto produto = produtoRepository.findById(idProduto)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        // 2. Executa a baixa usando o método que VOCÊ já criou na classe Produto
        produto.removerQuantidade(quantidadeVendida);

        // 3. REGRA DE INTEGRAÇÃO: Se ficou crítico, aciona o outro serviço
        if (produto.verificaEstoque()) {
            compraService.solicitarReposicao(produto);
        }

        // 4. Salva a nova quantidade no banco
        produtoRepository.save(produto);
    }
}