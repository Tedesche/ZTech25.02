package ZtechAplication.pagina;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
	public String login() {
		return "login"; // Retorna a página de login/entrada inicial
	}

	
    // Método para carregar dados para a página inicial/dashboard
	@GetMapping("/inicio")
	public String inicio(Model model) { 
		// Crie um "pedido de página" que limita a 10 itens
        // PageRequest.of(int page, int size)
        // Página 0 = a primeira página
        // Tamanho 10 = o limite que você queria
        Pageable limiteDe10 = PageRequest.of(0, 10);
        
        // Bônus: Para Vendas e OS, podemos pegar as 10 *mais recentes*
        Pageable top10Recentes = PageRequest.of(0, 10, Sort.by("dataInicio").descending());


		// 1. Buscar os dados (APENAS 10 DE CADA - RÁPIDO)
        // Usamos .getContent() para pegar a List<T> de dentro da Página
	    List<Cliente> clientes = clienteRepository.findAll(limiteDe10).getContent();
	    List<Funcionario> funcionarios = funcionarioRepository.findAll(limiteDe10).getContent();
	    List<OrdemServico> ordemServicos = ordemServicoRepository.findAll(top10Recentes).getContent();
	    List<Produto> produtos = produtoRepository.findAll(limiteDe10).getContent();
	    List<Venda> vendas = vendaRepository.findAll(top10Recentes).getContent();
	    
        // -------------------------------------------------------------------
        // (O RESTO DO SEU CÓDIGO FICA IDÊNTICO)

	    // 2. Converter para DTOs (agora só converte 10 de cada)
	    List<ClienteDTO> clienteDTOs = clienteController.getClienteDTO(clientes);
	    List<FuncionarioDTO> funcionarioDTOs = funcionarioController.getFuncionarioDTO(funcionarios);
	    List<OrdemServicoDTO> osDTOs = osController.getOSDTO(ordemServicos);
	    List<ProdutoDTO> produtoDTOs = produtoController.getProdutoDTO(produtos);
	    List<VendaDTO> vendaDTOs = vendaController.getVendaDTO(vendas);
		
	    // 3. Adicionar ao Model
	    model.addAttribute("listaDeClientes", clienteDTOs);
	    model.addAttribute("listaDeFuncionarios", funcionarioDTOs);
	    model.addAttribute("listaDeOrdensServico", osDTOs);
	    model.addAttribute("listaDeProdutos", produtoDTOs);
	    model.addAttribute("listaDeVendas", vendaDTOs);
	 
	    // Valores dos cards
	    	// 1. Total de produtos cadastrados
        long totalProdutos = produtoRepository.count();
        model.addAttribute("totalProdutos", totalProdutos);
        
        	// 2. Numero de Vendas e O.S.s do mes e seu ganho 
        // Busca todas as OS com seus relacionamentos
        List<OrdemServico> todasAsOS = ordemServicoRepository.findAllWithRelationships();
        List<Venda> todasAsVendas = vendaRepository.findAllWithRelationships();
        
        int ano = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();
        mes = 6;
        
        // contagem de vendas e OSs do mes
        long totalOS = ordemServicoRepository.countByYearAndMonth(ano, mes);
        long totalVendas = vendaRepository.countByYearAndMonth(ano, mes);
        long totalPedidos = totalOS + totalVendas;
        // Adiciona o numero total de OS e vendas do mes ao modelo
        model.addAttribute("totalPedidosMes", totalPedidos);

     // --- 2. Calculo EFICIENTE do Lucro Total do Mes ---
        String statusConcluido = StatusLibrary.getStatusDescricao(3); // "Concluido"

        // Pede ao banco para SOMAR o lucro, já filtrando por status e data
        BigDecimal lucroOSMes = ordemServicoRepository.sumLucroConcluidoByYearAndMonth(
            statusConcluido, ano, mes);
        // Pede ao banco para SOMAR o lucro, já filtrando por data
        BigDecimal lucroVendasMes = vendaRepository.sumLucroByYearAndMonth( 
        	ano, mes);

        // IMPORTANTE: SUM pode retornar null se nao houver registros.
        // Devemos tratar isso antes de somar.
        BigDecimal lucroTotalMes = BigDecimal.ZERO;
        if (lucroOSMes != null) {
            lucroTotalMes = lucroTotalMes.add(lucroOSMes);
        }
        if (lucroVendasMes != null) {
            lucroTotalMes = lucroTotalMes.add(lucroVendasMes);
        }
        // Adiciona o ganho total de OS e vendas do mês ao modelo
        model.addAttribute("lucroTotalMes", lucroTotalMes);
        
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