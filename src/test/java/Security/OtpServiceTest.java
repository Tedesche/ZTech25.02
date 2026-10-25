package Security;

import org.junit.jupiter.api.Test;

import ZtechAplication.service.OtpService;

import static org.junit.jupiter.api.Assertions.*;

public class OtpServiceTest {

	@Test
    public void deveGerarTokenDeSeisDigitosNumericos() {

        OtpService otpService = new OtpService();
        

        String token = otpService.generateAndSaveOtp("usuario_teste"); 


        assertNotNull(token);
        assertEquals(6, token.length(), "O token não possui 6 dígitos.");
        assertTrue(token.matches("\\d{6}"), "O token possui caracteres não numéricos.");
    }
}
	



