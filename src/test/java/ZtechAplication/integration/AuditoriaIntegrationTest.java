package ZtechAplication.integration;

import ZtechAplication.model.Cliente;
import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.service.AuditoriaService;
import ZtechAplication.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AuditoriaIntegrationTest {

    @Autowired
    private ClienteService clienteService;

    // Dublê do Banco: Controlamos o que o banco vai responder
    @MockBean
    private ClienteRepository clienteRepository;

    // Dublê do Log: Vamos espionar se ele foi chamado
    @MockBean
    private AuditoriaService auditoriaService;

    @Test
    public void deveGravarLogAoAcessarDadosDeCliente() {
        // 1. DADOS DE ENTRADA: IDs fornecidos pela sua tabela de requisitos
        Integer idFuncionario = 60; // Simula o ID do funcionário logado
        Integer idClienteAlvo = 12; // Simula o ID do cliente sendo consultado

        // 2. PREPARAÇÃO: Forçamos o banco a confirmar que o cliente existe
        // Usamos Optional.of(new Object()) para simular que o retorno não é vazio
        when(clienteRepository.findById(idClienteAlvo)).thenReturn(Optional.of(new Cliente()));
        // 3. AÇÃO: O funcionário tenta ler a tabela do cliente
        clienteService.acessarDadosCliente(idClienteAlvo, idFuncionario);

        // 4. VALIDAÇÃO: O módulo de auditoria TEM que ter sido acionado exatamente 1 vez com os IDs corretos
        verify(auditoriaService, times(1)).registrarAcessoLog(idFuncionario, idClienteAlvo);
    }

    @Test
    public void naoDeveGravarLogSeClienteNaoExistir() {
        // 1. PREPARAÇÃO: Forçamos o banco a dizer que não encontrou ninguém (Optional.empty)
        when(clienteRepository.findById(anyInt())).thenReturn(Optional.empty());

        // 2. AÇÃO: Tenta acessar um cliente inexistente
        clienteService.acessarDadosCliente(99, 60);

        // 3. VALIDAÇÃO: Como não leu dados reais, o log NÃO DEVE ser acionado
        verify(auditoriaService, never()).registrarAcessoLog(anyInt(), anyInt());
    }
}