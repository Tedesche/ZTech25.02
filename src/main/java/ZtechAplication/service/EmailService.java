package ZtechAplication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envia um e-mail de texto simples.
     * @param para O e-mail do destinatário.
     * @param assunto O assunto do e-mail.
     * @param texto O corpo do e-mail.
     */
    public void enviarEmailSimples(String para, String assunto, String texto) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ztechsenac@gmail.com"); // <-- Coloque seu e-mail de remetente aqui
            message.setTo(para);
            message.setSubject(assunto);
            message.setText(texto);
            mailSender.send(message);
        } catch (Exception e) {
            // Logar o erro ou lançar uma exceção customizada
            System.err.println("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}