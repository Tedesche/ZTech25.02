package ZtechAplication.pagina;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

// --- IMPORTAÇÕES ADICIONADAS PARA O 2FA ---
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import ZtechAplication.service.OtpService;
import ZtechAplication.repository.UsuarioRepository;
import ZtechAplication.model.Usuario;
// --- FIM DAS IMPORTAÇÕES ---


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
	
	// --- DEPENDÊNCIAS ADICIONADAS PARA O 2FA ---
	@Autowired
	private OtpService otpService;

	@Autowired
	private UsuarioRepository usuarioRepository; // Necessário para buscar o usuário final
	// --- FIM DAS DEPENDÊNCIAS ---
	

	@GetMapping("/") // Mapeamento para a rota raiz
	public String login() {
		return "login"; // Retorna a página de login/entrada inicial
	}

	
	// Método para carregar dados para a página inicial/dashboard
		@GetMapping("/inicio")
		public String inicio(Model model,
				// --- MUDANÇA AQUI: Recebendo os números das páginas da URL ---
				@RequestParam(value = "pageEstoque", defaultValue = "0") int pageEstoque,
				@RequestParam(value = "pageOS", defaultValue = "0") int pageOS,
				@RequestParam(value = "pageVendas", defaultValue = "0") int pageVendas,
				@RequestParam(value = "pageFuncionarios", defaultValue = "0") int pageFuncionarios,
				@RequestParam(value = "pageClientes", defaultValue = "0") int pageClientes
			) {
			
			// --- MUDANÇA AQUI: Definindo o tamanho da página em um só lugar ---
			int pageSize = 10; // 10 itens por página

			// Crie os "pedidos de página" usando as variáveis que recebemos
			Pageable pageableCliente = PageRequest.of(pageClientes, pageSize);
			Pageable pageableFuncionario = PageRequest.of(pageFuncionarios, pageSize);
			Pageable pageableProduto = PageRequest.of(pageEstoque, pageSize);
			
			// Mantemos a ordenação para Vendas e OS
			Pageable pageableOS = PageRequest.of(pageOS, pageSize, Sort.by("dataInicio").descending());
			Pageable pageableVenda = PageRequest.of(pageVendas, pageSize, Sort.by("dataInicio").descending());


			// 1. Buscar os dados como OBJETOS PAGE (e não mais List)
			Page<Cliente> paginaClientes = clienteRepository.findAll(pageableCliente);
			Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(pageableFuncionario);
			Page<OrdemServico> paginaOrdemServicos = ordemServicoRepository.findAll(pageableOS);
			Page<Produto> paginaProdutos = produtoRepository.findAll(pageableProduto);
			Page<Venda> paginaVendas = vendaRepository.findAll(pageableVenda);
			
			// -------------------------------------------------------------------

			// 2. Converter para DTOs (Preservando a Paginação)
			//	 Nós convertemos o *conteúdo* da página para DTOs
			//	 E criamos uma *nova Página* de DTOs
			
			List<ClienteDTO> clienteDTOs = clienteController.getClienteDTO(paginaClientes.getContent());
			Page<ClienteDTO> paginaDTOClientes = new PageImpl<>(clienteDTOs, pageableCliente, paginaClientes.getTotalElements());

			List<FuncionarioDTO> funcionarioDTOs = funcionarioController.getFuncionarioDTO(paginaFuncionarios.getContent());
			Page<FuncionarioDTO> paginaDTOFuncionarios = new PageImpl<>(funcionarioDTOs, pageableFuncionario, paginaFuncionarios.getTotalElements());

			List<OrdemServicoDTO> osDTOs = osController.getOSDTO(paginaOrdemServicos.getContent());
			Page<OrdemServicoDTO> paginaDTOOS = new PageImpl<>(osDTOs, pageableOS, paginaOrdemServicos.getTotalElements());

			List<ProdutoDTO> produtoDTOs = produtoController.getProdutoDTO(paginaProdutos.getContent());
			Page<ProdutoDTO> paginaDTOProdutos = new PageImpl<>(produtoDTOs, pageableProduto, paginaProdutos.getTotalElements());
			
			List<VendaDTO> vendaDTOs = vendaController.getVendaDTO(paginaVendas.getContent());
			Page<VendaDTO> paginaDTOVendas = new PageImpl<>(vendaDTOs, pageableVenda, paginaVendas.getTotalElements());

			
			// 3. Adicionar as PÁGINAS (Page) ao Model, não as Listas (List)
			// --- MUDANÇA AQUI: Renomeando os atributos para refletir que são Páginas ---
			model.addAttribute("paginaDeClientes", paginaDTOClientes);
			model.addAttribute("paginaDeFuncionarios", paginaDTOFuncionarios);
			model.addAttribute("paginaDeOrdensServico", paginaDTOOS);
			model.addAttribute("paginaDeProdutos", paginaDTOProdutos);
			model.addAttribute("paginaDeVendas", paginaDTOVendas);
			
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
		// Adiciona o numero total de OS e vendas do mes ao modelo
		model.addAttribute("totalVendas", totalVendas);
		model.addAttribute("totalOS", totalOS);

	// --- 2. Calculo EFICIENTE do Lucro Total do Mes ---
		String statusConcluido = StatusLibrary.getStatusDescricao(3); // "Concluido"
		// Pede ao banco para SOMAR o lucro, já filtrando por status e data
		BigDecimal lucroOSMes = ordemServicoRepository.sumLucroConcluidoByYearAndMonth(
				statusConcluido, ano, mes);
		// Devemos tratar isso antes de somar.
		BigDecimal lucroOSs = BigDecimal.ZERO;
		if (lucroOSMes != null) {
			lucroOSs = lucroOSs.add(lucroOSMes);
		}
		// Adiciona o ganho total de OS do mês ao modelo
		model.addAttribute("lucroOSs", lucroOSs);
		
		// Pede ao banco para SOMAR o lucro, já filtrando por data
		BigDecimal lucroVendasMes = vendaRepository.sumLucroByYearAndMonth( 
			ano, mes);
		// Devemos tratar isso antes de somar.
			BigDecimal lucroVendas = BigDecimal.ZERO;
		if (lucroVendasMes != null) {
			lucroVendas = lucroVendas.add(lucroVendasMes);
		}
		// Adiciona o ganho total de vendas do mês ao modelo
		model.addAttribute("lucroVendas", lucroVendas);
		
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

	// --- MÉTODOS ADICIONADOS PARA O 2FA ---

	/**
	 * GET: Mostra a página para o usuário digitar o OTP.
	 */
	@GetMapping("/login-verificacao")
	public String exibirPaginaVerificacao(Model model) {
		// Pega o usuário "pré-autenticado"
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		// Se não tiver ninguém pré-autenticado, ou se a role não for a temporária, manda para o login
		if (auth == null || !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PRE_AUTH"))) {
			return "redirect:/login";
		}
		
		model.addAttribute("username", auth.getName());
		return "login-verificacao"; // O nome do nosso novo arquivo HTML (login-verificacao.html)
	}


	/**
	 * POST: Valida o OTP que o usuário digitou.
	 */
	@PostMapping("/verificar-otp")
    public String verificarOtp(@RequestParam String otp, HttpServletRequest request) {
        
        // 1. Pega o usuário "pré-autenticado"
        Authentication preAuth = SecurityContextHolder.getContext().getAuthentication();
        if (preAuth == null || !preAuth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PRE_AUTH"))) {
            return "redirect:/login?error=true"; // Sessão expirou ou inválida
        }

        String username = preAuth.getName();

        // 2. Valida o OTP
        if (otpService.validateOtp(username, otp)) {
            // DEU CERTO!
            
            // 3. Carrega o usuário real do banco para obter as ROLES corretas
            // --- CORREÇÃO AQUI ---
            // Usando "findByUsername" e tratando o Optional
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado após verificação OTP: " + username));
            
            // 4. Cria a autenticação FINAL (com as roles de verdade)
            Authentication authFinal = new UsernamePasswordAuthenticationToken(
                    usuario, 
                    null, 
                    usuario.getAuthorities() // As permissões REAIS do usuário
            );
            
            // 5. Seta a autenticação final e limpa a sessão
            SecurityContextHolder.getContext().setAuthentication(authFinal);
            request.getSession(true); // Cria uma nova sessão limpa

            return "redirect:/inicio"; // Manda para a página principal

        } else {
            // DEU ERRADO!
            // Manda de volta para a página de verificação com mensagem de erro
            return "redirect:/login-verificacao?error=true";
        }
	}
}
    