package ZtechAplication.pagina;

import java.math.BigDecimal; 
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; 
import java.util.List; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping; 
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.DTO.OrdemServicoDTO;
import ZtechAplication.model.Cliente;
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
	private OrdemServicoRepository ordemServicoRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private ServicoRepository servicoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
	
    // --- VIEW METHODS ---

	@GetMapping(value = "/cadastrarForm")
	public ModelAndView form() {
        ModelAndView mv = new ModelAndView("cadastro_OS"); 
        OrdemServicoDTO osDTO = new OrdemServicoDTO(); 
        osDTO.setDataInicio(localDateToString(LocalDate.now(), "yyyy-MM-dd")); 
        osDTO.setHoraInicio(localTimeToString(LocalTime.now(), "HH:mm")); 

        mv.addObject("ordemServico", osDTO); 
        mv.addObject("produtos", produtoRepository.findAllWithRelationships());
        mv.addObject("servicos", servicoRepository.findAll());
        mv.addObject("clientes", clienteRepository.findAllWithRelationships());
        return mv; 
	}
	
	@PostMapping(value = "/cadastrar")
	public String cadastrarOS(@Validated @ModelAttribute("ordemServico") OrdemServicoDTO osDTO, 
				  BindingResult result, 
                  RedirectAttributes attributes, 
                  Model model) { 
		
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios."); 
            attributes.addFlashAttribute("ordemServico", osDTO); 
			return "redirect:/ordens/cadastrarForm"; 
		}
		
		Produto produto = produtoRepository.findById(osDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + osDTO.getIdProduto()));
		Servico servico = servicoRepository.findById(osDTO.getIdServico())
                .orElseThrow(() -> new IllegalArgumentException("Serviço inválido: " + osDTO.getIdServico()));
        Cliente cliente = clienteRepository.findById(osDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + osDTO.getIdCliente()));
		
        if (osDTO.getQuantidade() == null || osDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            attributes.addFlashAttribute("ordemServico", osDTO);
            return "redirect:/ordens/cadastrarForm";
        }
        if (produto.getQuantidade() < osDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade insuficiente em estoque.");
            attributes.addFlashAttribute("ordemServico", osDTO); 
            return "redirect:/ordens/cadastrarForm"; 
        }
		
        OrdemServico os = new OrdemServico();
        processarOS(os, osDTO, produto, servico, cliente);
        os.setStatus(StatusLibrary.getStatusDescricao(1)); 
        
        produto.removerQuantidade(osDTO.getQuantidade());
        produtoRepository.save(produto);
        
        ordemServicoRepository.save(os);
        attributes.addFlashAttribute("mensagem", "Ordem de Serviço cadastrada com sucesso!"); 
        return "redirect:/ordens/listar"; 
	}
	
	@GetMapping(value = "/listar")
	public String listarOS(@PageableDefault(size = 10) Pageable pageable, Model model) {
		Page<OrdemServico> paginaDeOSsEntidades = ordemServicoRepository.findAll(pageable);
        Page<OrdemServicoDTO> paginaDeOSDTOs = paginaDeOSsEntidades.map(this::converterParaDTO);
        
        model.addAttribute("paginaOrdens", paginaDeOSDTOs); 
        if (!model.containsAttribute("termo")) { 
            model.addAttribute("termo", null);
        }
        return "ordens"; 
	}
	
	@GetMapping("/buscar")
    public String buscarOS(@RequestParam(value = "termo", required = false) String termo, 
                         @PageableDefault(size = 10) Pageable pageable,
                         Model model) {
        Specification<OrdemServico> spec = SpecificationController.comTermoOS(termo);
        Page<OrdemServico> paginaDeOSsEntidades = ordemServicoRepository.findAll(spec, pageable);
        
        if (termo != null && !termo.isEmpty() && paginaDeOSsEntidades.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhuma OS encontrada para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaDeOSsEntidades.isEmpty()) {
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }

        Page<OrdemServicoDTO> paginaDeOSDTOs = paginaDeOSsEntidades.map(this::converterParaDTO);
        model.addAttribute("paginaOrdens", paginaDeOSDTOs); 
        model.addAttribute("termo", termo); 
        return "ordens"; 
    }
	
	@GetMapping(value = "/editarForm/{idOS}")
	public ModelAndView editarForm(@PathVariable("idOS") Integer idOS) { 
		OrdemServico os = ordemServicoRepository.findById(idOS)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));
        
        ModelAndView mv = new ModelAndView("alterarOS"); 
        mv.addObject("ordemServico", converterParaDTO(os)); 
        
        String dataISOInicio = os.getDataInicio().format(DateTimeFormatter.ISO_DATE);
        mv.addObject("dataFormatada", dataISOInicio);
        
        mv.addObject("produtos", produtoRepository.findAllWithRelationships());
        mv.addObject("servicos", servicoRepository.findAll());
        mv.addObject("clientes", clienteRepository.findAllWithRelationships());
        mv.addObject("listaStatusOS", StatusLibrary.getAllStatusDescriptions()); 
        return mv; 
	}
	
	@PostMapping(value = "/editar/{idOS}")
	public String editarOS(@PathVariable("idOS") Integer idOS, 
						 @Validated @ModelAttribute("ordemServico") OrdemServicoDTO osDTO, 
						 BindingResult result, 
						 RedirectAttributes attributes, 
                         Model model) { 

		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("ordemServico", osDTO); 
			return "redirect:/ordens/editarForm/" + idOS; 
		}

		OrdemServico osExistente = ordemServicoRepository.findById(idOS)
				.orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

		Produto produtoAntigo = osExistente.getProduto();
		int quantidadeAntiga = osExistente.getQuantidade();

		Produto produtoNovo = produtoRepository.findById(osDTO.getIdProduto())
				.orElseThrow(() -> new IllegalArgumentException("Produto novo inválido: " + osDTO.getIdProduto()));
		Servico servicoNovo = servicoRepository.findById(osDTO.getIdServico())
                .orElseThrow(() -> new IllegalArgumentException("Serviço novo inválido: " + osDTO.getIdServico()));
		Cliente clienteNovo = clienteRepository.findById(osDTO.getIdCliente())
				.orElseThrow(() -> new IllegalArgumentException("Cliente novo inválido: " + osDTO.getIdCliente()));
        
        // Restaura estoque
        if (produtoAntigo != null) { 
		    produtoAntigo.adicionarQuantidade(quantidadeAntiga);
        }
        
        int estoqueDisponivelParaNovoProduto;
        if(produtoNovo.getIdProduto().equals(produtoAntigo != null ? produtoAntigo.getIdProduto() : null)) {
            estoqueDisponivelParaNovoProduto = produtoAntigo.getQuantidade();
        } else {
            estoqueDisponivelParaNovoProduto = produtoNovo.getQuantidade();
        }

		if (estoqueDisponivelParaNovoProduto < osDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque insuficiente.");
            if (produtoAntigo != null) {
			    produtoAntigo.removerQuantidade(quantidadeAntiga);
            }
            attributes.addFlashAttribute("ordemServico", osDTO);
			return "redirect:/ordens/editarForm/" + idOS;
		}
        
        if (produtoAntigo != null && !produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            produtoRepository.save(produtoAntigo);
        }

        processarOS(osExistente, osDTO, produtoNovo, servicoNovo, clienteNovo);
        // Mantém status se não vier no DTO ou atualiza se vier
        if (osDTO.getStatusOS() != null && !osDTO.getStatusOS().isEmpty()) {
             osExistente.setStatus(osDTO.getStatusOS());
        }

		produtoNovo.removerQuantidade(osDTO.getQuantidade());
		produtoRepository.save(produtoNovo);
		
		ordemServicoRepository.save(osExistente);
		attributes.addFlashAttribute("mensagem", "Ordem de Serviço atualizada com sucesso!");
		return "redirect:/ordens/listar"; 
	}
	
	@GetMapping(value = "/deletar/{idOS}") 
	public String deletarOS(@PathVariable("idOS") Integer idOS, RedirectAttributes attributes) { 
		OrdemServico os = ordemServicoRepository.findById(idOS)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

        Produto produto = os.getProduto();
        if (produto != null) { 
            produto.adicionarQuantidade(os.getQuantidade());
            produtoRepository.save(produto);
        }

        ordemServicoRepository.delete(os);
        attributes.addFlashAttribute("mensagem", "Ordem de Serviço removida com sucesso!");
        return "redirect:/ordens/listar"; 
	}
	
	@GetMapping(value = "/atualizarStatus/{idOS}") 
	public String atualizarStatusOS(@PathVariable("idOS") Integer idOS, RedirectAttributes attributes) {
		OrdemServico osExistente = ordemServicoRepository.findById(idOS)
				.orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço inválida: " + idOS));

		String proxStatus = StatusLibrary.getProximaDescricao(osExistente.getStatus());
		
		if (proxStatus.equals("Concluido")) {
			osExistente.setDataFim(LocalDate.now());
			osExistente.setHoraFim(LocalTime.now());
			osExistente.setStatus(proxStatus);
			ordemServicoRepository.save(osExistente);
	        attributes.addFlashAttribute("mensagem", "Status atualizado: " + proxStatus);
	        return "redirect:/ordens/listar"; 
		}
		if (proxStatus.equals("Cancelado")) {
			osExistente.setDataFim(LocalDate.now());
			osExistente.setHoraFim(LocalTime.now());
			osExistente.setStatus(proxStatus);
			Produto produto = osExistente.getProduto(); 
	        if (produto != null) { 
                produto.adicionarQuantidade(osExistente.getQuantidade());
                produtoRepository.save(produto);
	        }
			ordemServicoRepository.save(osExistente);
	        attributes.addFlashAttribute("mensagem", "Status atualizado: " + proxStatus);
	        return "redirect:/ordens/listar"; 
		}
		
		osExistente.setStatus(proxStatus);		
        ordemServicoRepository.save(osExistente);
        attributes.addFlashAttribute("mensagem", "Status atualizado: " + proxStatus);
        return "redirect:/ordens/listar"; 
	}
	
	public List<OrdemServicoDTO> getOSDTO(List<OrdemServico> oss) {
		List<OrdemServicoDTO> listaDeDTOs = new ArrayList<>();
		for (OrdemServico os : oss ) {
			listaDeDTOs.add(converterParaDTO(os));
		}
		return listaDeDTOs; 
	}

    // --- NOVOS MÉTODOS API (JSON) ------------------------------------------------------------

    @GetMapping("/api/ordem/listar")
    @ResponseBody
    public ResponseEntity<Page<OrdemServicoDTO>> apiListarOrdens(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<OrdemServico> paginaOrdens = ordemServicoRepository.findAll(pageable);
        Page<OrdemServicoDTO> paginaDTO = paginaOrdens.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @PostMapping("/api/ordem/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarOrdem(@RequestBody OrdemServicoDTO osDTO) {
        try {
            Produto produto = produtoRepository.findById(osDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
            Servico servico = servicoRepository.findById(osDTO.getIdServico())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado"));
            Cliente cliente = clienteRepository.findById(osDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

            OrdemServico os;
            if (osDTO.getIdOS() != null) {
                // Edição: Restaurar estoque antigo
                os = ordemServicoRepository.findById(osDTO.getIdOS()).orElse(new OrdemServico());
                if (os.getProduto() != null) {
                    os.getProduto().adicionarQuantidade(os.getQuantidade());
                    produtoRepository.save(os.getProduto());
                }
                // Recarregar produto para estoque atual
                produto = produtoRepository.findById(osDTO.getIdProduto()).get();
            } else {
                os = new OrdemServico();
                os.setStatus(StatusLibrary.getStatusDescricao(1));
            }

            if (produto.getQuantidade() < osDTO.getQuantidade()) {
                return ResponseEntity.badRequest().body("Estoque insuficiente.");
            }

            processarOS(os, osDTO, produto, servico, cliente);
            if(osDTO.getStatusOS() != null) {
                os.setStatus(osDTO.getStatusOS());
            }

            produto.removerQuantidade(osDTO.getQuantidade());
            produtoRepository.save(produto);
            
            OrdemServico osSalva = ordemServicoRepository.save(os);
            return ResponseEntity.ok(converterParaDTO(osSalva));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/api/ordem/{id}")
    @ResponseBody
    public ResponseEntity<OrdemServicoDTO> apiBuscarOrdem(@PathVariable Integer id) {
        return ordemServicoRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/ordem/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarOrdem(@PathVariable Integer id) {
        try {
            OrdemServico os = ordemServicoRepository.findById(id).orElseThrow(() -> new Exception("OS não encontrada"));
            // Restaura estoque
            Produto produto = os.getProduto();
            if (produto != null) {
                produto.adicionarQuantidade(os.getQuantidade());
                produtoRepository.save(produto);
            }
            ordemServicoRepository.deleteById(id);
            return ResponseEntity.ok("Deletado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao deletar: " + e.getMessage());
        }
    }
	
	// --- MÉTODOS AUXILIARES ---

    private void processarOS(OrdemServico os, OrdemServicoDTO osDTO, Produto produto, Servico servico, Cliente cliente) {
        os.setDataInicio(stringToLocalDate(osDTO.getDataInicio(), "yyyy-MM-dd"));
        os.setHoraInicio(stringToLocalTime(osDTO.getHoraInicio(), "HH:mm"));
        if(osDTO.getDataFim() != null) os.setDataFim(stringToLocalDate(osDTO.getDataFim(), "yyyy-MM-dd"));
        if(osDTO.getHoraFim() != null) os.setHoraFim(stringToLocalTime(osDTO.getHoraFim(), "HH:mm"));
        
        os.setQuantidade(osDTO.getQuantidade());
        os.setProduto(produto);
        os.setServico(servico);
        os.setCliente(cliente);
        
        BigDecimal valorProdutoTotal = produto.getValor().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        BigDecimal custoProdutoTotal = produto.getCusto().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        os.setValor(servico.getValor().add(valorProdutoTotal));
        os.setLucro(servico.getValor().add(valorProdutoTotal.subtract(custoProdutoTotal)));
    }

	private OrdemServicoDTO converterParaDTO(OrdemServico os) {
		OrdemServicoDTO dto = new OrdemServicoDTO();
        dto.setIdOS(os.getIdOS());
        dto.setDataInicio(localDateToString(os.getDataInicio(), "dd/MM/yyyy"));
        dto.setHoraInicio(localTimeToString(os.getHoraInicio(), "HH:mm"));
        dto.setDataFim(localDateToString(os.getDataFim(), "dd/MM/yyyy"));
        dto.setHoraFim(localTimeToString(os.getHoraFim(), "HH:mm"));
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
	
	public static LocalDate stringToLocalDate(String dataString, String formato) {
        if (dataString == null || dataString.trim().isEmpty()) return null; 
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return LocalDate.parse(dataString, formatter); 
	}

	public static String localDateToString(LocalDate data, String formato) {
        if (data == null) return ""; 
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return data.format(formatter); 
	}
	
	public static LocalTime stringToLocalTime(String timeString, String formato) {
        if (timeString == null || timeString.trim().isEmpty()) return null; 
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return LocalTime.parse(timeString, formatter); 
	}

	public static String localTimeToString(LocalTime timeLocal, String formato) {
        if (timeLocal == null) return ""; 
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
	    return timeLocal.format(formatter); 
	}
	
	@GetMapping(value = "/teste") 
	public String teste (){
		return "correto"; 
	}
}