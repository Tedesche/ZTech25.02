package ZtechAplication.service;

import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    
    // Método que registra o rastro de acesso aos dados sensíveis
    public void registrarAcessoLog(Integer idFuncionario, Integer idCliente) {
        // No futuro, isso gravará numa tabela "TB_LOG_AUDITORIA" ou arquivo de texto.
        System.out.println("LOG DE AUDITORIA: Funcionário " + idFuncionario + " acessou dados do Cliente " + idCliente);
    }
}