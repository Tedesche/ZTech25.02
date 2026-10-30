package model;

import org.junit.jupiter.api.Test;

import ZtechAplication.model.Produto;

import static org.junit.jupiter.api.Assertions.*;

public class ProdutoTest {

    @Test
    public void deveRetornarTrueParaEstoqueCritico() {
        Produto produto = new Produto();
        // Usando o seu método existente para o estoque atual
        produto.setQuantidade(5); 
        // Usando o novo método para o limite mínimo
        produto.setQuantidadeMinima(10); 

        // Executa a checagem
        boolean resultado = produto.verificaEstoque();

        // O esperado é TRUE, pois 5 é menor que 10
        assertTrue(resultado, "O sistema deve alertar (TRUE) quando a quantidade for menor ou igual à mínima.");
    }

    @Test
    public void deveRetornarFalseParaEstoqueAcimaDaMargem() {
        Produto produto = new Produto();
        produto.setQuantidade(15);
        produto.setQuantidadeMinima(10);

        boolean resultado = produto.verificaEstoque();

        // O esperado é FALSE, pois 15 está acima do limite de 10
        assertFalse(resultado, "O sistema não deve alertar (FALSE) quando houver folga na quantidade em estoque.");
    }
}