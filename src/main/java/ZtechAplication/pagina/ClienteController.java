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

import ZtechAplication.DTO.ClienteDTO;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Email;
import ZtechAplication.model.Endereco;
import ZtechAplication.model.Telefone;
import ZtechAplication.repository.ClienteRepository;


@Controller
@RequestMapping(value = "/cliente" ) 
public class ClienteController {

	@Autowired
	private ClienteRepository clienteRepository; // Renomeado para convenção
	
	// Exibe o formulário de cadastro de novo cliente
	@GetMapping(value = "/cadastrarForm") // Alterado de @RequestMapping para @GetMapping
	public ModelAndView cadastrarForm() { 
		ModelAndView mv = new ModelAndView("cadastroCliente"); // Template cadastroCliente.html
		mv.addObject("clienteDTO", new ClienteDTO() ); // Usa clienteDTO para o formulário
		return mv;
	}
	
	// Processa o cadastro do novo cliente
	@PostMapping(value = "/cadastrar")
	public String cadastrarCliente(@Validated @ModelAttribute("clienteDTO") ClienteDTO clienteDTO, // Usa clienteDTO
										   BindingResult result, 
										   RedirectAttributes attributes, Model model) {
		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO); // Devolve o DTO com os erros
			return "redirect:/cliente/cadastrarForm"; 
		}
		
		// Verifica se CPF já existe
        if (clienteRepository.findByCpf(clienteDTO.getCpf()).isPresent()) {
            attributes.addFlashAttribute("mensagem", "Erro ao cadastrar: CPF já existente.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO);
            return "redirect:/cliente/cadastrarForm";
        }
		
		Cliente cliente = new Cliente();
		cliente.setNomeCliente(clienteDTO.getNomeCliente());
		cliente.setCpf(clienteDTO.getCpf());
		
		Email email = new Email();
		email.setCliente(cliente); 
		cliente.setEmail(email); 
		
		Telefone tele = new Telefone();
		tele.setTelefone(clienteDTO.getTelefone());
		tele.setCliente(cliente);
		cliente.setTelefone(tele);
		
		Endereco end = new Endereco();
		end.setRua(clienteDTO.getRua());
		end.setCep(clienteDTO.getCep());
		end.setBairro(clienteDTO.getBairro());
		end.setCidade(clienteDTO.getCidade());
		end.setNumeroCasa(clienteDTO.getNumeroCasa());
		end.setCliente(cliente);
		cliente.setEndereco(end);
		
		clienteRepository.save(cliente); 
		attributes.addFlashAttribute("mensagem", "Cliente cadastrado(a) com sucesso!");
		return "redirect:/cliente/listar"; // Redireciona para a lista após cadastro
	}
	
	// Lista todos os clientes com paginação
	@GetMapping(value = "/listar")    
	public String listarClientes(Model model, @PageableDefault(size=10) Pageable pageable) { 
		Page<Cliente> paginaClientes = clienteRepository.findAll(pageable); // Busca paginada simples
        // Para carregar relacionamentos com paginação, o ideal é usar @EntityGraph ou fazer o fetch na Specification
        // Aqui, vamos converter para DTO, o que pode acionar lazy loading se não tratado.
        // O método converterParaDTO já lida com nulidade de email, telefone, endereco.
        Page<ClienteDTO> paginaClienteDTOs = paginaClientes.map(this::converterParaDTO);

		model.addAttribute("paginaClientes", paginaClienteDTOs); 
        if (!model.containsAttribute("termo")) { // Garante que 'termo' exista para os links de paginação
            model.addAttribute("termo", null);
        }
		return "clientes"; // Template clientes.html
	}

	// Busca clientes com base em um termo e com paginação
	@GetMapping("/buscar") // Alterado de @RequestMapping
	public String buscarClientes (@RequestParam(value ="termo", required=false) String termo,
						  @PageableDefault (size=10 ) Pageable pageable, 
						  Model model) { 
        Specification<Cliente> spec = SpecificationController.comTermoCli(termo); // Usa a Specification
		Page<Cliente> paginaClientes = clienteRepository.findAll(spec, pageable); // Busca com Specification
        Page<ClienteDTO> paginaClienteDTOs = paginaClientes.map(this::converterParaDTO);

		model.addAttribute("paginaClientes", paginaClienteDTOs);
		model.addAttribute("termo", termo);
        if (termo != null && !termo.isEmpty() && paginaClientes.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhum cliente encontrado para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaClientes.isEmpty()){
             model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }
		return "clientes"; 
	}
	
	// Exibe o formulário de edição de um cliente
	@GetMapping(value = "/editarForm/{idCliente}") // Alterado de @RequestMapping para @GetMapping
	public ModelAndView editarForm(@PathVariable("idCliente") Integer idCliente) { // @PathVariable explícito
		ModelAndView mv = new ModelAndView("alterarCliente"); // Template alterarCliente.html
	    Cliente cliente = clienteRepository.findById(idCliente)
	        .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));
	    
	    ClienteDTO clienteDTO = converterParaDTO(cliente);
	    mv.addObject("clienteDTO", clienteDTO); // Envia clienteDTO para o formulário
	    return mv;							 
	}
	
	// Processa a edição de um cliente existente
	@PostMapping(value = "/editar/{idCliente}") 
	public String editarCliente(@ModelAttribute("clienteDTO") @Validated ClienteDTO clienteDTO, // Alterado "cliente" para "clienteDTO"
							 @PathVariable("idCliente") Integer idCliente, // @PathVariable explícito
							 BindingResult result, 
							 RedirectAttributes attributes, Model model) {

		if (result.hasErrors()) {
			attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO); // Devolve o DTO com os erros
			return "redirect:/cliente/editarForm/" + idCliente;
		}
		
		Cliente cliente = clienteRepository.findById(idCliente)
			    .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));

        // Verifica se o CPF foi alterado e se o novo CPF já existe para OUTRO cliente
        if (!cliente.getCpf().equals(clienteDTO.getCpf())) {
            Optional<Cliente> clienteExistenteComCpf = clienteRepository.findByCpf(clienteDTO.getCpf());
            if (clienteExistenteComCpf.isPresent() && !clienteExistenteComCpf.get().getIdCliente().equals(idCliente)) {
                attributes.addFlashAttribute("mensagem", "Erro ao atualizar: Novo CPF já pertence a outro cliente.");
                attributes.addFlashAttribute("clienteDTO", clienteDTO);
                return "redirect:/cliente/editarForm/" + idCliente;
            }
        }
		
		cliente.setNomeCliente(clienteDTO.getNomeCliente());
		cliente.setCpf(clienteDTO.getCpf());
		
        // Atualiza Email (garante que não seja nulo)
        if (cliente.getEmail() == null) {
            cliente.setEmail(new Email());
            cliente.getEmail().setCliente(cliente);
        }
		
        // Atualiza Telefone (garante que não seja nulo)
        if (cliente.getTelefone() == null) {
            cliente.setTelefone(new Telefone());
            cliente.getTelefone().setCliente(cliente);
        }
		cliente.getTelefone().setTelefone(clienteDTO.getTelefone());
		
        // Atualiza Endereço (garante que não seja nulo)
        if (cliente.getEndereco() == null) {
            cliente.setEndereco(new Endereco());
            cliente.getEndereco().setCliente(cliente);
        }
		cliente.getEndereco().setRua(clienteDTO.getRua());
		cliente.getEndereco().setCep(clienteDTO.getCep());
		cliente.getEndereco().setBairro(clienteDTO.getBairro());
		cliente.getEndereco().setCidade(clienteDTO.getCidade());
		cliente.getEndereco().setNumeroCasa(clienteDTO.getNumeroCasa());
		
		clienteRepository.save(cliente); 
		attributes.addFlashAttribute("mensagem", "Cliente atualizado(a) com sucesso!");
		return "redirect:/cliente/listar"; 
	}
		
	// Deleta um cliente
	@GetMapping(value = "/deletar/{idCliente}") // Alterado de @RequestMapping para @GetMapping
	public String deletarCliente(@PathVariable("idCliente") Integer idCliente, RedirectAttributes attributes) { // @PathVariable explícito
		Cliente cliente = clienteRepository.findById(idCliente) 
	            .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));
	    
        // Antes de deletar o cliente, é importante considerar o que fazer com as Vendas e OrdensDeServico
        // que possam referenciar este cliente.
        // Se houver restrições de chave estrangeira, a exclusão pode falhar.
        // Opções:
        // 1. Impedir a exclusão se houver referências (verificar vendas/OS antes de deletar).
        // 2. Permitir exclusão e as FKs em Venda/OS se tornarem nulas (requer que FK_CLIENTE seja anulável nessas tabelas).
        // 3. Excluir em cascata (configuração no @OneToMany em Cliente - geralmente perigoso para dados transacionais).
        try {
            clienteRepository.delete(cliente); 
            attributes.addFlashAttribute("mensagem", "Cliente removido(a) com sucesso!");
        } catch (Exception e) {
            // Exceção pode ocorrer devido a restrições de chave estrangeira
            attributes.addFlashAttribute("mensagem", "Erro ao remover cliente: Pode estar associado a vendas ou ordens de serviço. Detalhe: " + e.getMessage());
        }
        return "redirect:/cliente/listar"; 
	}
	
	public List<ClienteDTO> getClienteDTO(List<Cliente> clientes) {
		//cria a lista que vai receber a conversão
		List<ClienteDTO> listaDeDTOs = new ArrayList<>();
		//passa um for para popula uma list com os clienets passados
		for (Cliente cliente : clientes) {
			listaDeDTOs.add(converterParaDTO(cliente));
		}
		return listaDeDTOs; 
	}
	
    // Método auxiliar para converter Cliente para ClienteDTO
	private ClienteDTO converterParaDTO(Cliente cliente) {
	    ClienteDTO dto = new ClienteDTO(); 
	    dto.setIdCliente(cliente.getIdCliente());
	    dto.setNomeCliente(cliente.getNomeCliente());
	    dto.setCpf(cliente.getCpf());
	    
	    if (cliente.getEmail() != null) {        
	        dto.setEndEmail(cliente.getEmail().getEndEmail());
	    } else {
	        dto.setEndEmail(""); 
	    }
	    if (cliente.getTelefone() != null) {
	        dto.setTelefone(cliente.getTelefone().getTelefone());
	    } else {
	        dto.setTelefone(""); 
	    }
	    if (cliente.getEndereco() != null) {
	        Endereco end = cliente.getEndereco();
	        dto.setRua(end.getRua());
	        dto.setCep(end.getCep());
	        dto.setBairro(end.getBairro());
	        dto.setCidade(end.getCidade());
	        dto.setNumeroCasa(end.getNumeroCasa());
	    }
	    return dto;
	}
}