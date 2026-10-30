package ZtechAplication;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentoUtilTest {

    @Test
    public void deveValidarTodosOsCenariosDeMascararDoc() {
    	
    	new DocumentoUtil();
        
        // Cenário 1: Caminho feliz (CPF válido e com tamanho correto)
        String cpfValido = "123.456.789-00";
        String resultadoValido = DocumentoUtil.mascarar_doc(cpfValido);
        assertEquals("***.456.789-**", resultadoValido, "O CPF não foi mascarado no formato correto.");

        // Cenário 2: Condição de guarda (CPF nulo)
        String resultadoNulo = DocumentoUtil.mascarar_doc(null);
        assertNull(resultadoNulo, "O método deve retornar null de forma segura quando a entrada for null.");

        // Cenário 3: Condição de guarda (CPF com tamanho menor que 14)
        String cpfCurto = "123456";
        String resultadoCurto = DocumentoUtil.mascarar_doc(cpfCurto);
        assertEquals(cpfCurto, resultadoCurto, "O método deve retornar a string original se o tamanho for menor que 14 caracteres.");
    }
}