package ZtechAplication.service;

import org.springframework.stereotype.Service;
import ZtechAplication.model.Produto;

@Service
public class CompraService {
    
    public void solicitarReposicao(Produto produto) {
        // No futuro, este método pode gravar na tabela de pedidos de compra.
        // Para a arquitetura atual, a simples chamada do método valida a integração.
    }
}