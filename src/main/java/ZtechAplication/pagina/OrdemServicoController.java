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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    
    // --- MÉTODOS DE API (JSON) ---

    @GetMapping("/api/ordem/listar")
    @ResponseBody
    public ResponseEntity<Page<OrdemServicoDTO>> apiListarOrdens(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<OrdemServico> paginaOrdens = ordemServicoRepository.findAll(pageable);
        Page<OrdemServicoDTO> paginaDTO = paginaOrdens.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @GetMapping("/api/ordem/{id}")
    @ResponseBody
    public ResponseEntity<OrdemServicoDTO> apiBuscarOrdem(@PathVariable Integer id) {
        return ordemServicoRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            
            // EDIÇÃO: Restaura estoque antigo antes de aplicar a nova quantidade
            if (osDTO.getIdOS() != null) {
                os = ordemServicoRepository.findById(osDTO.getIdOS()).orElse(new OrdemServico());
                if (os.getProduto() != null) {
                    os.getProduto().adicionarQuantidade(os.getQuantidade());
                    produtoRepository.save(os.getProduto());
                }
                // Recarrega produto atualizado do banco
                produto = produtoRepository.findById(osDTO.getIdProduto()).get();
            } else {
                // CRIAÇÃO
                os = new OrdemServico();
                // Define data de início apenas na criação se não vier preenchida
                if (osDTO.getDataInicio() == null || osDTO.getDataInicio().isEmpty()) {
                    os.setDataInicio(LocalDate.now());
                    os.setHoraInicio(LocalTime.now());
                }
                
                if(osDTO.getStatusOS() == null || osDTO.getStatusOS().isEmpty()) {
                    os.setStatus("Registrada");
                }
            }

            // Valida estoque para a nova quantidade
            if (produto.getQuantidade() < osDTO.getQuantidade()) {
                return ResponseEntity.badRequest().body("Estoque insuficiente. Disponível: " + produto.getQuantidade());
            }

            // Atualiza dados da entidade (Preço, ids, etc)
            processarOS(os, osDTO, produto, servico, cliente);
            
            // --- CORREÇÃO DE STATUS E DATA FIM ---
            if(osDTO.getStatusOS() != null && !osDTO.getStatusOS().isEmpty()) {
                os.setStatus(osDTO.getStatusOS());

                // Se o status for CONCLUIDA, define a Data Fim
                if ("CONCLUIDA".equals(osDTO.getStatusOS())) {
                    // Só define se ainda não tiver data fim, ou atualiza sempre (opção atual: atualiza sempre)
                    os.setDataFim(LocalDate.now());
                    os.setHoraFim(LocalTime.now());
                } else {
                    // Se reabriu a OS (ex: mudou de Concluida para Em Andamento), limpa a data fim
                    os.setDataFim(null);
                    os.setHoraFim(null);
                }
            }

            // Baixa estoque do produto
            produto.removerQuantidade(osDTO.getQuantidade());
            produtoRepository.save(produto);
            
            OrdemServico osSalva = ordemServicoRepository.save(os);
            return ResponseEntity.ok(converterParaDTO(osSalva));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/ordem/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarOrdem(@PathVariable Integer id) {
        try {
            OrdemServico os = ordemServicoRepository.findById(id)
                    .orElseThrow(() -> new Exception("OS não encontrada"));
            
            // Restaura estoque ao deletar
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
        // CORREÇÃO: Só altera a DataInicio se o DTO trouxe uma data nova explícita
        // Se for edição e o campo vier vazio, mantém a data que já estava no banco
        if (osDTO.getDataInicio() != null && !osDTO.getDataInicio().isEmpty()) {
            os.setDataInicio(stringToLocalDate(osDTO.getDataInicio(), "yyyy-MM-dd"));
        }
        if (osDTO.getHoraInicio() != null && !osDTO.getHoraInicio().isEmpty()) {
            os.setHoraInicio(stringToLocalTime(osDTO.getHoraInicio(), "HH:mm"));
        }
        
        os.setQuantidade(osDTO.getQuantidade());
        os.setProduto(produto);
        os.setServico(servico);
        os.setCliente(cliente);
        
        BigDecimal valorProdutoTotal = produto.getValor().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        BigDecimal custoProdutoTotal = produto.getCusto().multiply(BigDecimal.valueOf(osDTO.getQuantidade()));
        
        // Valor total = Serviço + Produtos
        os.setValor(servico.getValor().add(valorProdutoTotal));
        // Lucro = Serviço + (ValorProduto - CustoProduto)
        os.setLucro(servico.getValor().add(valorProdutoTotal.subtract(custoProdutoTotal)));
    }

    private OrdemServicoDTO converterParaDTO(OrdemServico os) {
        OrdemServicoDTO dto = new OrdemServicoDTO();
        dto.setIdOS(os.getIdOS());
        dto.setDataInicio(localDateToString(os.getDataInicio(), "dd/MM/yyyy"));
        dto.setHoraInicio(localTimeToString(os.getHoraInicio(), "HH:mm"));
        // Adiciona Data Fim ao DTO para aparecer na tabela
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
    
    // Utilitários de Data
    public static LocalDate stringToLocalDate(String dataString, String formato) {
        if (dataString == null || dataString.trim().isEmpty()) return LocalDate.now(); 
        try { return LocalDate.parse(dataString, DateTimeFormatter.ofPattern(formato)); } 
        catch (Exception e) { return LocalDate.now(); }
    }

    public static String localDateToString(LocalDate data, String formato) {
        if (data == null) return ""; 
        return data.format(DateTimeFormatter.ofPattern(formato)); 
    }
    
    public static LocalTime stringToLocalTime(String timeString, String formato) {
        if (timeString == null || timeString.trim().isEmpty()) return LocalTime.now(); 
        try { return LocalTime.parse(timeString, DateTimeFormatter.ofPattern(formato)); }
        catch (Exception e) { return LocalTime.now(); }
    }

    public static String localTimeToString(LocalTime timeLocal, String formato) {
        if (timeLocal == null) return ""; 
        return timeLocal.format(DateTimeFormatter.ofPattern(formato)); 
    }
}