package model;

import org.junit.jupiter.api.Test;

import ZtechAplication.model.Venda;

import static org.junit.jupiter.api.Assertions.*;

public class VendaTest {

    @Test
    public void deveCalcularLucroBrutoCorretamente() {
        Venda venda = new Venda();
        double lucro = venda.calculaLucro(100.0, 60.0);
        
        assertEquals(40.0, lucro, 0.001, "O lucro retornado deve ser o valor exato de 40.");
    }

    @Test
    public void deveImpedirMargemDeLucroNegativa() {
        Venda venda = new Venda();
        
        assertThrows(IllegalArgumentException.class, () -> {
            venda.calculaLucro(50.0, 60.0);
        }, "Deve lançar uma exceção ao tentar processar uma venda com prejuízo.");
    }
}