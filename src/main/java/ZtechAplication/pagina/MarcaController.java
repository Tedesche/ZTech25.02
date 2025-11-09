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
	private MarcaRepository classeRepo;

	@GetMapping("/marcas")
	public ResponseEntity<List<Marca>> listarMarcas() {
	    List<Marca> marcas = classeRepo.findAll();
	    return ResponseEntity.ok(marcas);
	}
	
	// Novo metodo de Salvar
	@PostMapping("/api/marcas/salvar")
	@ResponseBody
	public ResponseEntity<?> marcaSalvar (@RequestBody Marca marca) {
		try { //validação do campo
			if  ( marca.getNome() == null || marca.getNome().trim().isEmpty() ){
				return ResponseEntity
						.status(HttpStatus.BAD_REQUEST)
						.body("O nome da MarcaNõ pode ser vazia.");
			}
			Marca marcaSalva = classeRepo.save(marca);
			
			//da um retorno 200, positovo
			return ResponseEntity.ok(marcaSalva);
			
		}	catch (Exception e) {
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Erro ao salvar a Marca " + e.getMessage());
		}
		
	}
	 
	// metodo para a modal de produto
	@GetMapping("/api/marcas")
    @ResponseBody
    public List<Marca> getMarcasApi() {
        return classeRepo.findAll();
    }
	
		
}
