package ZtechAplication.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import ZtechAplication.model.Email;
import ZtechAplication.model.Usuario;
import ZtechAplication.service.EmailService; 
import ZtechAplication.service.OtpService; 
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // Injeta os serviços que acabamos de criar
    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. Pega o usuário que acabou de logar
        Usuario usuario = (Usuario) authentication.getPrincipal();

        // 2. Gera e salva um OTP para esse usuário
        String otp = otpService.generateAndSaveOtp(usuario.getUsername());

        // 3. Envia o e-mail (Verifique sua lógica: seu usuário tem um Funcionario?)
        // Estou usando a lógica do seu arquivo original.
        if (usuario.getFuncionario() != null && usuario.getFuncionario().getEmail() != null) {
            Email emailDoFuncionario = usuario.getFuncionario().getEmail();
            
            String assunto = "ZTech Pro - Seu código de verificação";
            String texto = "Olá, " + usuario.getFuncionario().getNomeFuncionario() + ".\n\n"
                         + "Seu código de login (OTP) é: " + otp + "\n\n"
                         + "Este código expira em 5 minutos.";
            
            emailService.enviarEmailSimples(emailDoFuncionario.getEndEmail(), assunto, texto);
        } else {
            System.err.println("Usuário " + usuario.getUsername() + " não possui e-mail cadastrado para 2FA.");
            // Você pode redirecionar para uma página de erro aqui
        }

        // 4. "Rebaixa" a autenticação para uma role temporária.
        // Isso impede que o usuário acesse o /inicio sem verificar o OTP.
        Authentication preAuth = new UsernamePasswordAuthenticationToken(
                usuario, 
                null, // Não precisamos mais da senha
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PRE_AUTH")) // Role temporária
        );
        SecurityContextHolder.getContext().setAuthentication(preAuth);

        // 5. Redireciona para a nova página de verificação
        response.sendRedirect("/login-verificacao");
    }
}