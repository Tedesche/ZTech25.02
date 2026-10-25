package model;

import org.junit.jupiter.api.Test;

import ZtechAplication.model.OrdemServico;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}