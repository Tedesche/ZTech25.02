package ZtechAplication.pagina;

import java.math.BigDecimal; 
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; 
import java.util.List; 
import java.util.Optional; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping; 
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.DTO.OrdemServicoDTO;
import ZtechAplication.DTO.ProdutoDTO;
import ZtechAplication.model.Categoria;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Marca;
import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Servico;
import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.OrdemServicoRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.repository.ServicoRepository;


@Controller
@RequestMapping(value = "/ordens")
public class OrdemServicoController {

	@Autowired
	private OrdemServicoRepository ordemServicoRepository; // Repositório para operações de CRUD em OrdemServico
    @Autowired
    private ProdutoRepository produtoRepository; // Repositório para operações de CRUD em Produto

    @Autowired
    private ServicoRepository servicoRepository; // Repositório para operações de CRUD em Servico

    @Autowired
    private ClienteRepository clienteRepository; // Repositório para operações de CRUD em Cliente
	
	// Método para exibir o formulário de cadastro de uma nova Ordem de Serviço
	@GetMapping(value = "/cadastrarForm")
	public ModelAndView form() {
        ModelAndView mv = new ModelAndView("cadastro_OS"); // Define o nome do arquivo HTML do formulário
        OrdemServicoDTO osDTO = new OrdemServicoDTO(); // Cria um novo DTO para o formulário
        // Define datas e horas atuais como padrão para novos cadastros
        osDTO.setDataInicio(localDateToString(LocalDate.now(), "yyyy-MM-dd")); // Formata a data atual para o input date
        osDTO.setHoraInicio(localTimeToString(LocalTime.now(), "HH:mm")); // Formata a hora atual para o input time

      // Nome do objeto alinhado com th:object="${ordemServico}" do template de cadastro
      mv.addObject("ordemServico", osDTO); 
      // Adiciona listas de produtos, serviços e clientes para preencher os selects no formulário
      mv.addObject("produtos", produtoRepository.findAllWithRelationships());
      mv.addObject("servicos", servicoRepository.findAll());
      mv.addObject("clientes", clienteRepository.findAllWithRelationships());
      return mv; // Retorna o ModelAndView com o formulário e os dados necessários
	}
	
	// Método para processar o cadastro de uma nova Ordem de Serviço
	@PostMapping(value = "/cadastrar")
    // Nome do @ModelAttribute alinhado com o th:object="${ordemServico}" do template de cadastro
	public String cadastrarOS(@Validated @ModelAttribute("ordemServico") OrdemServicoDTO osDTO, 
				  BindingResult result, // Resultado da validação do DTO
                  RedirectAttributes attributes, // Para adicionar mensagens flash após o redirecionamento
                  Model model) { 
		
		// Verifica se há erros de validação no DTO
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios."); // Mensagem de erro
            attributes.addFlashAttribute("ordemServico", osDTO); 
			return "redirect:/ordens/cadastrarForm"; // Redireciona de volta para o formulário de cadastro
		}
		
		// Busca as entidades relacionadas (Produto, Servico, Cliente) pelos IDs fornecidos no DTO
		Produto produto = produtoRepository.findById(osDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + osDTO.getIdProduto()));
		Servico servico = servicoRepository.findById(osDTO.getIdServico())
                .orElseThrow(() -> new IllegalArgumentException("Serviço inválido: " + osDTO.getIdServico()));
        Cliente cliente = clienteRepository.findById(osDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + osDTO.getIdCliente()));
		
        // Validação da quantidade
        if (osDTO.getQuantidade() == null || osDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            attributes.addFlashAttribute("ordemServico", osDTO); // Devolve o DTO
            return "redirect:/ordens/cadastrarForm"; // Redireciona para o formulário
        }
        // Validação de estoque do produto
        if (produto.getQuantidade() < osDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", 
            		"Quantidade em estoque ("+ produto.getQuantidade() +") insuficiente para o produto: " + produto.getNome());
            attributes.addFlashAttribute("ordemServico", osDTO); // Devolve o DTO
            return "redirect:/ordens/cadastrarForm"; // Redireciona para o formulário
        }
		
        // Cria uma nova entidade OrdemServico e popula com os dados do DTO e das entidades buscadas
        OrdemServico os = new OrdemServico();
        
        os.setDataInicio(stringToLocalDate(osDTO.getDataInicio(), "yyyy-MM-dd")); // Converte String para LocalDate
        os.setHoraInicio(stringToLocalTime(osDTO.getHoraInicio(), "HH:mm") ); // Converte String para LocalTime
        
        // Define data e hora de fim se informadas
        if (osDTO.getDataFim() != null && !osDTO.getDataFim().isEmpty()) {
            os.setDataFim(stringToLocalDate(osDTO.getDataFim(), "yyyy-MM-dd"));
        }
        if (osDTO.getHoraFim() != null && !osDTO.getHoraFim().isEmpty()) {
            os.setHoraFim(stringToLocalTime(osDTO.getHoraFim(), "HH:mm") );
        }

        os.setQuantidade(osDTO.getQuantidade());
        os.setStatus(StatusLibrary.getStatusDescricao(1)); // Define o status inicial como "Registrada"
        os.setProduto(produto);
        os.setServico(servico);
        os.setCliente(cliente);
        
        // Calcula o valor total e o lucro da Ordem de Serviço
        BigDecimal valorProdutoTotal = produto.getValor().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        BigDecimal custoProdutoTotal = produto.getCusto().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        os.setValor(servico.getValor().add(valorProdutoTotal));
        os.setLucro(servico.getValor().add(valorProdutoTotal.subtract(custoProdutoTotal)));
        
        // Atualiza a quantidade do produto no estoque
        produto.removerQuantidade(osDTO.getQuantidade());
        produtoRepository.save(produto);
        
        // Salva a nova Ordem de Serviço no banco de dados
        ordemServicoRepository.save(os);
        attributes.addFlashAttribute("mensagem", "Ordem de Serviço cadastrada com sucesso!"); // Mensagem de sucesso
        return "redirect:/ordens/listar"; // Redireciona para a lista de Ordens de Serviço
	}
	
	
	// Método para listar todas as Ordens de Serviço com paginação
	@GetMapping(value = "/listar")
	public String listarOS(@PageableDefault(size = 10) Pageable pageable, Model model) {
        // Busca todas as OS de forma paginada
		Page<OrdemServico> paginaDeOSsEntidades = ordemServicoRepository.findAll(pageable);
        // Converte a página de entidades para uma página de DTOs
        Page<OrdemServicoDTO> paginaDeOSDTOs = paginaDeOSsEntidades.map(this::converterParaDTO);
        
        model.addAttribute("paginaOrdens", paginaDeOSDTOs); // Adiciona a página de DTOs ao modelo
        // Garante que o atributo 'termo' exista no modelo, mesmo que nulo, para os links de paginação na busca
        if (!model.containsAttribute("termo")) { 
            model.addAttribute("termo", null);
        }
        return "ordens"; // Retorna o nome do arquivo HTML da lista de OS
	}
	
	// Método para buscar Ordens de Serviço com base em um termo de pesquisa, com paginação
	@GetMapping("/buscar")
    public String buscarOS(@RequestParam(value = "termo", required = false) String termo, 
                         @PageableDefault(size = 10) Pageable pageable,
                         Model model) {
        Page<OrdemServico> paginaDeOSsEntidades;
        
        // Cria uma Specification para a busca com base no termo
        Specification<OrdemServico> spec = SpecificationController.comTermoOS(termo);
        // Realiza a busca paginada usando a Specification
        paginaDeOSsEntidades = ordemServicoRepository.findAll(spec, pageable);
        
        // Adiciona mensagens informativas sobre o resultado da busca
        if (termo != null && !termo.isEmpty() && paginaDeOSsEntidades.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhuma OS encontrada para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaDeOSsEntidades.isEmpty()) {
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }

        // Converte as entidades encontradas para DTOs
        Page<OrdemServicoDTO> paginaDeOSDTOs = paginaDeOSsEntidades.map(this::converterParaDTO);
        model.addAttribute("paginaOrdens", paginaDeOSDTOs); // Adiciona a página de DTOs ao modelo
        model.addAttribute("termo", termo); // Adiciona o termo de busca ao modelo para repopular o campo de pesquisa
        return "ordens"; // Retorna para a mesma página de listagem, agora com os resultados da busca
    }
	
	// Método para exibir o formulário de edição de uma Ordem de Serviço existente
	@GetMapping(value = "/editarForm/{idOS}")
	public ModelAndView editarForm(@PathVariable("idOS") Integer idOS) { // PathVariable nomeado explicitamente
		// Busca a OS no banco de dados pelo ID
		OrdemServico os = ordemServicoRepository.findById(idOS)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));
        
        ModelAndView mv = new ModelAndView("alterarOS"); // Define o template HTML para edição
        // Nome do objeto alinhado com th:object="${ordemServico}" do template de alteração
        mv.addObject("ordemServico", converterParaDTO(os)); 
        
        String dataISOInicio = os.getDataInicio().format(DateTimeFormatter.ISO_DATE);
        mv.addObject("dataFormatada", dataISOInicio);
        String dataISOFim = os.getDataFim().format(DateTimeFormatter.ISO_DATE);
        mv.addObject("dataFormatada", dataISOFim);
        
        // Adiciona listas de produtos, serviços, clientes e status para os selects no formulário
        mv.addObject("produtos", produtoRepository.findAllWithRelationships());
        mv.addObject("servicos", servicoRepository.findAll());
        mv.addObject("clientes", clienteRepository.findAllWithRelationships());
        mv.addObject("listaStatusOS", StatusLibrary.getAllStatusDescriptions()); // Lista de todos os status disponíveis
        return mv; // Retorna o ModelAndView com o formulário e os dados
	}
	
	// Método para processar a edição de uma Ordem de Serviço existente
	@PostMapping(value = "/editar/{idOS}")
	public String editarOS(@PathVariable("idOS") Integer idOS, // PathVariable nomeado explicitamente
                         // Nome do @ModelAttribute alinhado com o th:object="${ordemServico}" do template de alteração
						 @Validated @ModelAttribute("ordemServico") OrdemServicoDTO osDTO, 
						 BindingResult result, // Resultado da validação
						 RedirectAttributes attributes, // Para mensagens flash
                         Model model) { 

		// Verifica erros de validação no DTO
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("ordemServico", osDTO); // Devolve o DTO com erros e dados preenchidos
			return "redirect:/ordens/editarForm/" + idOS; // Redireciona de volta ao formulário de edição
		}

		// Busca a Ordem de Serviço existente no banco
		OrdemServico osExistente = ordemServicoRepository.findById(idOS)
				.orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

		// Guarda referências ao produto e quantidade antigos para ajuste de estoque
		Produto produtoAntigo = osExistente.getProduto();
		int quantidadeAntiga = osExistente.getQuantidade();

		// Busca as novas entidades relacionadas (Produto, Servico, Cliente)
		Produto produtoNovo = produtoRepository.findById(osDTO.getIdProduto())
				.orElseThrow(() -> new IllegalArgumentException("Produto novo inválido: " + osDTO.getIdProduto()));
		Servico servicoNovo = servicoRepository.findById(osDTO.getIdServico())
                .orElseThrow(() -> new IllegalArgumentException("Serviço novo inválido: " + osDTO.getIdServico()));
		Cliente clienteNovo = clienteRepository.findById(osDTO.getIdCliente())
				.orElseThrow(() -> new IllegalArgumentException("Cliente novo inválido: " + osDTO.getIdCliente()));
        
        // Validação da nova quantidade
        if (osDTO.getQuantidade() == null || osDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            attributes.addFlashAttribute("ordemServico", osDTO);
            return "redirect:/ordens/editarForm/" + idOS;
        }

		// Lógica de ajuste de estoque:
		// 1. Restaura a quantidade do produto antigo no estoque.
        if (produtoAntigo != null) { // Verifica se havia um produto antigo
		    produtoAntigo.adicionarQuantidade(quantidadeAntiga);
        }
        
        // 2. Calcula o estoque disponível para o novo produto (ou o mesmo produto, se não mudou).
        int estoqueDisponivelParaNovoProduto;
        if(produtoNovo.getIdProduto().equals(produtoAntigo != null ? produtoAntigo.getIdProduto() : null)) { // Lida com produtoAntigo nulo
            estoqueDisponivelParaNovoProduto = produtoAntigo.getQuantidade();
        } else {
            estoqueDisponivelParaNovoProduto = produtoNovo.getQuantidade();
        }

        // Verifica se há estoque suficiente para o novo produto/quantidade
		if (estoqueDisponivelParaNovoProduto < osDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque (" + estoqueDisponivelParaNovoProduto + ") insuficiente para o produto: " + produtoNovo.getNome());
            if (produtoAntigo != null) {
			    produtoAntigo.removerQuantidade(quantidadeAntiga);
            }
            attributes.addFlashAttribute("ordemServico", osDTO);
			return "redirect:/ordens/editarForm/" + idOS;
		}
        
        if (produtoAntigo != null && !produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            produtoRepository.save(produtoAntigo);
        }

		// Atualiza os dados da OS existente com os novos valores
		osExistente.setDataInicio(stringToLocalDate(osDTO.getDataInicio(), "yyyy-MM-dd"));
		osExistente.setHoraInicio(stringToLocalTime(osDTO.getHoraInicio(), "HH:mm"));
		
        if (osDTO.getDataFim() != null && !osDTO.getDataFim().isEmpty()) {
            osExistente.setDataFim(stringToLocalDate(osDTO.getDataFim(), "yyyy-MM-dd"));
        } else {
            osExistente.setDataFim(null); 
        }
        if (osDTO.getHoraFim() != null && !osDTO.getHoraFim().isEmpty()) {
            osExistente.setHoraFim(stringToLocalTime(osDTO.getHoraFim(), "HH:mm"));
        } else {
            osExistente.setHoraFim(null); 
        }
        
		osExistente.setQuantidade(osDTO.getQuantidade());
        
        if (osDTO.getStatusOS() != null && !osDTO.getStatusOS().isEmpty()) {
            boolean statusValido = StatusLibrary.getAllStatusDescriptions().stream()
                                    .anyMatch(status -> status.equalsIgnoreCase(osDTO.getStatusOS()));
            if (statusValido) {
                 osExistente.setStatus(osDTO.getStatusOS());
            } 
        }

		osExistente.setProduto(produtoNovo);
        osExistente.setServico(servicoNovo);
		osExistente.setCliente(clienteNovo);
		
        BigDecimal valorProdutoTotal = produtoNovo.getValor().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        BigDecimal custoProdutoTotal = produtoNovo.getCusto().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        osExistente.setValor(servicoNovo.getValor().add(valorProdutoTotal));
        osExistente.setLucro(servicoNovo.getValor().add(valorProdutoTotal.subtract(custoProdutoTotal)));

		produtoNovo.removerQuantidade(osDTO.getQuantidade());
		produtoRepository.save(produtoNovo);
		
		ordemServicoRepository.save(osExistente);
		attributes.addFlashAttribute("mensagem", "Ordem de Serviço atualizada com sucesso!");
		return "redirect:/ordens/listar"; 
	}
	
	// Método para deletar uma Ordem de Serviço
	@GetMapping(value = "/deletar/{idOS}") 
	public String deletarOS(@PathVariable("idOS") Integer idOS, RedirectAttributes attributes) { // PathVariable nomeado explicitamente
		// Busca a OS no banco de dados
		OrdemServico os = ordemServicoRepository.findById(idOS)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

        Produto produto = os.getProduto();
        if (produto != null) { 
            produto.adicionarQuantidade(os.getQuantidade());
            produtoRepository.save(produto);
        }

        ordemServicoRepository.delete(os);
        attributes.addFlashAttribute("mensagem", "Ordem de Serviço removida com sucesso e estoque restaurado (se necessário)!");
        return "redirect:/ordens/listar"; 
	}
	
	@GetMapping(value = "/atualizarStatus/{idOS}") 
	public String atualizarStatusOS(@PathVariable("idOS") Integer idOS, RedirectAttributes attributes) { // PathVariable nomeado explicitamente
		// Busca a OS no banco de dados
		OrdemServico osExistente = ordemServicoRepository.findById(idOS)
				.orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

//		usando a classe de StatusLibrary pegamos a proxima posição do opssivel status
		String proxStatus = StatusLibrary.getProximaDescricao(osExistente.getStatus());
		
		if (proxStatus == "Concluido") {
//			atualiza as datas e hoarios para o momento que salvou
			osExistente.setDataFim(LocalDate.now());
			osExistente.setHoraFim(LocalTime.now());
			osExistente.setStatus(proxStatus);
			ordemServicoRepository.save(osExistente);
	        attributes.addFlashAttribute("mensagem", "Status de Ordem de Serviço atualizada com sucesso - " + proxStatus);
	        return "redirect:/ordens/listar"; 
		}
		if (proxStatus == "Cancelado") {
//			atualiza as datas e hoarios para o momento que salvou
			osExistente.setDataFim(LocalDate.now());
			osExistente.setHoraFim(LocalTime.now());
			osExistente.setStatus(proxStatus);
//			por ser um cancelamento, atualizamos o estoque também
			Produto produto = osExistente.getProduto(); 
	        if (produto != null) { 
            produto.adicionarQuantidade(osExistente.getQuantidade());
            produtoRepository.save(produto);
	        }
			ordemServicoRepository.save(osExistente);
	        attributes.addFlashAttribute("mensagem", "Status de Ordem de Serviço atualizada com sucesso - " + proxStatus);
	        return "redirect:/ordens/listar"; 
		}
		
		osExistente.setStatus(proxStatus);		
        ordemServicoRepository.save(osExistente);
        attributes.addFlashAttribute("mensagem", "Status de Ordem de Serviço atualizada com sucesso - " + proxStatus);
        return "redirect:/ordens/listar"; 
	}
	
	
	// Método auxiliar para converter a entidade OrdemServico para OrdemServicoDTO
	private OrdemServicoDTO converterParaDTO(OrdemServico os) {
		OrdemServicoDTO dto = new OrdemServicoDTO();
        dto.setIdOS(os.getIdOS());
        dto.setDataInicio(os.getDataInicio() != null ? localDateToString(os.getDataInicio(), "dd/MM/yyyy") : "");
        dto.setHoraInicio(os.getHoraInicio() != null ? localTimeToString(os.getHoraInicio(), "HH:mm") : "");
        dto.setDataFim(os.getDataFim() != null ? localDateToString(os.getDataFim(), "dd/MM/yyyy") : "");
        dto.setHoraFim(os.getHoraFim() != null ? localTimeToString(os.getHoraFim(), "HH:mm") : "");
        dto.setValor(os.getValor());
        dto.setLucro(os.getLucro());
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
	
    // --- MÉTODOS UTILITÁRIOS PARA CONVERSÃO DE DATA E HORA ---

	public static LocalDate stringToLocalDate(String dataString, String formato) {
        if (dataString == null || dataString.trim().isEmpty()) {
            return null; 
        }
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return LocalDate.parse(dataString, formatter); 
	}

	public static String localDateToString(LocalDate data, String formato) {
        if (data == null) {
            return ""; 
        }
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return data.format(formatter); 
	}
	
	public static LocalTime stringToLocalTime(String timeString, String formato) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null; 
        }
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return LocalTime.parse(timeString, formatter); 
	}

	public static String localTimeToString(LocalTime timeLocal, String formato) {
        if (timeLocal == null) {
            return ""; 
        }
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return timeLocal.format(formatter); 
	}
	
	@GetMapping(value = "/teste") 
	public String teste (){
		return "correto"; 
	}
	
}