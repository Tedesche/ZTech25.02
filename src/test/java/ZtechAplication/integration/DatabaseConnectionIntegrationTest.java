package ZtechAplication.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// A anotação @SpringBootTest é o que transforma isso num teste de integração. 
// Ela sobe toda a aplicação (incluindo o banco H2) antes de rodar o teste.
@SpringBootTest
public class DatabaseConnectionIntegrationTest {

    // O Spring injeta automaticamente a ferramenta de conexão com o banco
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void deveConectarAoBancoERealizarSelect1() {
        // Passos do Teste: Inicializar pool de conexões e Realizar SELECT 1
        Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        // Resultado Esperado: O banco deve responder com sucesso
        assertNotNull(resultado, "A conexão falhou, o resultado da query foi nulo.");
        assertEquals(1, resultado, "O banco de dados não retornou o valor 1 esperado.");
    }
}