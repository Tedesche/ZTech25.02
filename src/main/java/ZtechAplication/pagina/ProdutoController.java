package ZtechAplication.pagina;

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

import ZtechAplication.DTO.ProdutoDTO;
import ZtechAplication.model.Categoria;
import ZtechAplication.model.Marca;
import ZtechAplication.model.Produto;
import ZtechAplication.repository.CategoriaRepository;
import ZtechAplication.repository.MarcaRepository;
import ZtechAplication.repository.ProdutoRepository;

@Controller
@RequestMapping(value = "/produto")
public class ProdutoController {

	@Autowired
	private ProdutoRepository produtoRepository;
	@Autowired
    private CategoriaRepository categoriaRepository;
	@Autowired
    private MarcaRepository marcaRepository;
	
	// --- MÉTODOS DE VISUALIZAÇÃO (Telas HTML) ---

	@GetMapping(value = "/cadastrarForm")
	public ModelAndView cadastrarForm() {
		ModelAndView mv = new ModelAndView("cadastroProduto");
		mv.addObject("produtoDTO", new ProdutoDTO());
		mv.addObject("categoria", new Categoria());
        mv.addObject("categorias", categoriaRepository.findAll());
        mv.addObject("marcas", marcaRepository.findAll());
		return mv;
	}
	
	@PostMapping(value = "/cadastrar")
	public String cadastrarProduto(@Validated @ModelAttribute("produtoDTO") ProdutoDTO produtoDTO, BindingResult result, RedirectAttributes attributes, Model model) {
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("produtoDTO", produtoDTO);
			return "redirect:/produto/cadastrarForm";
		}
		
		Categoria categoria = categoriaRepository.findById(produtoDTO.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoria inválida: " + produtoDTO.getIdCategoria()));
		Marca marca = marcaRepository.findById(produtoDTO.getIdMarca())
                .orElseThrow(() -> new IllegalArgumentException("Marca inválida: " + produtoDTO.getIdMarca()));
		
		Produto produto = new Produto();
		produto.setNome(produtoDTO.getNome());
		produto.setCusto(produtoDTO.getCusto());
		produto.setValor(produtoDTO.getValor());
		produto.setQuantidade(produtoDTO.getQuantidade());
		produto.setDescricao(produtoDTO.getDescricao());
		produto.setCategoria(categoria);
		produto.setMarca(marca);
		
		produtoRepository.save(produto);
		attributes.addFlashAttribute("mensagem", "Produto cadastrado com sucesso!");
		return "redirect:/produto/listar";
	}
	
	@GetMapping(value = "/listar")
	public String listarProdutos(Model model, @PageableDefault(size = 10) Pageable pageable) {
		Page<Produto> paginaProdutos = produtoRepository.findAll(pageable);
        Page<ProdutoDTO> paginaProdutoDTOs = paginaProdutos.map(this::converterParaDTO);

        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());
		model.addAttribute("paginaProdutos", paginaProdutoDTOs);
        
        if (!model.containsAttribute("termo")) {
            model.addAttribute("termo", null);
        }
		return "estoque";
	}
	
    // --- MÉTODO REINSERIDO (Necessário para o indexController) ---
    public List<ProdutoDTO> getProdutoDTO(List<Produto> produtos) {
        List<ProdutoDTO> listaDeDTOs = new ArrayList<>();
        for (Produto produto : produtos) {
            listaDeDTOs.add(converterParaDTO(produto));
        }
        return listaDeDTOs;
    }

	// --- MÉTODOS DA API (JSON para o Dashboard) ---
	
	@GetMapping("/api/produto/listar")
    @ResponseBody 
    public ResponseEntity<Page<ProdutoDTO>> apiListarProdutos(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Produto> paginaProdutos = produtoRepository.findAll(pageable);
        Page<ProdutoDTO> paginaDTO = paginaProdutos.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @PostMapping("/api/produto/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarProduto(@RequestBody ProdutoDTO produtoDTO) {
        try {
            if (produtoDTO.getNome() == null || produtoDTO.getNome().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("O nome do produto é obrigatório.");
            }
            if (produtoDTO.getIdCategoria() == null) {
                return ResponseEntity.badRequest().body("A categoria é obrigatória.");
            }
            if (produtoDTO.getIdMarca() == null) {
                return ResponseEntity.badRequest().body("A marca é obrigatória.");
            }

            Categoria categoria = categoriaRepository.findById(produtoDTO.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
            Marca marca = marcaRepository.findById(produtoDTO.getIdMarca())
                .orElseThrow(() -> new IllegalArgumentException("Marca não encontrada"));

            Produto produto = new Produto();
            if (produtoDTO.getIdProduto() != null) {
                 produto = produtoRepository.findById(produtoDTO.getIdProduto()).orElse(new Produto());
            }
            
            produto.setNome(produtoDTO.getNome());
            produto.setCusto(produtoDTO.getCusto());
            produto.setValor(produtoDTO.getValor());
            produto.setQuantidade(produtoDTO.getQuantidade());
            produto.setDescricao(produtoDTO.getDescricao());
            produto.setCategoria(categoria);
            produto.setMarca(marca);

            Produto produtoSalvo = produtoRepository.save(produto);
            return ResponseEntity.ok(converterParaDTO(produtoSalvo));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erro ao salvar produto: " + e.getMessage());
        }
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ProdutoDTO> apiBuscarProduto(@PathVariable Integer id) {
        return produtoRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarProduto(@PathVariable Integer id) {
        try {
            if (!produtoRepository.existsById(id)) return ResponseEntity.notFound().build();
            produtoRepository.deleteById(id);
            return ResponseEntity.ok("Deletado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Erro: Produto possui vínculos.");
        }
    }
	
    // Método auxiliar conversor
	private ProdutoDTO converterParaDTO(Produto produto) {
	    ProdutoDTO dto = new ProdutoDTO();
	    dto.setIdProduto(produto.getIdProduto());
	    dto.setNome(produto.getNome());
	    dto.setCusto(produto.getCusto());
	    dto.setValor(produto.getValor());
        dto.setQuantidade(produto.getQuantidade());
	    dto.setDescricao(produto.getDescricao());

	    if (produto.getCategoria() != null) {
	        dto.setIdCategoria(produto.getCategoria().getIdCategoria());
	        dto.setCategoria(produto.getCategoria().getNome()); 
	    }
	    if (produto.getMarca() != null) {
	        dto.setIdMarca(produto.getMarca().getIdMarca());
	        dto.setMarca(produto.getMarca().getNome()); 
	    }
	    return dto;
	}
}