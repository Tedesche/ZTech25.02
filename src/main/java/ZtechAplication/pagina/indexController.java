package ZtechAplication.pagina;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; 
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping; // Import necessário para @GetMapping

import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.OrdemServicoRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.model.OrdemServico; 

import java.math.BigDecimal; 
// Removido: import java.text.NumberFormat; pois a formatação será no Thymeleaf
import java.time.LocalDate; 
import java.time.YearMonth; 
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList; 
import java.util.List;
// Removido: import java.util.Locale; 
import java.util.Map;
import java.util.stream.Collectors;
// Removido: import java.util.Comparator; // Se não estiver sendo usado para ordenação explícita aqui
import java.util.LinkedHashMap; 

@Controller
public class indexController {

    @Autowired
    private ProdutoRepository produtoRepository; // Repositório para Clientes

    @Autowired
    private ClienteRepository clienteRepository; // Repositório para Produtos

    @Autowired
    private OrdemServicoRepository ordemServicoRepository; // Repositório para Ordens de Serviço

	@GetMapping("/") // Mapeamento para a rota raiz
	public String index() {
		return "login"; // Retorna a página de login/entrada inicial
	}

    // Mapeamento GET para a página de login (requisitado pelo Spring Security)
    @GetMapping("/login")
    public String login() {
        return "login"; // Retorna o template de login
    }
	
    // Método para carregar dados para a página inicial/dashboard
	@RequestMapping("/inicio")
	public String inicio(Model model) { 
        
		return "index"; // Retorna o nome do template da página inicial (CORRIGIDO)
	}

    // Mapeamentos para as outras páginas (redirecionando para os controllers específicos)
	@RequestMapping("/clientes")
	public String clientes() {
        return "redirect:/cliente/listar"; 
	}
	@RequestMapping("/estoque")
	public String estoque() {
        return "redirect:/produto/listar";
	}
	@RequestMapping("/vendas")
	public String vendas() {
        return "redirect:/vendas/listar";
	}
	@RequestMapping("/ordens")
	public String ordens() {
        return "redirect:/ordens/listar";
	}
	@RequestMapping("/cadastro_cliente")
	public String cadastro_cliente() {
        return "redirect:/cliente/cadastrarForm";
	}
	@RequestMapping("/cadastro_produto")
	public String cadastro_produto() {
        return "redirect:/produto/cadastrarForm";
	}
	@RequestMapping("/cadastro_OS")
	public String cadastro_OS() {
        return "redirect:/ordens/cadastrarForm";
	}
}