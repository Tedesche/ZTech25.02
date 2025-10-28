package ZtechAplication.pagina;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// import java.util.List; // Removido se a listagem principal for paginada

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.DeleteMapping; // Usar @GetMapping para simplicidade no HTML
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping; // Usar @PostMapping para edição
import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestMethod; // Usar anotações específicas
import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController; // Alterado para @Controller
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ZtechAplication.DTO.FuncionarioDTO;
import ZtechAplication.DTO.FuncionarioDTO;
import ZtechAplication.model.Funcionario;
import ZtechAplication.model.Email;
import ZtechAplication.model.Endereco;
import ZtechAplication.model.Funcionario;
import ZtechAplication.model.Telefone;
import ZtechAplication.repository.FuncionarioRepository;


@Controller
@RequestMapping(value = "/funcionario" ) 
public class FuncionarioController {

	@Autowired
	private FuncionarioRepository funcionarioRepository; // Renomeado para convenção
	
	// Exibe o formulário de cadastro de novo funcionario
	@GetMapping(value = "/cadastrarForm") // Alterado de @RequestMapping para @GetMapping
	public ModelAndView cadastrarForm() { 
		ModelAndView mv = new ModelAndView("cadastroFuncionario"); // Template cadastroFuncionario.html
		mv.addObject("funcionarioDTO", new FuncionarioDTO() ); // Usa funcionarioDTO para o formulário
		return mv;
	}
	
	// Processa o cadastro do novo funcionario
	@PostMapping(value = "/cadastrar")
	public String cadastrarFuncionario(@Validated @ModelAttribute("funcionarioDTO") FuncionarioDTO funcionarioDTO, // Usa funcionarioDTO
										   BindingResult result, 
										   RedirectAttributes attributes, Model model) {
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO); // Devolve o DTO com os erros
			return "redirect:/funcionario/cadastrarForm"; 
		}
		
		// Verifica se CPF já existe
        if (funcionarioRepository.findByCpf(funcionarioDTO.getCpf()).isPresent()) {
            attributes.addFlashAttribute("mensagem", "Erro ao cadastrar: CPF já existente.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
            return "redirect:/funcionario/cadastrarForm";
        }
		
		Funcionario funcionario = new Funcionario();
		funcionario.setNomeFuncionario(funcionarioDTO.getNomeFuncionario());
		funcionario.setCpf(funcionarioDTO.getCpf());
		
		Email email = new Email();
		email.setEmail(funcionarioDTO.getEndEmail());
		email.setFuncionario(funcionario); 
		funcionario.setEmail(email); 
		
		Telefone tele = new Telefone();
		tele.setTelefone(funcionarioDTO.getTelefone());
		tele.setFuncionario(funcionario);
		funcionario.setTelefone(tele);
		
		Endereco end = new Endereco();
		end.setRua(funcionarioDTO.getRua());
		end.setCep(funcionarioDTO.getCep());
		end.setBairro(funcionarioDTO.getBairro());
		end.setCidade(funcionarioDTO.getCidade());
		end.setNumeroCasa(funcionarioDTO.getNumeroCasa());
		end.setFuncionario(funcionario);
		funcionario.setEndereco(end);
		
		funcionarioRepository.save(funcionario); 
		attributes.addFlashAttribute("mensagem", "Funcionario cadastrado(a) com sucesso!");
		return "redirect:/funcionario/listar"; // Redireciona para a lista após cadastro
	}
	
	// Lista todos os funcionarios com paginação
	@GetMapping(value = "/listar")    
	public String listarFuncionarios(Model model, @PageableDefault(size=10) Pageable pageable) { 
		Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(pageable); // Busca paginada simples
        // Para carregar relacionamentos com paginação, o ideal é usar @EntityGraph ou fazer o fetch na Specification
        // Aqui, vamos converter para DTO, o que pode acionar lazy loading se não tratado.
        // O método converterParaDTO já lida com nulidade de email, telefone, endereco.
        Page<FuncionarioDTO> paginaFuncionarioDTOs = paginaFuncionarios.map(this::converterParaDTO);

		model.addAttribute("paginaFuncionarios", paginaFuncionarioDTOs); 
        if (!model.containsAttribute("termo")) { // Garante que 'termo' exista para os links de paginação
            model.addAttribute("termo", null);
        }
		return "funcionarios"; // Template funcionarios.html
	}

	// Busca funcionarios com base em um termo e com paginação
	@GetMapping("/buscar") // Alterado de @RequestMapping
	public String buscarFuncionarios (@RequestParam(value ="termo", required=false) String termo,
						  @PageableDefault (size=10 ) Pageable pageable, 
						  Model model) { 
        Specification<Funcionario> spec = SpecificationController.comTermoFun(termo); // Usa a Specification
		Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(spec, pageable); // Busca com Specification
        Page<FuncionarioDTO> paginaFuncionarioDTOs = paginaFuncionarios.map(this::converterParaDTO);

		model.addAttribute("paginaFuncionarios", paginaFuncionarioDTOs);
		model.addAttribute("termo", termo);
        if (termo != null && !termo.isEmpty() && paginaFuncionarios.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhum funcionario encontrado para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaFuncionarios.isEmpty()){
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }
		return "funcionarios"; 
	}
	
	// Exibe o formulário de edição de um funcionario
	@GetMapping(value = "/editarForm/{idFuncionario}") // Alterado de @RequestMapping para @GetMapping
	public ModelAndView editarForm(@PathVariable("idFuncionario") Integer idFuncionario) { // @PathVariable explícito
		ModelAndView mv = new ModelAndView("alterarFuncionario"); // Template alterarFuncionario.html
	    Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
	        .orElseThrow(() -> new IllegalArgumentException("Funcionario inválido: " + idFuncionario));
	    
	    FuncionarioDTO funcionarioDTO = converterParaDTO(funcionario);
	    mv.addObject("funcionarioDTO", funcionarioDTO); // Envia funcionarioDTO para o formulário
	    return mv;							 
	}
	
	// Processa a edição de um funcionario existente
	@PostMapping(value = "/editar/{idFuncionario}") 
	public String editarFuncionario(@ModelAttribute("funcionarioDTO") @Validated FuncionarioDTO funcionarioDTO, // Alterado "funcionario" para "funcionarioDTO"
							 @PathVariable("idFuncionario") Integer idFuncionario, // @PathVariable explícito
							 BindingResult result, 
							 RedirectAttributes attributes, Model model) {

		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO); // Devolve o DTO com os erros
			return "redirect:/funcionario/editarForm/" + idFuncionario;
		}
		
		Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
			    .orElseThrow(() -> new IllegalArgumentException("Funcionario inválido: " + idFuncionario));

        // Verifica se o CPF foi alterado e se o novo CPF já existe para OUTRO funcionario
        if (!funcionario.getCpf().equals(funcionarioDTO.getCpf())) {
            Optional<Funcionario> funcionarioExistenteComCpf = funcionarioRepository.findByCpf(funcionarioDTO.getCpf());
            if (funcionarioExistenteComCpf.isPresent() && !funcionarioExistenteComCpf.get().getIdFun().equals(idFuncionario)) {
                attributes.addFlashAttribute("mensagem", "Erro ao atualizar: Novo CPF já pertence a outro funcionario.");
                attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
                return "redirect:/funcionario/editarForm/" + idFuncionario;
            }
        }
		
		funcionario.setNomeFuncionario(funcionarioDTO.getNomeFuncionario());
		funcionario.setCpf(funcionarioDTO.getCpf());
		
        // Atualiza Email (garante que não seja nulo)
        if (funcionario.getEmail() == null) {
            funcionario.setEmail(new Email());
            funcionario.getEmail().setFuncionario(funcionario);
        }
		funcionario.getEmail().setEmail(funcionarioDTO.getEndEmail());
		
        // Atualiza Telefone (garante que não seja nulo)
        if (funcionario.getTelefone() == null) {
            funcionario.setTelefone(new Telefone());
            funcionario.getTelefone().setFuncionario(funcionario);
        }
		funcionario.getTelefone().setTelefone(funcionarioDTO.getTelefone());
		
        // Atualiza Endereço (garante que não seja nulo)
        if (funcionario.getEndereco() == null) {
            funcionario.setEndereco(new Endereco());
            funcionario.getEndereco().setFuncionario(funcionario);
        }
		funcionario.getEndereco().setRua(funcionarioDTO.getRua());
		funcionario.getEndereco().setCep(funcionarioDTO.getCep());
		funcionario.getEndereco().setBairro(funcionarioDTO.getBairro());
		funcionario.getEndereco().setCidade(funcionarioDTO.getCidade());
		funcionario.getEndereco().setNumeroCasa(funcionarioDTO.getNumeroCasa());
		
		funcionarioRepository.save(funcionario); 
		attributes.addFlashAttribute("mensagem", "Funcionario atualizado(a) com sucesso!");
		return "redirect:/funcionario/listar"; 
	}
		
	// Deleta um funcionario
	@GetMapping(value = "/deletar/{idFuncionario}") // Alterado de @RequestMapping para @GetMapping
	public String deletarFuncionario(@PathVariable("idFuncionario") Integer idFuncionario, RedirectAttributes attributes) { // @PathVariable explícito
		Funcionario funcionario = funcionarioRepository.findById(idFuncionario) 
	            .orElseThrow(() -> new IllegalArgumentException("Funcionario inválido: " + idFuncionario));
	    
        // Antes de deletar o funcionario, é importante considerar o que fazer com as Vendas e OrdensDeServico
        // que possam referenciar este funcionario.
        // Se houver restrições de chave estrangeira, a exclusão pode falhar.
        // Opções:
        // 1. Impedir a exclusão se houver referências (verificar vendas/OS antes de deletar).
        // 2. Permitir exclusão e as FKs em Venda/OS se tornarem nulas (requer que FK_CLIENTE seja anulável nessas tabelas).
        // 3. Excluir em cascata (configuração no @OneToMany em Funcionario - geralmente perigoso para dados transacionais).
        try {
            funcionarioRepository.delete(funcionario); 
            attributes.addFlashAttribute("mensagem", "Funcionario removido(a) com sucesso!");
        } catch (Exception e) {
            // Exceção pode ocorrer devido a restrições de chave estrangeira
            attributes.addFlashAttribute("mensagem", "Erro ao remover funcionario: Pode estar associado a vendas ou ordens de serviço. Detalhe: " + e.getMessage());
        }
        return "redirect:/funcionario/listar"; 
	}
	
	public List<FuncionarioDTO> getFuncionarioDTO(List<Funcionario> funcionarios) {
		//cria a lista que vai receber a conversão
		List<FuncionarioDTO> listaDeDTOs = new ArrayList<>();
		//passa um for para popula uma list com os clienets passados
		for (Funcionario funcionario : funcionarios) {
			listaDeDTOs.add(converterParaDTO(funcionario));
		}
		return listaDeDTOs; 
	}
	
    // Método auxiliar para converter Funcionario para FuncionarioDTO
	private FuncionarioDTO converterParaDTO(Funcionario funcionario) {
	    FuncionarioDTO dto = new FuncionarioDTO(); 
	    dto.setIdFuncionario(funcionario.getIdFun());
	    dto.setNomeFuncionario(funcionario.getNomeFuncionario());
	    dto.setCpf(funcionario.getCpf());
	    
	    if (funcionario.getEmail() != null) {        
	        dto.setEndEmail(funcionario.getEmail().getEndEmail());
	    } else {
	        dto.setEndEmail(""); 
	    }
	    if (funcionario.getTelefone() != null) {
	        dto.setTelefone(funcionario.getTelefone().getTelefone());
	    } else {
	        dto.setTelefone(""); 
	    }
	    if (funcionario.getEndereco() != null) {
	        Endereco end = funcionario.getEndereco();
	        dto.setRua(end.getRua());
	        dto.setCep(end.getCep());
	        dto.setBairro(end.getBairro());
	        dto.setCidade(end.getCidade());
	        dto.setNumeroCasa(end.getNumeroCasa());
	    }
	    return dto;
	}
}