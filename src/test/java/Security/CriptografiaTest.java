package Security;


import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

public class CriptografiaTest {

    @Test
    public void deveGerarHashValidoEIrreversivel() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaPlana = "SenhaSegura123";
        
        
        String hash = encoder.encode(senhaPlana);
        
        
        assertNotEquals(senhaPlana, hash, "O hash gerado não pode ser igual à senha plana.");
        assertTrue(encoder.matches(senhaPlana, hash), "O hash deve ser válido e reconhecido pelo encoder.");
    }
}