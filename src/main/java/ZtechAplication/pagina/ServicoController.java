package ZtechAplication.pagina;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.model.Servico;
import ZtechAplication.repository.ServicoRepository;


@Controller
@RequestMapping(value = "/servico") //para que qualquer um deles seja valido
public class ServicoController {

	@Autowired
	private ServicoRepository classeRepo;

	// Exibe o formulário de cadastro de novo produto
	@GetMapping(value = "/cadastrarForm")
	public ModelAndView cadastrarForm() { // Nome do método mais descritivo
		ModelAndView mv = new ModelAndView("cadastro_servico"); // Template para cadastrar produto
		mv.addObject("servico", new Servico()); // Usar produtoDTO para o formulário
		return mv;
	}
	
	
	// Processa o cadastro do novo produto
	@PostMapping(value = "/cadastrar") // Alterado de @RequestMapping para @PostMapping
	public String cadastrarServico(@Validated @ModelAttribute("servico") Servico servico, BindingResult result,
			RedirectAttributes attributes, Model model) {
		
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos...");
			return "redirect:/servico/cadastrarServico";
		}
		
		classeRepo.save(servico);
		attributes.addFlashAttribute("mensagem", "Serviço cadastrado com sucesso!");
		return "redirect:/ordens/listar"; // Redireciona para a lista após o cadastro
	}
			
	
	
	
	
}
