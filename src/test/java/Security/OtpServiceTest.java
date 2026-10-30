package Security;

import org.junit.jupiter.api.Test;
import ZtechAplication.service.OtpService;
import static org.junit.jupiter.api.Assertions.*;

public class OtpServiceTest {

    @Test
    public void deveValidarTodosOsCenariosDoOtpService() {
        OtpService otpService = new OtpService();
        String username = "usuario_teste";

        // Cenário 1: Gerar token e validar formatação (6 dígitos numéricos)
        String token = otpService.generateAndSaveOtp(username);
        assertNotNull(token);
        assertEquals(6, token.length(), "O token não possui 6 dígitos.");
        assertTrue(token.matches("\\d{6}"), "O token possui caracteres não numéricos.");

        // Cenário 2: Falhar validação com OTP incorreto (cobre if com equals falso)
        boolean isValidoComErro = otpService.validateOtp(username, "000000");
        assertFalse(isValidoComErro, "A validação deve retornar false para um OTP incorreto.");

        // Cenário 3: Validar OTP com sucesso e testar invalidação automática (cobre caminho feliz)
        boolean isValid = otpService.validateOtp(username, token);
        assertTrue(isValid, "O OTP correto deveria ser validado com sucesso.");
        
        // Cenário 4: Garantir que o OTP foi invalidado após o primeiro uso
        boolean isAindaValido = otpService.validateOtp(username, token);
        assertFalse(isAindaValido, "O OTP deve ser invalidado e removido do cache após o uso.");

        // Cenário 5: Falhar validação sem OTP no cache (cobre if com otpSalvo == null)
        boolean isValidoSemCache = otpService.validateOtp("usuario_desconhecido", "123456");
        assertFalse(isValidoSemCache, "A validação deve retornar false se não houver OTP gerado.");

        // Cenário 6: Limpar OTP do cache manualmente (cobre o método clearOtp)
        String tokenParaLimpeza = otpService.generateAndSaveOtp("usuario_limpeza");
        otpService.clearOtp("usuario_limpeza");
        boolean isValidoAposLimpeza = otpService.validateOtp("usuario_limpeza", tokenParaLimpeza);
        assertFalse(isValidoAposLimpeza, "A validação deve falhar, pois o OTP foi limpo manualmente do cache.");
    }
}