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
import org.springframework.web.bind.annotation.GetMapping;

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
import java.util.List;

// --- IMPORTAÇÕES PARA O 2FA ---
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import ZtechAplication.service.OtpService;
import ZtechAplication.repository.UsuarioRepository;
import ZtechAplication.model.Usuario;

@Controller
public class indexController {

    @Autowired private ClienteController clienteController;
    @Autowired private FuncionarioController funcionarioController;
    // @Autowired private OrdemServicoController osController; // Não é mais necessário para conversão
    @Autowired private ProdutoController produtoController;
    // @Autowired private VendaController vendaController; // Não é mais necessário para conversão

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private OrdemServicoRepository ordemServicoRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private VendaRepository vendaRepository;
    
    @Autowired private OtpService otpService;
    @Autowired private UsuarioRepository usuarioRepository;

    @GetMapping("/")
    public String login() {
        return "login";
    }
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/inicio")
    public String inicio(Model model,
            @RequestParam(value = "pageEstoque", defaultValue = "0") int pageEstoque,
            @RequestParam(value = "pageOS", defaultValue = "0") int pageOS,
            @RequestParam(value = "pageVendas", defaultValue = "0") int pageVendas,
            @RequestParam(value = "pageFuncionarios", defaultValue = "0") int pageFuncionarios,
            @RequestParam(value = "pageClientes", defaultValue = "0") int pageClientes
        ) {
        
        try {
            int pageSize = 10; 

            Pageable pageableCliente = PageRequest.of(pageClientes, pageSize);
            Pageable pageableFuncionario = PageRequest.of(pageFuncionarios, pageSize);
            Pageable pageableProduto = PageRequest.of(pageEstoque, pageSize);
            Pageable pageableOS = PageRequest.of(pageOS, pageSize, Sort.by("dataInicio").descending());
            Pageable pageableVenda = PageRequest.of(pageVendas, pageSize, Sort.by("dataInicio").descending());

            // 1. Buscar os dados (Entidades)
            Page<Cliente> paginaClientes = clienteRepository.findAll(pageableCliente);
            Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(pageableFuncionario);
            Page<OrdemServico> paginaOrdemServicos = ordemServicoRepository.findAll(pageableOS);
            Page<Produto> paginaProdutos = produtoRepository.findAll(pageableProduto);
            Page<Venda> paginaVendas = vendaRepository.findAll(pageableVenda);
            
            // 2. Converter para DTOs
            // Clientes
            List<ClienteDTO> clienteDTOs = clienteController.getClienteDTO(paginaClientes.getContent());
            Page<ClienteDTO> paginaDTOClientes = new PageImpl<>(clienteDTOs, pageableCliente, paginaClientes.getTotalElements());

            // Funcionários
            List<FuncionarioDTO> funcionarioDTOs = funcionarioController.getFuncionarioDTO(paginaFuncionarios.getContent());
            Page<FuncionarioDTO> paginaDTOFuncionarios = new PageImpl<>(funcionarioDTOs, pageableFuncionario, paginaFuncionarios.getTotalElements());

            // Produtos
            List<ProdutoDTO> produtoDTOs = produtoController.getProdutoDTO(paginaProdutos.getContent());
            Page<ProdutoDTO> paginaDTOProdutos = new PageImpl<>(produtoDTOs, pageableProduto, paginaProdutos.getTotalElements());
            
            // --- CORREÇÃO AQUI: Conversão direta de O.S. e Vendas ---
            
            // Ordem de Serviço (Mapeamento direto usando método auxiliar)
            Page<OrdemServicoDTO> paginaDTOOS = paginaOrdemServicos.map(this::converterOSParaDTO);

            // Vendas (Mapeamento direto usando método auxiliar)
            Page<VendaDTO> paginaDTOVendas = paginaVendas.map(this::converterVendaParaDTO);

            // 3. Adicionar as PÁGINAS ao Model (CORRIGIDO: Descomentado)
            model.addAttribute("paginaDeClientes", paginaDTOClientes);
            model.addAttribute("paginaDeFuncionarios", paginaDTOFuncionarios);
            model.addAttribute("paginaDeOrdensServico", paginaDTOOS); // Agora não é nulo
            model.addAttribute("paginaDeProdutos", paginaDTOProdutos);
            model.addAttribute("paginaDeVendas", paginaDTOVendas);     // Agora não é nulo
            
            // Valores dos cards
            long totalProdutos = produtoRepository.count();
            model.addAttribute("totalProdutos", totalProdutos);
            
            // --- CÁLCULO DE DATAS PARA O MÊS ATUAL ---
            LocalDate hoje = LocalDate.now();
            YearMonth mesAtual = YearMonth.from(hoje);
            LocalDate inicioMes = mesAtual.atDay(1);
            LocalDate fimMes = mesAtual.atEndOfMonth();
            
            // Contagem usando as novas queries com datas
            long totalOS = ordemServicoRepository.countByDataInicioBetween(inicioMes, fimMes);
            long totalVendas = vendaRepository.countByDataInicioBetween(inicioMes, fimMes);
            
            model.addAttribute("totalVendas", totalVendas);
            model.addAttribute("totalOS", totalOS);

            // Calculo do Lucro Total do Mês
            String statusConcluido = StatusLibrary.getStatusDescricao(3); // "Concluido"
            
            BigDecimal lucroOSMes = ordemServicoRepository.sumLucroConcluidoByDataFimBetween(
                    statusConcluido, inicioMes, fimMes);
            
            BigDecimal lucroOSs = (lucroOSMes != null) ? lucroOSMes : BigDecimal.ZERO;
            model.addAttribute("lucroOSs", lucroOSs);
            
            BigDecimal lucroVendasMes = vendaRepository.sumLucroByDataInicioBetween(inicioMes, fimMes);
                
            BigDecimal lucroVendas = (lucroVendasMes != null) ? lucroVendasMes : BigDecimal.ZERO;
            model.addAttribute("lucroVendas", lucroVendas);
            
            return "index";

        } catch (Exception e) {
            e.printStackTrace();
            return "error"; 
        }
    }

    // --- MÉTODOS AUXILIARES PARA CONVERSÃO (Para evitar erros se não existirem nos outros controllers) ---

    private OrdemServicoDTO converterOSParaDTO(OrdemServico os) {
        OrdemServicoDTO dto = new OrdemServicoDTO();
        dto.setIdOS(os.getIdOS());
        dto.setDataInicio(os.getDataInicio() != null ? os.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        dto.setHoraInicio(os.getHoraInicio() != null ? os.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        dto.setDataFim(os.getDataFim() != null ? os.getDataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        dto.setValor(os.getValor());
        dto.setStatusOS(os.getStatus());
        dto.setQuantidade(os.getQuantidade());

        if (os.getProduto() != null) {
            dto.setIdProduto(os.getProduto().getIdProduto());
            dto.setNomeProduto(os.getProduto().getNome());
        }
        if (os.getServico() != null) {
            dto.setIdServico(os.getServico().getIdServico());
            dto.setNomeServico(os.getServico().getNome());
        }
        if (os.getCliente() != null) {
            dto.setIdCliente(os.getCliente().getIdCliente());
            dto.setNomeCliente(os.getCliente().getNomeCliente());
        }
        return dto;
    }

    private VendaDTO converterVendaParaDTO(Venda venda) {
        VendaDTO dto = new VendaDTO();
        dto.setIdVenda(venda.getIdVenda());
        dto.setDataInicio(venda.getDataInicio() != null ? venda.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        dto.setHoraInicio(venda.getHoraInicio() != null ? venda.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        dto.setValor(venda.getValor());
        dto.setLucro(venda.getLucro());
        dto.setQuantidade(venda.getQuantidade());

        if (venda.getProduto() != null) {
            dto.setIdProduto(venda.getProduto().getIdProduto());
            dto.setNomeProduto(venda.getProduto().getNome());
        }
        if (venda.getCliente() != null) {
            dto.setIdCliente(venda.getCliente().getIdCliente());
            dto.setNomeCliente(venda.getCliente().getNomeCliente());
        }
        return dto;
    }

    // ... Mapeamentos restantes mantidos iguais ...
    @RequestMapping("/clientes")
    public String clientes() { return "redirect:/cliente/listar"; }
    @RequestMapping("/estoque")
    public String estoque() { return "redirect:/produto/listar"; }
    @RequestMapping("/vendas")
    public String vendas() { return "redirect:/vendas/listar"; }
    @RequestMapping("/ordens")
    public String ordens() { return "redirect:/ordens/listar"; }
    @RequestMapping("/cadastro_cliente")
    public String cadastro_cliente() { return "redirect:/cliente/cadastrarForm"; }
    @RequestMapping("/cadastro_produto")
    public String cadastro_produto() { return "redirect:/produto/cadastrarForm"; }
    @RequestMapping("/cadastro_OS")
    public String cadastro_OS() { return "redirect:/ordens/cadastrarForm"; }

    // --- MÉTODOS DE 2FA ---
    @GetMapping("/login-verificacao")
    public String exibirPaginaVerificacao(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PRE_AUTH"))) {
            return "redirect:/login";
        }
        model.addAttribute("username", auth.getName());
        return "login-verificacao";
    }

    @PostMapping("/verificar-otp")
    public String verificarOtp(@RequestParam String otp, HttpServletRequest request) {
        Authentication preAuth = SecurityContextHolder.getContext().getAuthentication();
        if (preAuth == null || !preAuth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PRE_AUTH"))) {
            return "redirect:/login?error=true";
        }

        String username = preAuth.getName();

        if (otpService.validateOtp(username, otp)) {
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));
            
            Authentication authFinal = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authFinal);
            request.getSession(true);

            return "redirect:/inicio";
        } else {
            return "redirect:/login-verificacao?error=true";
        }
    }
}