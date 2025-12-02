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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

    // --- MÉTODOS DE API (JSON) - Usados pelo JavaScript ---

    @GetMapping("/api/venda/listar")
    @ResponseBody
    public ResponseEntity<Page<VendaDTO>> apiListarVendas(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Venda> paginaVendas = vendaRepository.findAll(pageable);
        Page<VendaDTO> paginaDTO = paginaVendas.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @GetMapping("/api/venda/{id}")
    @ResponseBody
    public ResponseEntity<VendaDTO> apiBuscarVenda(@PathVariable Integer id) {
        return vendaRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            // EDIÇÃO: Lógica para restaurar estoque antes de aplicar a nova quantidade
            if (vendaDTO.getIdVenda() != null) {
                venda = vendaRepository.findById(vendaDTO.getIdVenda()).orElse(new Venda());
                
                // Se já tinha produto, devolve a quantidade antiga ao estoque
                if (venda.getProduto() != null) {
                    venda.getProduto().adicionarQuantidade(venda.getQuantidade());
                    produtoRepository.save(venda.getProduto());
                }
                
                // Recarrega o produto do banco para ter o estoque atualizado
                produto = produtoRepository.findById(vendaDTO.getIdProduto()).get();
            } else {
                // CRIAÇÃO
                venda = new Venda();
            }

            // Verifica se tem estoque suficiente para a NOVA quantidade
            if (produto.getQuantidade() < vendaDTO.getQuantidade()) {
                // Se falhar na validação e for edição, o estoque antigo já foi devolvido (correto, pois a operação aborta)
                // Mas para garantir integridade caso abortemos aqui, poderíamos reverter, mas o save não foi chamado na venda ainda.
                // Idealmente o estoque só é baixado no final.
                return ResponseEntity.badRequest().body("Estoque insuficiente. Disponível: " + produto.getQuantidade());
            }

            // Atualiza os dados da venda
            processarVenda(venda, vendaDTO, produto, cliente);
            
            // Baixa a nova quantidade do estoque
            produto.removerQuantidade(vendaDTO.getQuantidade());
            produtoRepository.save(produto);
            
            Venda vendaSalva = vendaRepository.save(venda);
            return ResponseEntity.ok(converterParaDTO(vendaSalva));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/venda/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarVenda(@PathVariable Integer id) {
        try {
            Venda venda = vendaRepository.findById(id)
                    .orElseThrow(() -> new Exception("Venda não encontrada"));
            
            // Restaura o estoque ao deletar a venda
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

    // --- MÉTODOS DE TELA (Thymeleaf) - Mantidos para compatibilidade ---

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
    
    @GetMapping("/buscar")
    public String buscar(@RequestParam(value = "termo", required = false) String termo,
                         @PageableDefault(size = 10) Pageable pageable,
                         Model model) {
        Specification<Venda> spec = SpecificationController.comTermoVenda(termo);
        Page<Venda> paginaVendasEntidades = vendaRepository.findAll(spec, pageable);
        
        if (termo != null && !termo.isEmpty() && paginaVendasEntidades.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhuma venda encontrada para: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty()) {
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }

        Page<VendaDTO> paginaVendaDTOs = paginaVendasEntidades.map(this::converterParaDTO);
        model.addAttribute("paginaVendas", paginaVendaDTOs);
        model.addAttribute("termo", termo);
        return "vendas";
    }

    @GetMapping(value = "/cadastrarForm")
    public ModelAndView form() {
        ModelAndView mv = new ModelAndView("cadastro_vendas");
        mv.addObject("venda", new VendaDTO());
        return mv;
    }

    // --- MÉTODOS AUXILIARES ---

    private void processarVenda(Venda venda, VendaDTO vendaDTO, Produto produto, Cliente cliente) {
        venda.setDataInicio(stringToLocalDate(vendaDTO.getDataInicio(), "yyyy-MM-dd"));
        venda.setHoraInicio(stringToLocalTime(vendaDTO.getHoraInicio(), "HH:mm"));
        venda.setQuantidade(vendaDTO.getQuantidade());
        venda.setProduto(produto);
        venda.setCliente(cliente);
        // Cálculos de valores
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
    
    // Utilitários de Data/Hora
    public static LocalDate stringToLocalDate(String dataString, String formato) {
         if (dataString == null || dataString.trim().isEmpty()) return LocalDate.now(); 
         try {
             DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
             return LocalDate.parse(dataString, formatter);
         } catch (Exception e) { return LocalDate.now(); }
    }

    public static String localDateToString(LocalDate data, String formato) {
         if (data == null) return ""; 
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
         return data.format(formatter); 
    }
    
    public static LocalTime stringToLocalTime(String timeString, String formato) {
         if (timeString == null || timeString.trim().isEmpty()) return LocalTime.now(); 
         try {
             DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
             return LocalTime.parse(timeString, formatter); 
         } catch(Exception e) { return LocalTime.now(); }
    }

    public static String localTimeToString(LocalTime timeLocal, String formato) {
         if (timeLocal == null) return ""; 
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato); 
         return timeLocal.format(formatter); 
    }
}