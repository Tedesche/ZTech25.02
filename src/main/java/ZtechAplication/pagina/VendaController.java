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

    // --- VIEW METHODS ---

    @GetMapping(value = "/cadastrarForm")
    public ModelAndView form() {
        ModelAndView mv = new ModelAndView("cadastro_vendas");
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
            return "redirect:/vendas/cadastrarForm";
        }

        Produto produto = produtoRepository.findById(vendaDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + vendaDTO.getIdProduto()));

        Cliente cliente = clienteRepository.findById(vendaDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + vendaDTO.getIdCliente()));

        if (vendaDTO.getQuantidade() == null || vendaDTO.getQuantidade() <= 0) {
            attributes.addFlashAttribute("mensagem", "A quantidade deve ser maior que zero.");
            return "redirect:/vendas/cadastrarForm";
        }
        
        if (produto.getQuantidade() < vendaDTO.getQuantidade()) {
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque insuficiente para o produto: " + produto.getNome());
            return "redirect:/vendas/cadastrarForm";
        }

        Venda venda = new Venda();
        processarVenda(venda, vendaDTO, produto, cliente);

        produto.removerQuantidade(vendaDTO.getQuantidade());
        produtoRepository.save(produto);

        vendaRepository.save(venda);
        attributes.addFlashAttribute("mensagem", "Venda cadastrada com sucesso!");
        return "redirect:/vendas/listar";
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
        
        // Restaura estoque antigo
        produtoAntigo.adicionarQuantidade(quantidadeAntiga);
        
        int estoqueDisponivelParaNovoProduto = produtoNovo.getQuantidade();
        if(produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            estoqueDisponivelParaNovoProduto = produtoAntigo.getQuantidade();
        }

        if (estoqueDisponivelParaNovoProduto < vendaDTO.getQuantidade()) {
            produtoAntigo.removerQuantidade(quantidadeAntiga); // Desfaz a restauração
            attributes.addFlashAttribute("mensagem", "Quantidade em estoque insuficiente.");
            return "redirect:/vendas/editarForm/" + idVenda;
        }
        
        // Se trocou de produto, salva o antigo restaurado
        if (!produtoNovo.getIdProduto().equals(produtoAntigo.getIdProduto())) {
            produtoRepository.save(produtoAntigo); 
        }

        processarVenda(vendaExistente, vendaDTO, produtoNovo, cliente);

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
        Specification<Venda> spec = SpecificationController.comTermoVenda(termo);
        Page<Venda> paginaVendasEntidades = vendaRepository.findAll(spec, pageable);
        
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
        List<VendaDTO> listaDeDTOs = new ArrayList<>();
        for (Venda venda : vendas) {
            listaDeDTOs.add(converterParaDTO(venda));
        }
        return listaDeDTOs; 
    }

    // --- NOVOS MÉTODOS API (JSON) ------------------------------------------------------------

    @GetMapping("/api/venda/listar")
    @ResponseBody
    public ResponseEntity<Page<VendaDTO>> apiListarVendas(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Venda> paginaVendas = vendaRepository.findAll(pageable);
        Page<VendaDTO> paginaDTO = paginaVendas.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @PostMapping("/api/venda/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarVenda(@RequestBody VendaDTO vendaDTO) {
        try {
            Produto produto = produtoRepository.findById(vendaDTO.getIdProduto())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
            Cliente cliente = clienteRepository.findById(vendaDTO.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

            if (vendaDTO.getQuantidade() <= 0) {
                return ResponseEntity.badRequest().body("Quantidade deve ser positiva.");
            }

            Venda venda;
            if (vendaDTO.getIdVenda() != null) {
                // Edição: Restaurar estoque antigo primeiro
                venda = vendaRepository.findById(vendaDTO.getIdVenda()).orElse(new Venda());
                if (venda.getProduto() != null) {
                    venda.getProduto().adicionarQuantidade(venda.getQuantidade());
                    produtoRepository.save(venda.getProduto());
                }
                // Recarregar produto para ter estoque atualizado
                produto = produtoRepository.findById(vendaDTO.getIdProduto()).get();
            } else {
                venda = new Venda();
            }

            if (produto.getQuantidade() < vendaDTO.getQuantidade()) {
                return ResponseEntity.badRequest().body("Estoque insuficiente.");
            }

            processarVenda(venda, vendaDTO, produto, cliente);
            
            produto.removerQuantidade(vendaDTO.getQuantidade());
            produtoRepository.save(produto);
            
            Venda vendaSalva = vendaRepository.save(venda);
            return ResponseEntity.ok(converterParaDTO(vendaSalva));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/api/venda/{id}")
    @ResponseBody
    public ResponseEntity<VendaDTO> apiBuscarVenda(@PathVariable Integer id) {
        return vendaRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/venda/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarVenda(@PathVariable Integer id) {
        try {
            Venda venda = vendaRepository.findById(id).orElseThrow(() -> new Exception("Venda não encontrada"));
            // Restaura estoque
            Produto produto = venda.getProduto();
            if (produto != null) {
                produto.adicionarQuantidade(venda.getQuantidade());
                produtoRepository.save(produto);
            }
            vendaRepository.deleteById(id);
            return ResponseEntity.ok("Deletado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao deletar: " + e.getMessage());
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void processarVenda(Venda venda, VendaDTO vendaDTO, Produto produto, Cliente cliente) {
        venda.setDataInicio(stringToLocalDate(vendaDTO.getDataInicio(), "yyyy-MM-dd"));
        venda.setHoraInicio(stringToLocalTime(vendaDTO.getHoraInicio(), "HH:mm"));
        venda.setQuantidade(vendaDTO.getQuantidade());
        venda.setProduto(produto);
        venda.setCliente(cliente);
        venda.setValor(produto.getValor().multiply(new BigDecimal(vendaDTO.getQuantidade())));
        venda.setLucro((produto.getValor().subtract(produto.getCusto())).multiply(new BigDecimal(vendaDTO.getQuantidade())));
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
    public String teste() {
        return "correto";
    }
}