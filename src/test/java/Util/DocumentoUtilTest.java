package Util;

import org.junit.jupiter.api.Test;

import ZtechAplication.DocumentoUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentoUtilTest {

    @Test
    public void deveMascararCpfCorretamente() {
        
        String cpfEntrada = "123.456.789-00";
        String resultadoEsperado = "***.456.789-**";

        
        String resultadoAtual = DocumentoUtil.mascarar_doc(cpfEntrada);

        
        assertEquals(resultadoEsperado, resultadoAtual, "O CPF não foi mascarado no formato correto.");
    }
}