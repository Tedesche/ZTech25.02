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

import ZtechAplication.DTO.ClienteDTO;
import ZtechAplication.DTO.VendaDTO;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Venda;
import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.repository.VendaRepository;

@Controller
@RequestMapping(value = "/vendas")
public class VendaController {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping(value = "/cadastrarForm")
    public ModelAndView form() {
        ModelAndView mv = new ModelAndView("cadastro_vendas"); // Usando o nome do seu arquivo
        VendaDTO vendaDTO = new VendaDTO();
        vendaDTO.setDataInicio(localDateToString(LocalDate.now(), "yyyy-MM-dd"));
        vendaDTO.setHoraInicio(localTimeToString(LocalTime.now(), "HH:mm"));
        mv.addObject("venda", vendaDTO);
        mv.addObject("produtos", produtoRepository.findAllWithRelationships());
        mv.addObject("clientes", clienteRepository.findAllWithRelationships());
        return mv;
    }

    @PostMapping(value = "/cadastrar")
    public String cadastrarVenda(@Validated @ModelAttribute("venda") VendaDTO vendaDTO, BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("venda", vendaDTO); 
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/cadastrarForm"; // URL Correta
        }

        Produto produto = produtoRepository.findById(vendaDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + vendaDTO.getIdProduto()));

        Cliente cliente = clienteRepository.findById(vendaDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + vendaDTO.getIdCliente()));

        if (vendaDTO.getQuantidade() == null || vendaDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            attributes.addFlashAttribute("venda", vendaDTO);
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/cadastrarForm"; // URL Correta
        }
        
        if (produto.getQuantidade() < vendaDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque insuficiente para o produto: " + produto.getNome());
            attributes.addFlashAttribute("venda", vendaDTO);
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/cadastrarForm"; // URL Correta
        }

        Venda venda = new Venda();
        venda.setDataInicio(stringToLocalDate(vendaDTO.getDataInicio(), "yyyy-MM-dd"));
        venda.setHoraInicio(stringToLocalTime(vendaDTO.getHoraInicio(), "HH:mm"));
        venda.setQuantidade(vendaDTO.getQuantidade());
        venda.setProduto(produto);
        venda.setCliente(cliente);
        venda.setValor(produto.getValor().multiply(new BigDecimal(vendaDTO.getQuantidade())));
        venda.setLucro((produto.getValor().subtract(produto.getCusto())).multiply(new BigDecimal(vendaDTO.getQuantidade())));

        produto.removerQuantidade(vendaDTO.getQuantidade());
        produtoRepository.save(produto);

        vendaRepository.save(venda);
        attributes.addFlashAttribute("mensagem", "Venda cadastrada com sucesso!");
        return "redirect:/vendas/listar"; // URL Correta
    }

    @GetMapping(value = "/listar")
    public String listarVendas(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<Venda> paginaDeVendasEntidades = vendaRepository.findAll(pageable);
        Page<VendaDTO> paginaDeVendaDTOs = paginaDeVendasEntidades.map(this::converterParaDTO);
        
        model.addAttribute("paginaVendas", paginaDeVendaDTOs);
        if (!model.containsAttribute("termo")) { 
            model.addAttribute("termo", null);
        }
        return "vendas";
    }

    @GetMapping(value = "/editarForm/{idVenda}")
    public ModelAndView editarForm(@PathVariable("idVenda") Integer idVenda) {
        Venda venda = vendaRepository.findById(idVenda)
                .orElseThrow(() -> new IllegalArgumentException("Venda inválida: " + idVenda));
        
        ModelAndView mv = new ModelAndView("alterarVenda");
        mv.addObject("venda", converterParaDTO(venda));
        
        String dataISOInicio = venda.getDataInicio().format(DateTimeFormatter.ISO_DATE);
        mv.addObject("dataFormatada", dataISOInicio);
        
        mv.addObject("produtos", produtoRepository.findAllWithRelationships());
        mv.addObject("clientes", clienteRepository.findAllWithRelationships());
        return mv;
    }

    @PostMapping(value = "/editar/{idVenda}")
    public String editarVenda(@PathVariable("idVenda") Integer idVenda, @Validated @ModelAttribute("venda") VendaDTO vendaDTO, BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("venda", vendaDTO); 
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/editarForm/" + idVenda;
        }

        Venda vendaExistente = vendaRepository.findById(idVenda)
                .orElseThrow(() -> new IllegalArgumentException("Venda inválida: " + idVenda));

        Produto produtoAntigo = vendaExistente.getProduto();
        int quantidadeAntiga = vendaExistente.getQuantidade();

        Produto produtoNovo = produtoRepository.findById(vendaDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto novo inválido: " + vendaDTO.getIdProduto()));
        
        Cliente cliente = clienteRepository.findById(vendaDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + vendaDTO.getIdCliente()));
        
        if (vendaDTO.getQuantidade() == null || vendaDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            attributes.addFlashAttribute("venda", vendaDTO);
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/editarForm/" + idVenda;
        }

        produtoAntigo.adicionarQuantidade(quantidadeAntiga);
        
        int estoqueDisponivelParaNovoProduto = produtoNovo.getQuantidade();
        if(produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            estoqueDisponivelParaNovoProduto = produtoAntigo.getQuantidade();
        }

        if (estoqueDisponivelParaNovoProduto < vendaDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque (" + estoqueDisponivelParaNovoProduto + ") insuficiente para o produto: " + produtoNovo.getNome());
            attributes.addFlashAttribute("venda", vendaDTO);
            produtoAntigo.removerQuantidade(quantidadeAntiga); 
            attributes.addFlashAttribute("produtos", produtoRepository.findAllWithRelationships());
            attributes.addFlashAttribute("clientes", clienteRepository.findAllWithRelationships());
            return "redirect:/vendas/editarForm/" + idVenda;
        }
        
        if (!produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            produtoRepository.save(produtoAntigo); 
        }

        vendaExistente.setDataInicio(stringToLocalDate(vendaDTO.getDataInicio(), "yyyy-MM-dd"));
        vendaExistente.setHoraInicio(stringToLocalTime(vendaDTO.getHoraInicio(), "HH:mm"));
        vendaExistente.setQuantidade(vendaDTO.getQuantidade());
        vendaExistente.setProduto(produtoNovo);
        vendaExistente.setCliente(cliente);
        vendaExistente.setValor(produtoNovo.getValor().multiply(new BigDecimal(vendaDTO.getQuantidade())));
        vendaExistente.setLucro((produtoNovo.getValor().subtract(produtoNovo.getCusto())).multiply(new BigDecimal(vendaDTO.getQuantidade())));

        produtoNovo.removerQuantidade(vendaDTO.getQuantidade());
        produtoRepository.save(produtoNovo);
        
        vendaRepository.save(vendaExistente);
        attributes.addFlashAttribute("mensagem", "Venda atualizada com sucesso!");
        return "redirect:/vendas/listar";
    }

    @GetMapping(value = "/deletar/{idVenda}")
    public String deletarVenda(@PathVariable("idVenda") Integer idVenda, RedirectAttributes attributes) {
        Venda venda = vendaRepository.findById(idVenda)
                .orElseThrow(() -> new IllegalArgumentException("Venda inválida: " + idVenda));

        Produto produto = venda.getProduto();
        produto.adicionarQuantidade(venda.getQuantidade());
        produtoRepository.save(produto);

        vendaRepository.delete(venda);
        attributes.addFlashAttribute("mensagem", "Venda removida com sucesso e estoque restaurado!");
        return "redirect:/vendas/listar";
    }
    
    @GetMapping("/buscar")
    public String buscar(@RequestParam(value = "termo", required = false) String termo,
                         @PageableDefault(size = 10) Pageable pageable,
                         Model model) {
        Page<Venda> paginaVendasEntidades;
        
        Specification<Venda> spec = SpecificationController.comTermoVenda(termo);
        paginaVendasEntidades = vendaRepository.findAll(spec, pageable);
        
        if (termo != null && !termo.isEmpty() && paginaVendasEntidades.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhuma venda encontrada para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaVendasEntidades.isEmpty()) {
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }

        Page<VendaDTO> paginaVendaDTOs = paginaVendasEntidades.map(this::converterParaDTO);
        model.addAttribute("paginaVendas", paginaVendaDTOs);
        model.addAttribute("termo", termo);
        return "vendas";
    }

    public List<VendaDTO> getVendaDTO(List<Venda> vendas) {
		//cria a lista que vai receber a conversão
		List<VendaDTO> listaDeDTOs = new ArrayList<>();
		//passa um for para popula uma list com os clienets passados
		for (Venda venda : vendas) {
			listaDeDTOs.add(converterParaDTO(venda));
		}
		return listaDeDTOs; 
	}
    
    private VendaDTO converterParaDTO(Venda venda) {
        VendaDTO dto = new VendaDTO();
        dto.setIdVenda(venda.getIdVenda());
        dto.setDataInicio(localDateToString(venda.getDataInicio(), "dd/MM/yyyy"));
        dto.setHoraInicio(localTimeToString(venda.getHoraInicio(), "HH:mm"));
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
    public String teste() {
        return "correto";
    }
}