package model;

import org.junit.jupiter.api.Test;

import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.StatusInvalidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrdemServicoTest {

    @Test
    public void deveRealizarMudancaDeStatusValida() {
        // 1. Instancia a OS existente
        OrdemServico os = new OrdemServico();
        
        // 2. Executa o método set_status() (usando o padrão Java)
        os.setStatus("Em Andamento");
        os.setStatus("Concluído");

        // 3. Valida se o status da OS foi atualizado na memória
        assertEquals("Concluído", os.getStatus(), "O status não foi atualizado corretamente.");
    }
    @Test
    public void deveBloquearAlteracaoEmOsCancelada() {
        OrdemServico os = new OrdemServico();
        os.setStatus("Cancelada"); // Define o estado inicial 

        assertThrows(StatusInvalidException.class, () -> {
            os.setStatus("Em Andamento"); // Tenta burlar a regra
        }, "O sistema deve lançar StatusInvalidException ao tentar reviver uma OS cancelada.");
    }
}