package ZtechAplication.pagina;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // Importante
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.model.Servico;
import ZtechAplication.repository.ServicoRepository;

@Controller
@RequestMapping(value = "/servico")
public class ServicoController {

    @Autowired
    private ServicoRepository classeRepo;

    // --- MÉTODOS DE VIEW (Antigos) ---
    @GetMapping(value = "/cadastrarForm")
    public ModelAndView cadastrarForm() {
        ModelAndView mv = new ModelAndView("cadastro_servico");
        mv.addObject("servico", new Servico());
        return mv;
    }
    
    @PostMapping(value = "/cadastrar")
    public String cadastrarServico(@Validated @ModelAttribute("servico") Servico servico, BindingResult result,
            RedirectAttributes attributes, Model model) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos...");
            return "redirect:/servico/cadastrarServico";
        }
        classeRepo.save(servico);
        attributes.addFlashAttribute("mensagem", "Serviço cadastrado com sucesso!");
        return "redirect:/ordens/listar";
    }

    // --- MÉTODOS API (Novos para o Dashboard) ---

    // 1. Listar para o Select
    @GetMapping("/api/servico/todos")
    @ResponseBody
    public ResponseEntity<Iterable<Servico>> apiListarTodosServicos() {
        return ResponseEntity.ok(classeRepo.findAll());
    }

    // 2. Salvar via JSON (Resolve o erro do Hibernate)
    @PostMapping("/api/servico/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarServicoJson(@RequestBody Servico servico) {
        try {
            if (servico.getNome() == null || servico.getNome().isEmpty()) {
                return ResponseEntity.badRequest().body("O nome do serviço é obrigatório.");
            }
            Servico salvo = classeRepo.save(servico);
            return ResponseEntity.ok(salvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao salvar: " + e.getMessage());
        }
    }
}