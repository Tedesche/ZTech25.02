package ZtechAplication.integration;

import ZtechAplication.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class EmailIntegrationTest {

    // Injeta o serviço real que você criou
    @Autowired
    private EmailService emailService;

    // Cria um dublê (Mock) do enviador de e-mails do Spring para não enviar de verdade
    @MockBean
    private JavaMailSender mailSender;

    @Test
    public void deveMontarEEnviarEmailComSucesso() {
        // 1. Dados de entrada
        String destinatario = "cliente@teste.com";
        String assunto = "Lembrete de Serviço";
        String texto = "Sua OS foi concluída.";

        // 2. Executar o método
        emailService.enviarEmailSimples(destinatario, assunto, texto);

        // 3. Capturar o e-mail que o seu serviço tentou enviar para a fila do JavaMailSender
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage mensagemEnviada = captor.getValue();

        // 4. Validar se os dados foram montados corretamente no objeto de envio
        assertEquals(destinatario, mensagemEnviada.getTo()[0], "O destinatário deve coincidir.");
        assertEquals(assunto, mensagemEnviada.getSubject(), "O assunto deve coincidir.");
        assertEquals(texto, mensagemEnviada.getText(), "O corpo do texto deve coincidir.");
        assertEquals("ztechsenac@gmail.com", mensagemEnviada.getFrom(), "O remetente padrão deve ser o e-mail da ZTech.");
    }

    @Test
    public void deveTratarFalhaNoServidorSmtpSemQuebrarAplicacao() {
        // 1. Simular uma queda do servidor: Forçamos o Mock a lançar um erro ao tentar enviar
        doThrow(new RuntimeException("Simulação: Servidor SMTP Fora do Ar"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // 2. Executar o método e validar se a exceção foi capturada pelo bloco try-catch da sua classe
        assertDoesNotThrow(() -> {
            emailService.enviarEmailSimples("cliente@teste.com", "Teste de Falha", "Texto");
        }, "O serviço de e-mail não deve repassar a exceção para o sistema, deve tratá-la internamente (catch).");
        
        // 3. Verificar se ele realmente tentou enviar antes de dar erro
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}