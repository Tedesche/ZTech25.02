package ZtechAplication.pagina;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import ZtechAplication.model.Marca;
import ZtechAplication.model.Servico;
import ZtechAplication.repository.MarcaRepository;
import ZtechAplication.repository.ServicoRepository;


@Controller
public class MarcaController {

	@Autowired
	private MarcaRepository marcaRepository;

	@GetMapping("/marcas")
	public ResponseEntity<List<Marca>> listarMarcas() {
	    List<Marca> marcas = marcaRepository.findAll();
	    return ResponseEntity.ok(marcas);
	}
	
	// 1. Listar Marcas (Resolve o erro: GET /api/marcas 404)
    @GetMapping("/api/marcas")
    @ResponseBody
    public ResponseEntity<List<Marca>> listarMarcasApi() {
        return ResponseEntity.ok(marcaRepository.findAll());
    }
    
    // 2. Salvar Marca (Já parecia ter algo, mas confirme se está assim)
    @PostMapping("/api/marcas/salvar")
    @ResponseBody
    public ResponseEntity<?> salvarMarcaApi(@RequestBody Marca marca) {
        if (marca.getNome() == null || marca.getNome().trim().isEmpty()) {
             return ResponseEntity.badRequest().body("Nome inválido");
        }
        return ResponseEntity.ok(marcaRepository.save(marca));
    }
	
		
}
