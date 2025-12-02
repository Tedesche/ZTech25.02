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


    public List<ClienteDTO> getClienteDTO(List<Cliente> clientes) {
        List<ClienteDTO> listaDeDTOs = new ArrayList<>();
        for (Cliente cliente : clientes) {
            listaDeDTOs.add(converterParaDTO(cliente));
        }
        return listaDeDTOs;
    }

    // --- MÉTODOS API (JSON) ------------------------------------------------------------

    @GetMapping("/api/cliente/listar")
    @ResponseBody
    public ResponseEntity<Page<ClienteDTO>> apiListarClientes(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Cliente> paginaClientes = clienteRepository.findAll(pageable);
        Page<ClienteDTO> paginaDTO = paginaClientes.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }
    
    // --- NOVA API PARA O SELECT DO DASHBOARD (Retorna todos sem paginação) ---
    @GetMapping("/api/cliente/todos")
    @ResponseBody
    public ResponseEntity<List<ClienteDTO>> apiListarTodosClientes() {
        List<Cliente> clientes = clienteRepository.findAll();
        List<ClienteDTO> dtos = getClienteDTO(clientes);
        return ResponseEntity.ok(dtos);
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