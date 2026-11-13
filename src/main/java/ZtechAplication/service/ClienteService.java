package ZtechAplication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ZtechAplication.repository.ClienteRepository;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    // Método que simula a leitura exigida pela tabela de testes
    public boolean acessarDadosCliente(Integer idCliente, Integer idFuncionarioLogado) {
        // Verifica se o cliente existe no banco de dados (o repository retorna um Optional)
        boolean clienteExiste = clienteRepository.findById(idCliente).isPresent();

        if (clienteExiste) {
            // REGRA LGPD: Se os dados foram encontrados e lidos, dispara o log obrigatoriamente
            auditoriaService.registrarAcessoLog(idFuncionarioLogado, idCliente);
        }
        
        return clienteExiste;
    }
}