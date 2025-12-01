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

import ZtechAplication.DTO.FuncionarioDTO;
import ZtechAplication.model.Email;
import ZtechAplication.model.Endereco;
import ZtechAplication.model.Funcionario;
import ZtechAplication.model.Telefone;
import ZtechAplication.repository.FuncionarioRepository;

@Controller
@RequestMapping(value = "/funcionario")
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    // --- MÉTODOS DE VIEW (THYMELEAF) ---

    @GetMapping(value = "/cadastrarForm")
    public ModelAndView cadastrarForm() {
        ModelAndView mv = new ModelAndView("cadastroFuncionario");
        mv.addObject("funcionarioDTO", new FuncionarioDTO());
        return mv;
    }

    @PostMapping(value = "/cadastrar")
    public String cadastrarFuncionario(@Validated @ModelAttribute("funcionarioDTO") FuncionarioDTO funcionarioDTO,
                                       BindingResult result,
                                       RedirectAttributes attributes, Model model) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
            return "redirect:/funcionario/cadastrarForm";
        }

        // Verifica se CPF já existe
        if (funcionarioRepository.findByCpf(funcionarioDTO.getCpf()).isPresent()) {
            attributes.addFlashAttribute("mensagem", "Erro ao cadastrar: CPF já existente.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
            return "redirect:/funcionario/cadastrarForm";
        }

        Funcionario funcionario = new Funcionario();
        atualizarDadosFuncionario(funcionario, funcionarioDTO);
        
        funcionarioRepository.save(funcionario);
        attributes.addFlashAttribute("mensagem", "Funcionário cadastrado(a) com sucesso!");
        return "redirect:/funcionario/listar";
    }

    @GetMapping(value = "/listar")
    public String listarFuncionarios(Model model, @PageableDefault(size = 10) Pageable pageable) {
        Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(pageable);
        Page<FuncionarioDTO> paginaFuncionarioDTOs = paginaFuncionarios.map(this::converterParaDTO);

        model.addAttribute("paginaFuncionarios", paginaFuncionarioDTOs);
        if (!model.containsAttribute("termo")) {
            model.addAttribute("termo", null);
        }
        return "funcionarios";
    }

    @GetMapping("/buscar")
    public String buscarFuncionarios(@RequestParam(value = "termo", required = false) String termo,
                                     @PageableDefault(size = 10) Pageable pageable,
                                     Model model) {
        Specification<Funcionario> spec = SpecificationController.comTermoFun(termo);
        Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(spec, pageable);
        Page<FuncionarioDTO> paginaFuncionarioDTOs = paginaFuncionarios.map(this::converterParaDTO);

        model.addAttribute("paginaFuncionarios", paginaFuncionarioDTOs);
        model.addAttribute("termo", termo);
        if (termo != null && !termo.isEmpty() && paginaFuncionarios.isEmpty()) {
            model.addAttribute("mensagemBusca", "Nenhum funcionário encontrado para o termo: '" + termo + "'.");
        } else if (termo != null && !termo.isEmpty() && !paginaFuncionarios.isEmpty()) {
            model.addAttribute("mensagemBusca", "Exibindo resultados para: '" + termo + "'.");
        }
        return "funcionarios";
    }

    @GetMapping(value = "/editarForm/{idFuncionario}")
    public ModelAndView editarForm(@PathVariable("idFuncionario") Integer idFuncionario) {
        ModelAndView mv = new ModelAndView("alterarFuncionario");
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário inválido: " + idFuncionario));

        FuncionarioDTO funcionarioDTO = converterParaDTO(funcionario);
        mv.addObject("funcionarioDTO", funcionarioDTO);
        return mv;
    }

    @PostMapping(value = "/editar/{idFuncionario}")
    public String editarFuncionario(@ModelAttribute("funcionarioDTO") @Validated FuncionarioDTO funcionarioDTO,
                                    @PathVariable("idFuncionario") Integer idFuncionario,
                                    BindingResult result,
                                    RedirectAttributes attributes, Model model) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("mensagem", "Verifique os campos obrigatórios.");
            attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
            return "redirect:/funcionario/editarForm/" + idFuncionario;
        }

        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário inválido: " + idFuncionario));

        if (!funcionario.getCpf().equals(funcionarioDTO.getCpf())) {
            Optional<Funcionario> funcionarioExistenteComCpf = funcionarioRepository.findByCpf(funcionarioDTO.getCpf());
            if (funcionarioExistenteComCpf.isPresent() && !funcionarioExistenteComCpf.get().getIdFun().equals(idFuncionario)) {
                attributes.addFlashAttribute("mensagem", "Erro ao atualizar: Novo CPF já pertence a outro funcionário.");
                attributes.addFlashAttribute("funcionarioDTO", funcionarioDTO);
                return "redirect:/funcionario/editarForm/" + idFuncionario;
            }
        }

        atualizarDadosFuncionario(funcionario, funcionarioDTO);
        funcionarioRepository.save(funcionario);
        attributes.addFlashAttribute("mensagem", "Funcionário atualizado(a) com sucesso!");
        return "redirect:/funcionario/listar";
    }

    @GetMapping(value = "/deletar/{idFuncionario}")
    public String deletarFuncionario(@PathVariable("idFuncionario") Integer idFuncionario, RedirectAttributes attributes) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário inválido: " + idFuncionario));
        try {
            funcionarioRepository.delete(funcionario);
            attributes.addFlashAttribute("mensagem", "Funcionário removido(a) com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagem", "Erro ao remover funcionário: Pode estar associado a vendas ou ordens de serviço. Detalhe: " + e.getMessage());
        }
        return "redirect:/funcionario/listar";
    }

    public List<FuncionarioDTO> getFuncionarioDTO(List<Funcionario> funcionarios) {
        List<FuncionarioDTO> listaDeDTOs = new ArrayList<>();
        for (Funcionario funcionario : funcionarios) {
            listaDeDTOs.add(converterParaDTO(funcionario));
        }
        return listaDeDTOs;
    }

    // --- NOVOS MÉTODOS API (JSON) ------------------------------------------------------------

    @GetMapping("/api/funcionario/listar")
    @ResponseBody
    public ResponseEntity<Page<FuncionarioDTO>> apiListarFuncionarios(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<Funcionario> paginaFuncionarios = funcionarioRepository.findAll(pageable);
        Page<FuncionarioDTO> paginaDTO = paginaFuncionarios.map(this::converterParaDTO);
        return ResponseEntity.ok(paginaDTO);
    }

    @PostMapping("/api/funcionario/salvar")
    @ResponseBody
    public ResponseEntity<?> apiSalvarFuncionario(@RequestBody FuncionarioDTO funcionarioDTO) {
        try {
            if (funcionarioDTO.getNomeFuncionario() == null || funcionarioDTO.getNomeFuncionario().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("O nome é obrigatório.");
            }

            Funcionario funcionario = new Funcionario();
            // Correção aqui: getIdFun() em vez de getIdFuncionario()
            if (funcionarioDTO.getIdFun() != null) {
                funcionario = funcionarioRepository.findById(funcionarioDTO.getIdFun())
                        .orElse(new Funcionario());
            } else {
                // Validação de CPF apenas na criação (quando não tem ID)
                if (funcionarioRepository.findByCpf(funcionarioDTO.getCpf()).isPresent()) {
                    return ResponseEntity.badRequest().body("CPF já cadastrado.");
                }
            }

            atualizarDadosFuncionario(funcionario, funcionarioDTO);
            Funcionario salvo = funcionarioRepository.save(funcionario);
            return ResponseEntity.ok(converterParaDTO(salvo));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar funcionário: " + e.getMessage());
        }
    }

    @GetMapping("/api/funcionario/{id}")
    @ResponseBody
    public ResponseEntity<FuncionarioDTO> apiBuscarFuncionario(@PathVariable Integer id) {
        return funcionarioRepository.findById(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/funcionario/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeletarFuncionario(@PathVariable Integer id) {
        try {
            if (!funcionarioRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            funcionarioRepository.deleteById(id);
            return ResponseEntity.ok("Deletado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Erro: Não pode excluir pois possui vínculos.");
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void atualizarDadosFuncionario(Funcionario funcionario, FuncionarioDTO funcionarioDTO) {
        funcionario.setNomeFuncionario(funcionarioDTO.getNomeFuncionario());
        funcionario.setCpf(funcionarioDTO.getCpf());

        if (funcionario.getEmail() == null) {
            funcionario.setEmail(new Email());
            funcionario.getEmail().setFuncionario(funcionario);
        }
        funcionario.getEmail().setEndEmail(funcionarioDTO.getEndEmail());

        if (funcionario.getTelefone() == null) {
            funcionario.setTelefone(new Telefone());
            funcionario.getTelefone().setFuncionario(funcionario);
        }
        funcionario.getTelefone().setTelefone(funcionarioDTO.getTelefone());

        if (funcionario.getEndereco() == null) {
            funcionario.setEndereco(new Endereco());
            funcionario.getEndereco().setFuncionario(funcionario);
        }
        funcionario.getEndereco().setRua(funcionarioDTO.getRua());
        funcionario.getEndereco().setCep(funcionarioDTO.getCep());
        funcionario.getEndereco().setBairro(funcionarioDTO.getBairro());
        funcionario.getEndereco().setCidade(funcionarioDTO.getCidade());
        funcionario.getEndereco().setNumeroCasa(funcionarioDTO.getNumeroCasa());
    }

    private FuncionarioDTO converterParaDTO(Funcionario funcionario) {
        FuncionarioDTO dto = new FuncionarioDTO();
        // O setter no DTO se chama setIdFuncionario, mas o getter do model é getIdFun
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