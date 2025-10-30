package ZtechAplication.pagina;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; 
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping; // Import necessário para @GetMapping

import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.FuncionarioRepository;
import ZtechAplication.repository.OrdemServicoRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.repository.VendaRepository;
import ZtechAplication.DTO.ClienteDTO;
import ZtechAplication.DTO.FuncionarioDTO;
import ZtechAplication.DTO.OrdemServicoDTO;
import ZtechAplication.DTO.ProdutoDTO;
import ZtechAplication.DTO.VendaDTO;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Funcionario;
import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Venda;

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
    private ClienteController clienteController; // Repositório para Produtos
    @Autowired
    private FuncionarioController funcionarioController; // Repositório para Produtos
    @Autowired
    private OrdemServicoController osController; // Repositório para Produtos
    @Autowired
    private ProdutoController produtoController; // Repositório para Produtos
    @Autowired
    private VendaController vendaController; // Repositório para Produtos
	

    @Autowired
    private ClienteRepository clienteRepository; // Repositório para Produtos
    @Autowired
    private FuncionarioRepository funcionarioRepository; // Repositório para Ordens de Serviço
    @Autowired
    private OrdemServicoRepository ordemServicoRepository; // Repositório para Ordens de Serviço
    @Autowired
    private ProdutoRepository produtoRepository; // Repositório para Clientes
    @Autowired
    private VendaRepository vendaRepository; // Repositório para Clientes
    

	@GetMapping("/") // Mapeamento para a rota raiz
	public String index() {
		return "login"; // Retorna a página de login/entrada inicial
	}

    // Mapeamento GET para a página de login (requisitado pelo Spring Security)
    @GetMapping("/inicio")
    public String login() {
        return "index"; // Retorna o template de login
    }
	
    // Método para carregar dados para a página inicial/dashboard
	@GetMapping("/inicio2.0")
	public String inicio(Model model) { 
		// 1. Buscar os dados específicos para cada tabela
	    // Usando queries otimizadas dos seus repositórios
	    List<Cliente> clientes = clienteRepository.findAll();
	    List<Funcionario> funcionarios = funcionarioRepository.findAll();
	    List<OrdemServico> ordemServicos = ordemServicoRepository.findAll();
	    List<Produto> produtos = produtoRepository.findAll();
	    List<Venda> vendas = vendaRepository.findAll();
	    
	 // 2. Converter para DTOs (se necessário)
	    List<ClienteDTO> clienteDTOs = clienteController.getClienteDTO(clientes);
	    List<FuncionarioDTO> funcionarioDTOs = funcionarioController.getFuncionarioDTO(funcionarios);
	    List<OrdemServicoDTO> osDTOs = osController.getOSDTO(ordemServicos);
	    List<ProdutoDTO> produtoDTOs = produtoController.getProdutoDTO(produtos);
	    List<VendaDTO> vendaDTOs = vendaController.getVendaDTO(vendas);
		
		
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