package ZtechAplication.pagina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatusLibrary {

	private static final LinkedHashMap<Integer, String> CODIGO_PARA_TEXTO = new LinkedHashMap<>();
    private static final Map<String, Integer> TEXTO_PARA_CODIGO = new HashMap<>();
    private static final List<String> ORDEM_DESCRICOES = new ArrayList<>();

    static {
        adicionarStatus(1, "Registrada");
        adicionarStatus(2, "Em Andamento");
        adicionarStatus(3, "Concluido");
        adicionarStatus(4, "Cancelado");
        adicionarStatus(5, "Cliente Ausente");
    }

    private static void adicionarStatus(int codigo, String descricao) {
        CODIGO_PARA_TEXTO.put(codigo, descricao);
        TEXTO_PARA_CODIGO.put(descricao.toLowerCase(), codigo);
        ORDEM_DESCRICOES.add(descricao);
    }

    // Novo método para obter a próxima descrição
    public static String getProximaDescricao(String descricaoAtual) {
        String descricaoNormalizada = descricaoAtual.toLowerCase();
        
        if (!TEXTO_PARA_CODIGO.containsKey(descricaoNormalizada)) {
            throw new IllegalArgumentException("Descrição inválida: " + descricaoAtual);
        }

        int indexAtual = ORDEM_DESCRICOES.indexOf(descricaoAtual);
        int proximoIndex = (indexAtual + 1) % ORDEM_DESCRICOES.size();
        
        return ORDEM_DESCRICOES.get(proximoIndex);
    }
    
    // MÉTODO QUE ESTÁ SENDO CHAMADO NO CONTROLLER:
    // Método para obter todas as descrições de status na ordem definida
    public static List<String> getAllStatusDescriptions() {
        return new ArrayList<>(ORDEM_DESCRICOES); // Retorna uma cópia da lista de descrições ordenadas
    }

    // Métodos anteriores mantidos
    public static String getStatusDescricao(int codigo) {
        return CODIGO_PARA_TEXTO.getOrDefault(codigo, "Desconhecido");
    }

    public static int getCodigo(String descricao) {
        Integer codigo = TEXTO_PARA_CODIGO.get(descricao.toLowerCase());
        if (codigo == null) {
            throw new IllegalArgumentException("Descrição inválida: " + descricao);
        }
        return codigo;
    }
}