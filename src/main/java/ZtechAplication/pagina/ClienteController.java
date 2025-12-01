package ZtechAplication.pagina;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import ZtechAplication.DTO.ClienteDTO;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Email;
import ZtechAplication.model.Endereco;
import ZtechAplication.model.Telefone;
import ZtechAplication.repository.ClienteRepository;

@Controller
@RequestMapping(value = "/cliente")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    // --- VIEW METHODS (THYMELEAF) ---

    @GetMapping(value = "/cadastrarForm")
    public ModelAndView cadastrarForm() {
        ModelAndView mv = new ModelAndView("cadastroCliente");
        mv.addObject("clienteDTO", new ClienteDTO());
        return mv;
    }

    @PostMapping(value = "/cadastrar")
    public String cadastrarCliente(@Validated @ModelAttribute("clienteDTO") ClienteDTO clienteDTO, 
                                   BindingResult result, 
                                   RedirectAttributes attributes, Model model) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO);
            return "redirect:/cliente/cadastrarForm";
        }

        if (clienteRepository.findByCpf(clienteDTO.getCpf()).isPresent()) {
            attributes.addFlashAttribute("mensagem", "Erro ao cadastrar: CPF já existente.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO);
            return "redirect:/cliente/cadastrarForm";
        }

        Cliente cliente = new Cliente();
        atualizarDadosCliente(cliente, clienteDTO);
        
        clienteRepository.save(cliente);
        attributes.addFlashAttribute("mensagem", "Cliente cadastrado(a) com sucesso!");
        return "redirect:/cliente/listar";
    }

    @GetMapping(value = "/listar")
    public String listarClientes(Model model, @PageableDefault(size = 10) Pageable pageable) {
        Page<Cliente> paginaClientes = clienteRepository.findAll(pageable);
        Page<ClienteDTO> paginaClienteDTOs = paginaClientes.map(this::converterParaDTO);

        model.addAttribute("paginaClientes", paginaClienteDTOs);
        if (!model.containsAttribute("termo")) {
            model.addAttribute("termo", null);
        }
        return "clientes";
    }

    @GetMapping("/buscar")
    public String buscarClientes(@RequestParam(value = "termo", required = false) String termo,
                                 @PageableDefault(size = 10) Pageable pageable,
                                 Model model) {
        Specification<Cliente> spec = SpecificationController.comTermoCli(termo);
        Page<Cliente> paginaClientes = clienteRepository.findAll(spec, pageable);
        Page<ClienteDTO> paginaClienteDTOs = paginaClientes.map(this::converterParaDTO);

        model.addAttribute("paginaClientes", paginaClienteDTOs);
        model.addAttribute("termo", termo);
        if (termo != null && !termo.isEmpty() && paginaClientes.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhum cliente encontrado para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaClientes.isEmpty()) {
            model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }
        return "clientes";
    }

    @GetMapping(value = "/editarForm/{idCliente}")
    public ModelAndView editarForm(@PathVariable("idCliente") Integer idCliente) {
        ModelAndView mv = new ModelAndView("alterarCliente");
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));

        ClienteDTO clienteDTO = converterParaDTO(cliente);
        mv.addObject("clienteDTO", clienteDTO);
        return mv;
    }

    @PostMapping(value = "/editar/{idCliente}")
    public String editarCliente(@ModelAttribute("clienteDTO") @Validated ClienteDTO clienteDTO,
                                @PathVariable("idCliente") Integer idCliente,
                                BindingResult result,
                                RedirectAttributes attributes, Model model) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("clienteDTO", clienteDTO);
            return "redirect:/cliente/editarForm/" + idCliente;
        }

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));

        if (!cliente.getCpf().equals(clienteDTO.getCpf())) {
            Optional<Cliente> clienteExistenteComCpf = clienteRepository.findByCpf(clienteDTO.getCpf());
            if (clienteExistenteComCpf.isPresent() && !clienteExistenteComCpf.get().getIdCliente().equals(idCliente)) {
                attributes.addFlashAttribute("mensagem", "Erro ao atualizar: Novo CPF já pertence a outro cliente.");
                attributes.addFlashAttribute("clienteDTO", clienteDTO);
                return "redirect:/cliente/editarForm/" + idCliente;
            }
        }

        atualizarDadosCliente(cliente, clienteDTO);
        clienteRepository.save(cliente);
        attributes.addFlashAttribute("mensagem", "Cliente atualizado(a) com sucesso!");
        return "redirect:/cliente/listar";
    }

    @GetMapping(value = "/deletar/{idCliente}")
    public String deletarCliente(@PathVariable("idCliente") Integer idCliente, RedirectAttributes attributes) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido: " + idCliente));
        try {
            clienteRepository.delete(cliente);
            attributes.addFlashAttribute("mensagem", "Cliente removido(a) com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagem", "Erro ao remover cliente: Pode estar associado a vendas ou ordens de serviço. Detalhe: " + e.getMessage());
        }
        return "redirect:/cliente/listar";
    }

    public List<ClienteDTO> getClienteDTO(List<Cliente> clientes) {
        List<ClienteDTO> listaDeDTOs = new ArrayList<>();
        for (Cliente cliente : clientes) {
            listaDeDTOs.add(converterParaDTO(cliente));
        }
        return listaDeDTOs;
    }

    // --- NOVOS MÉTODOS API (JSON) ------------------------------------------------------------

    @GetMapping("/api/cliente/listar")
    @ResponseBody
    public ResponseEntity<Page<ClienteDTO>> apiListarClientes(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Cliente> paginaClientes = clienteRepository.findAll(pageable);
        Page<ClienteDTO> paginaDTO = paginaClientes.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @PostMapping("/api/cliente/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarCliente(@RequestBody ClienteDTO clienteDTO) {
        try {
            if (clienteDTO.getNomeCliente() == null || clienteDTO.getNomeCliente().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("O nome do cliente é obrigatório.");
            }

            Cliente cliente = new Cliente();
            if (clienteDTO.getIdCliente() != null) {
                cliente = clienteRepository.findById(clienteDTO.getIdCliente()).orElse(new Cliente());
            } else {
                // Validação de CPF apenas na criação
                if (clienteRepository.findByCpf(clienteDTO.getCpf()).isPresent()) {
                    return ResponseEntity.badRequest().body("CPF já cadastrado.");
                }
            }

            atualizarDadosCliente(cliente, clienteDTO);
            Cliente clienteSalvo = clienteRepository.save(cliente);
            return ResponseEntity.ok(converterParaDTO(clienteSalvo));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar cliente: " + e.getMessage());
        }
    }

    @GetMapping("/api/cliente/{id}")
    @ResponseBody
    public ResponseEntity<ClienteDTO> apiBuscarCliente(@PathVariable Integer id) {
        return clienteRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/cliente/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarCliente(@PathVariable Integer id) {
        try {
            if (!clienteRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            clienteRepository.deleteById(id);
            return ResponseEntity.ok("Deletado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Erro: Cliente não pode ser excluído pois possui vínculos.");
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void atualizarDadosCliente(Cliente cliente, ClienteDTO clienteDTO) {
        cliente.setNomeCliente(clienteDTO.getNomeCliente());
        cliente.setCpf(clienteDTO.getCpf());

        if (cliente.getEmail() == null) {
            cliente.setEmail(new Email());
            cliente.getEmail().setCliente(cliente);
        }
        cliente.getEmail().setEndEmail(clienteDTO.getEndEmail());

        if (cliente.getTelefone() == null) {
            cliente.setTelefone(new Telefone());
            cliente.getTelefone().setCliente(cliente);
        }
        cliente.getTelefone().setTelefone(clienteDTO.getTelefone());

        if (cliente.getEndereco() == null) {
            cliente.setEndereco(new Endereco());
            cliente.getEndereco().setCliente(cliente);
        }
        cliente.getEndereco().setRua(clienteDTO.getRua());
        cliente.getEndereco().setCep(clienteDTO.getCep());
        cliente.getEndereco().setBairro(clienteDTO.getBairro());
        cliente.getEndereco().setCidade(clienteDTO.getCidade());
        cliente.getEndereco().setNumeroCasa(clienteDTO.getNumeroCasa());
    }

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