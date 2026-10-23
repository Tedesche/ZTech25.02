package ZtechAplication;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentoUtilTest {

    @Test
    public void deveMascararCpfCorretamente() {
        // Dados de Entrada
        String cpfEntrada = "123.456.789-00";
        String resultadoEsperado = "***.456.789-**";

        // Passos do Teste: Chamar a função
        String resultadoAtual = DocumentoUtil.mascarar_doc(cpfEntrada);

        // Resultado Esperado: Validação
        assertEquals(resultadoEsperado, resultadoAtual, "O CPF não foi mascarado no formato correto.");
    }
}