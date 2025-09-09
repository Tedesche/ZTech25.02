package ZtechAplication.pagina;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller; // Alterado para Controller
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
// Removido DeleteMapping, GetMapping, etc. pois @RequestMapping já cobre ou serão usados individualmente.
// Mantidos GetMapping e PostMapping onde apropriado.
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestMethod; // Usar anotações específicas como @PostMapping
import org.springframework.web.bind.annotation.RequestParam;
// Removido RestController
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.DTO.ProdutoDTO;
import ZtechAplication.model.Categoria;
import ZtechAplication.model.Marca;
import ZtechAplication.model.Produto;
import ZtechAplication.repository.CategoriaRepository;
import ZtechAplication.repository.MarcaRepository;
import ZtechAplication.repository.ProdutoRepository;


@Controller // Alterado de @RestController para @Controller
@RequestMapping(value = "/produto")
public class ProdutoController {

	@Autowired
	private ProdutoRepository produtoRepository; // Renomeado para seguir convenção
	@Autowired
    private CategoriaRepository categoriaRepository; // Renomeado para seguir convenção
	@Autowired
    private MarcaRepository marcaRepository; // Renomeado para seguir convenção
	
	// Exibe o formulário de cadastro de novo produto
	@GetMapping(value = "/cadastrarForm")
	public ModelAndView cadastrarForm() { // Nome do método mais descritivo
		ModelAndView mv = new ModelAndView("cadastroProduto"); // Template para cadastrar produto
		mv.addObject("produtoDTO", new ProdutoDTO() ); // Usar produtoDTO para o formulário
		// Se precisar carregar categorias e marcas existentes para selects no formulário:
		mv.addObject("categoria", new Categoria() );
        mv.addObject("categorias", categoriaRepository.findAll());
        mv.addObject("marcas", marcaRepository.findAll());
		return mv;
	}
	
	// Processa o cadastro do novo produto
	@PostMapping(value = "/cadastrar") // Alterado de @RequestMapping para @PostMapping
	public String cadastrarProduto(@Validated @ModelAttribute("produtoDTO") ProdutoDTO produtoDTO, BindingResult result, RedirectAttributes attributes, Model model) {
		
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            // Adiciona atributos de volta ao redirect para repopular o formulário e selects
            attributes.addFlashAttribute("produtoDTO", produtoDTO);
            // Se precisar carregar categorias e marcas existentes para selects no formulário:
            // attributes.addFlashAttribute("categoriasExistentes", categoriaRepository.findAll());
            // attributes.addFlashAttribute("marcasExistentes", marcaRepository.findAll());
			return "redirect:/produto/cadastrarForm";
		}
		
		Categoria categoria = categoriaRepository.findById(produtoDTO.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + produtoDTO.getIdCategoria()));
		Marca marca = marcaRepository.findById(produtoDTO.getIdMarca())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + produtoDTO.getIdMarca()));
		
		Produto produto = new Produto();
		produto.setNome(produtoDTO.getNome());
		produto.setCusto(produtoDTO.getCusto());
		produto.setValor(produtoDTO.getValor());
		produto.setQuantidade(produtoDTO.getQuantidade());
		produto.setDescricao(produtoDTO.getDescricao());
		produto.setCategoria(categoria);
		produto.setMarca(marca);
		
		// O método de adicionar produto à lista de categoria/marca não é necessário aqui
        // pois o relacionamento é gerenciado pelo JPA ao salvar o Produto com fk_categoria e fk_marca.
        // A relação `mappedBy` em Categoria e Marca indica que Produto é o dono do relacionamento.

		produtoRepository.save(produto);
		attributes.addFlashAttribute("mensagem", "Produto cadastrado com sucesso!");
		return "redirect:/produto/listar"; // Redireciona para a lista após o cadastro
	}
	
	// Lista todos os produtos com paginação
	@GetMapping(value = "/listar")
	public String listarProdutos(Model model, @PageableDefault(size = 10) Pageable pageable) { // Usa Model e retorna String
		Page<Produto> paginaProdutos = produtoRepository.findAll(pageable); // Busca paginada
        Page<ProdutoDTO> paginaProdutoDTOs = paginaProdutos.map(this::converterParaDTO);

		// Se precisar carregar categorias e marcas existentes para selects no formulário:
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());

		model.addAttribute("paginaProdutos", paginaProdutoDTOs);
        if (!model.containsAttribute("termo")) {
            model.addAttribute("termo", null);
        }
		return "estoque"; // Nome do template HTML para listar produtos (estoque.html)
	}
	
	// Busca produtos com base em um termo e com paginação
	@GetMapping("/buscar") // Alterado de @RequestMapping
	public String buscarProdutos(@RequestParam(value ="termo2", required=false) String termo,
						  @PageableDefault (size=10 ) Pageable pageable, 
						  Model model) {
        Specification<Produto> spec = SpecificationController.comTermoProd(termo);
		Page<Produto> paginaProdutos = produtoRepository.findAll(spec, pageable);
        Page<ProdutoDTO> paginaProdutoDTOs = paginaProdutos.map(this::converterParaDTO);

		// Se precisar carregar categorias e marcas existentes para selects no formulário:
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());
		model.addAttribute("paginaProdutos", paginaProdutoDTOs);
		model.addAttribute("termo", termo);
        if (termo != null && !termo.isEmpty() && paginaProdutos.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhum produto encontrado para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaProdutos.isEmpty()){
            model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }
		return "estoque"; // Mesmo template da listagem
	}
	
	// Busca produtos com base em um termo e com paginação
		@GetMapping("/buscaSequencial") // Alterado de @RequestMapping
		public String buscarProdutosSequencial(@RequestParam(value ="termo1", required=false) String termo1,
				                           @RequestParam(value ="termo2", required=false) String termo2,
							               @PageableDefault (size=10 ) Pageable pageable, 
							               Model model) {
	        Specification<Produto> spec = SpecificationController.comFiltroSequencial(termo1, termo2);
			Page<Produto> paginaProdutos = produtoRepository.findAll(spec, pageable);
	        Page<ProdutoDTO> paginaProdutoDTOs = paginaProdutos.map(this::converterParaDTO);

			// Se precisar carregar categorias e marcas existentes para selects no formulário:
	        model.addAttribute("categorias", categoriaRepository.findAll());
	        model.addAttribute("marcas", marcaRepository.findAll());
			model.addAttribute("paginaProdutos", paginaProdutoDTOs);
			model.addAttribute("termo1", termo1);
			model.addAttribute("termo2", termo2);
			
			String mensagemBusca = null;
//			para ambos os termos
			if (termo1 != null && !termo1.isEmpty() && termo2 != null && !termo2.isEmpty()) {
			    if (paginaProdutos.isEmpty()) {
			        mensagemBusca = "Nenhum produto encontrado para a combinação do Filtro '" + termo1 + "' e do Termo '" + termo2 + "'.";
			    } else {
			        mensagemBusca = "Exibindo resultados para a combinação do Filtro '" + termo1 + "' e do Termo '" + termo2 + "'.";
			    }
			} // apenas para o filtro
			else if (termo1 != null && !termo1.isEmpty()) {
			    if (paginaProdutos.isEmpty()) {
			        mensagemBusca = "Nenhum produto encontrado para o Filtro: '" + termo1 + "'.";
			    } else {
			        mensagemBusca = "Exibindo resultados para o Filtro: '" + termo1 + "'.";
			    }
			} // apenas para a busca 
			else if (termo2 != null && !termo2.isEmpty()) {
			    if (paginaProdutos.isEmpty()) {
			        mensagemBusca = "Nenhum produto encontrado para o Termo: '" + termo2 + "'.";
			    } else {
			        mensagemBusca = "Exibindo resultados para o Termo: '" + termo2 + "'.";
			    }
			}

			if (mensagemBusca != null) {
			    model.addAttribute("mensagemBusca", mensagemBusca);
			} 
	        
			return "estoque"; // Mesmo template da listagem
		}
	
	// Exibe o formulário de edição de um produto
	@GetMapping(value = "/editarForm/{idProduto}")
	public ModelAndView editarForm(@PathVariable("idProduto") Integer idProduto) { // Corrigido @PathVariable
		ModelAndView mv = new ModelAndView("alterarProduto"); // Template para alterar produto
		Produto produto = produtoRepository.findById(idProduto)
				.orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + idProduto));
				
		ProdutoDTO produtoDTO = converterParaDTO(produto);
		mv.addObject("produtoDTO", produtoDTO); // Envia produtoDTO para o formulário
        // Se precisar carregar categorias e marcas existentes para selects:
         mv.addObject("categorias", categoriaRepository.findAll());
         mv.addObject("marcas", marcaRepository.findAll());
		return mv;
	}
	
	// Processa a edição de um produto existente
	@PostMapping(value = "/editar/{idProduto}")
	public String editarProduto(@ModelAttribute("produtoDTO") @Validated ProdutoDTO produtoDTO, // Alterado "produto" para "produtoDTO"
							 @PathVariable("idProduto") Integer idProduto, // Corrigido @PathVariable
							 BindingResult result, 
							 RedirectAttributes attributes, Model model) {

		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("produtoDTO", produtoDTO);
            // attributes.addFlashAttribute("categoriasExistentes", categoriaRepository.findAll());
            // attributes.addFlashAttribute("marcasExistentes", marcaRepository.findAll());
			return "redirect:/produto/editarForm/" + idProduto;
		}

        Produto produto = produtoRepository.findById(idProduto)
			    .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + idProduto));
		
        // Busca ou cria a Categoria
        Categoria categoria = categoriaRepository.findByNome(produtoDTO.getCategoria())
                                .orElseGet(() -> {
                                    Categoria novaCategoria = new Categoria();
                                    novaCategoria.setNome(produtoDTO.getCategoria());
                                    return categoriaRepository.save(novaCategoria);
                                });
        // Busca ou cria a Marca
        Marca marca = marcaRepository.findByNome(produtoDTO.getMarca())
                        .orElseGet(() -> {
                            Marca novaMarca = new Marca();
                            novaMarca.setNome(produtoDTO.getMarca());
                            return marcaRepository.save(novaMarca);
                        });

		produto.setNome(produtoDTO.getNome());
		produto.setCusto(produtoDTO.getCusto());
		produto.setValor(produtoDTO.getValor());
		produto.setQuantidade(produtoDTO.getQuantidade());
		produto.setDescricao(produtoDTO.getDescricao());
		produto.setCategoria(categoria);
        produto.setMarca(marca);
		
		produtoRepository.save(produto);
		attributes.addFlashAttribute("mensagem", "Produto atualizado com sucesso!");
		return "redirect:/produto/listar";
	}
	
	// Deleta um produto
	@GetMapping(value = "/deletar/{idProduto}") // Alterado para @GetMapping para simplicidade, mas @DeleteMapping é semanticamente mais correto se o formulário/link puder enviar DELETE
	public String deletarProduto(@PathVariable("idProduto") Integer idProduto, RedirectAttributes attributes) { // Corrigido @PathVariable
        Produto produto = produtoRepository.findById(idProduto)
				.orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + idProduto));
		
        // Antes de deletar o produto, é importante considerar o que fazer com as Vendas e OrdensDeServico
        // que possam referenciar este produto. Dependendo da regra de negócio:
        // 1. Impedir a exclusão se houver referências.
        // 2. Excluir em cascata (perigoso, pode apagar dados de vendas).
        // 3. Anonimizar a referência no produto (colocar FK_PRODUTO como null em Venda/OS - requer que a FK seja anulável).
        // Por ora, a exclusão direta pode falhar se houver constraints de chave estrangeira.
        // Para este exemplo, vamos assumir que a exclusão é permitida ou que não há referências.
        try {
            produtoRepository.delete(produto);
            attributes.addFlashAttribute("mensagem", "Produto removido com sucesso!");
        } catch (Exception e) {
             // Exceção pode ocorrer devido a restrições de chave estrangeira
            attributes.addFlashAttribute("mensagem", "Erro ao remover produto: Pode estar associado a vendas ou ordens de serviço. Detalhe: " + e.getMessage());
        }
        return "redirect:/produto/listar";
	}
	
    // Método auxiliar para converter Produto para ProdutoDTO
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
	    } else {
	        dto.setIdCategoria(null);
            dto.setCategoria(null);
	    }

	    if (produto.getMarca() != null) {
	        dto.setIdMarca(produto.getMarca().getIdMarca());
	        dto.setMarca(produto.getMarca().getNome()); 
	    } else {
	        dto.setIdMarca(null);
            dto.setMarca(null);
	    }
	    return dto;
	}
	
	@GetMapping(value = "/teste") // Endpoint de teste, pode ser removido
	public String teste (){
		return "correto";
	}
}