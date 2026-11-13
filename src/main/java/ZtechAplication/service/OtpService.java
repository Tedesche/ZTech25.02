package ZtechAplication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    // Define um cache para os OTPs que expira após 5 MINUTOS
    private final Cache<String, String> otpCache;
    private final Random random = new Random();

    public OtpService() {
        this.otpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) 
                .build();
    }

    /**
     * Gera um novo OTP de 6 dígitos, salva no cache e o retorna.
     * @param username O nome de usuário (chave do cache)
     * @return O OTP de 6 dígitos como String.
     */
    public String generateAndSaveOtp(String username) {
        // Gera um número entre 100000 e 999999
        int otpInt = 100000 + random.nextInt(900000); 
        String otp = String.valueOf(otpInt);
        
        otpCache.put(username, otp);
        return otp;
    }

    /**
     * Valida o OTP fornecido pelo usuário.
     * @param username O nome de usuário (chave do cache)
     * @param otpFornecido O código que o usuário digitou
     * @return true se o OTP for válido, false caso contrário.
     */
    public boolean validateOtp(String username, String otpFornecido) {
        String otpSalvo = otpCache.getIfPresent(username);

        if (otpSalvo != null && otpSalvo.equals(otpFornecido)) {
            otpCache.invalidate(username); // Remove o OTP após o uso
            return true;
        }
        return false;
    }

    /**
     * Limpa o OTP do cache (ex: se o usuário errar).
     * @param username O nome de usuário (chave do cache)
     */
    public void clearOtp(String username) {
        otpCache.invalidate(username);
    }
 // 1. INSERÇÃO: Injeta o serviço de e-mail dentro do serviço de OTP
    @Autowired
    private EmailService emailService;

    // 2. INSERÇÃO: Novo método que orquestra a geração e o envio
    public String solicitarToken2fa(String username, String emailDestino) {
        
        // Reaproveita o seu método que já existe para gerar e salvar no cache
        String token = this.generateAndSaveOtp(username); 
        
        // Monta e dispara o e-mail
        String assunto = "Seu Código de Acesso 2FA - ZTech";
        String texto = "Olá, " + username + "! Seu código de verificação é: " + token;
        
        emailService.enviarEmailSimples(emailDestino, assunto, texto);
        
        return token;
    }
}
