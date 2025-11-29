package ZtechAplication.pagina;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import ZtechAplication.model.Categoria;
import ZtechAplication.repository.CategoriaRepository;

@Controller
public class CategoriaController {

	@Autowired
    private CategoriaRepository categoriaRepository;

	// --- MANTENHA ESTE: É a API correta que retorna JSON ---
    @GetMapping("/api/categorias")
    @ResponseBody
    public ResponseEntity<List<Categoria>> listarCategoriasApi() {
        return ResponseEntity.ok(categoriaRepository.findAll());
    }
    

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
        categoriaRepository.save(categoria);
        return ResponseEntity.ok(resposta);
    }
	
	@PostMapping("/cadastrar")
	@ResponseBody
	public ResponseEntity<?> cadastrarCategoriaViaAjax(@RequestBody Categoria categoria) {
		categoriaRepository.save(categoria);
	    return ResponseEntity.ok("Categoria cadastrada com sucesso!");
	}
	
	
	
}






