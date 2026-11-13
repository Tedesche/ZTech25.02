package ZtechAplication.integration;

import ZtechAplication.service.OtpService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
public class Seguranca2faIntegrationTest {

    // Injetamos o serviço de OTP real para ser testado
    @Autowired
    private OtpService otpService;

    // Criamos o dublê apenas para o disparador final do Spring
    @MockBean
    private JavaMailSender mailSender; 

    @Test
    public void deveGerarTokenEColocarNaFilaDeEmailComSucesso() {
        // 1. Preparação dos dados
        String username = "usuario_teste";
        String emailCliente = "cliente@ztech.com.br";

        // 2. Ação: Solicita o token integrado (gera e envia)
        String tokenGerado = otpService.solicitarToken2fa(username, emailCliente);

        // 3. Verificação: Captura a mensagem que chegou na fila do JavaMailSender
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        
        // Verifica se o método send() foi chamado exatamente 1 vez
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage mensagemEnviada = captor.getValue();

        // 4. Validações finais
        assertEquals(emailCliente, mensagemEnviada.getTo()[0], "O e-mail deve ser direcionado ao cliente correto.");
        assertTrue(mensagemEnviada.getText().contains(tokenGerado), "O corpo do texto do e-mail precisa conter o token que foi gerado.");
    }
}