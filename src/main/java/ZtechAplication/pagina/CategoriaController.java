package ZtechAplication.pagina;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.model.Categoria;
import ZtechAplication.model.Marca;
import ZtechAplication.model.Servico;
import ZtechAplication.repository.CategoriaRepository;
import ZtechAplication.repository.ServicoRepository;


@Controller
@RequestMapping(value = "/categoria") //para que qualquer um deles seja valido
public class CategoriaController {

	@Autowired
	private CategoriaRepository classeRepo;

	@PostMapping("/enviar")
    public ResponseEntity<Map<String, String>> receberDados(@RequestBody Map<String, String> dados) {
        String nome = dados.get("nome");
        if (nome == null) {
            System.out.println("Nenhum nome recebico");
        }
        System.out.println("Nome recebido: " + nome);

        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", "Nome recebido com sucesso!");
        
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
		classeRepo.save(categoria);
        return ResponseEntity.ok(resposta);
    }

	@GetMapping("/categorias")
	public ResponseEntity<List<Categoria>> listarMarcas() {
	    List<Categoria> categoria = classeRepo.findAll();
	    return ResponseEntity.ok(categoria);
	}
	
	
	
	// Exibe o formulário de cadastro de novo produto
	@GetMapping(value = "/cadastrarForm")
	public ModelAndView cadastrarForm() { // Nome do método mais descritivo
		ModelAndView mv = new ModelAndView("cadastro_categoria"); // Template para cadastrar produto
		mv.addObject("categoria", new Categoria()); // Usar produtoDTO para o formulário
		return mv;
	}
	
	
//	// Processa o cadastro do novo produto
//	@PostMapping(value = "/cadastrar") // Alterado de @RequestMapping para @PostMapping
//	public String cadastrarServico(@Validated @ModelAttribute("categoria") Categoria categoria, BindingResult result,
//			RedirectAttributes attributes, Model model) {
//		
//		if (result.hasErrors()) {
//			attributes.addFlashAttribute("mensagem", "Verifique os campos...");
//			return "redirect:/servico/cadastrarServico";
//		}
//		
//		classeRepo.save(categoria);
//		attributes.addFlashAttribute("mensagem", "Marca cadastrado com sucesso!");
//		return "redirect:/ordens/listar"; // Redireciona para a lista após o cadastro
//	}
	
	@PostMapping("/cadastrar")
	@ResponseBody
	public ResponseEntity<?> cadastrarCategoriaViaAjax(@RequestBody Categoria categoria) {
	    classeRepo.save(categoria);
	    return ResponseEntity.ok("Categoria cadastrada com sucesso!");
	}
}






