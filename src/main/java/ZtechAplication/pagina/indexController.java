package ZtechAplication.pagina;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; 
import org.springframework.web.bind.annotation.RequestMapping;

import ZtechAplication.repository.ClienteRepository;
import ZtechAplication.repository.OrdemServicoRepository;
import ZtechAplication.repository.ProdutoRepository;
import ZtechAplication.model.OrdemServico; 

import java.math.BigDecimal; 
// Removido: import java.text.NumberFormat; pois a formatação será no Thymeleaf
import java.time.LocalDate; 
import java.time.YearMonth; 
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList; 
import java.util.List;
// Removido: import java.util.Locale; 
import java.util.Map;
import java.util.stream.Collectors;
// Removido: import java.util.Comparator; // Se não estiver sendo usado para ordenação explícita aqui
import java.util.LinkedHashMap; 

@Controller
public class indexController {

    @Autowired
    private ProdutoRepository produtoRepository; // Repositório para Clientes

    @Autowired
    private ClienteRepository clienteRepository; // Repositório para Produtos

    @Autowired
    private OrdemServicoRepository ordemServicoRepository; // Repositório para Ordens de Serviço

	@RequestMapping("/")
	public String index() {
		return "index"; // Retorna a página de login/entrada inicial
	}
	
    // Método para carregar dados para a página inicial/dashboard
	@RequestMapping("/inicio")
	public String inicio(Model model) { 
        // 1. Total de produtos cadastrados
        long totalProdutos = produtoRepository.count();
        model.addAttribute("totalProdutos", totalProdutos);

        // 2. Total de clientes cadastrados
        long totalClientes = clienteRepository.count();
        model.addAttribute("totalClientes", totalClientes);

        // 3. Processamento de Ordens de Serviço
        List<OrdemServico> todasAsOS = ordemServicoRepository.findAllWithRelationships(); // Busca todas as OS com seus relacionamentos
        model.addAttribute("totalOS", todasAsOS.size()); // Adiciona o total de OS ao modelo

        // 3.1 Contagem Total de OS por Status (para o card e gráfico de status)
        // Pega a lista de todos os status definidos para garantir que todos apareçam no dashboard
        List<String> todosOsStatusDefinidos = StatusLibrary.getAllStatusDescriptions();
        
        // Agrupa as OS por status e conta quantas existem em cada status
        Map<String, Long> contagemTotalPorStatusRaw = todasAsOS.stream()
            .filter(os -> os.getStatus() != null) // Considera apenas OS que têm um status definido
            .collect(Collectors.groupingBy(
                OrdemServico::getStatus, // Agrupa pelo valor do campo 'status'
                Collectors.counting()    // Conta o número de OS em cada grupo de status
            ));

        // Garante que todos os status definidos na StatusLibrary apareçam, mesmo que a contagem seja 0
        Map<String, Long> contagemTotalPorStatusOrdenado = new LinkedHashMap<>();
        for (String statusDefinido : todosOsStatusDefinidos) {
            contagemTotalPorStatusOrdenado.put(statusDefinido, contagemTotalPorStatusRaw.getOrDefault(statusDefinido, 0L));
        }
        
        model.addAttribute("contagemTotalPorStatus", contagemTotalPorStatusOrdenado); // Para os cards de status
        // Adiciona listas separadas de labels (nomes dos status) e dados (contagens) para o gráfico de status
        model.addAttribute("labelsGraficoStatusOs", new ArrayList<>(contagemTotalPorStatusOrdenado.keySet()));
        model.addAttribute("dadosGraficoStatusOs", new ArrayList<>(contagemTotalPorStatusOrdenado.values()));


        // 3.2 Cards Financeiros das Ordens de Serviço
        BigDecimal valorTotalOSAbertasDecimal = BigDecimal.ZERO; // Inicializa o valor total de OS abertas
        // Itera sobre todas as OS para calcular o valor total daquelas com status "Registrada" ou "Em Andamento"
        for (OrdemServico os : todasAsOS) {
            if (os.getStatus() != null && 
                (os.getStatus().equals(StatusLibrary.getStatusDescricao(1)) /*"Registrada"*/ || 
                 os.getStatus().equals(StatusLibrary.getStatusDescricao(2)) /*"Em Andamento"*/ )) {
                if (os.getValor() != null) { // Verifica se o valor da OS não é nulo
                    valorTotalOSAbertasDecimal = valorTotalOSAbertasDecimal.add(os.getValor());
                }
            }
        }
        model.addAttribute("valorTotalOSAbertas", valorTotalOSAbertasDecimal); // Envia o BigDecimal para o template

        YearMonth mesAtual = YearMonth.now(); // Obtém o ano e mês atuais
        BigDecimal lucroTotalOSConcluidasMesDecimal = BigDecimal.ZERO; // Inicializa o lucro total de OS concluídas no mês
        // Itera sobre todas as OS para calcular o lucro daquelas concluídas no mês atual
        for (OrdemServico os : todasAsOS) {
            if (os.getStatus() != null && os.getStatus().equals(StatusLibrary.getStatusDescricao(3)) /*"Concluido"*/ &&
                os.getDataFim() != null && YearMonth.from(os.getDataFim()).equals(mesAtual)) { // Verifica se a OS foi concluída no mês atual
                if (os.getLucro() != null) { // Verifica se o lucro da OS não é nulo
                    lucroTotalOSConcluidasMesDecimal = lucroTotalOSConcluidasMesDecimal.add(os.getLucro());
                }
            }
        }
        model.addAttribute("lucroTotalOSConcluidasMes", lucroTotalOSConcluidasMesDecimal); // Envia o BigDecimal para o template

        // 3.3 Dados para o Gráfico de OS Criadas nos Últimos 7 Dias
        LocalDate hoje = LocalDate.now(); // Data atual
        LocalDate dataInicioPeriodo = hoje.minusDays(6); // Data de 6 dias atrás (para pegar os últimos 7 dias)

        // Agrupa as OS criadas no período por data de início e conta
        Map<LocalDate, Long> osCriadasPorDiaMap = todasAsOS.stream()
            .filter(os -> os.getDataInicio() != null && // Considera apenas OS com data de início definida
                           !os.getDataInicio().isBefore(dataInicioPeriodo) && // Dentro do período dos últimos 7 dias
                           !os.getDataInicio().isAfter(hoje))
            .collect(Collectors.groupingBy(
                OrdemServico::getDataInicio, // Agrupa pela data de início
                Collectors.counting()        // Conta o número de OS em cada dia
            ));

        List<String> labelsGraficoOsRecentes = new ArrayList<>(); // Labels para o eixo X do gráfico (datas)
        List<Long> dadosGraficoOsRecentes = new ArrayList<>();    // Dados para o eixo Y do gráfico (contagens)
        DateTimeFormatter formatterDiaMes = DateTimeFormatter.ofPattern("dd/MM"); // Formato para exibir as datas no gráfico

        // Preenche os dados para cada um dos últimos 7 dias, mesmo que a contagem seja 0
        for (int i = 0; i < 7; i++) {
            LocalDate dia = dataInicioPeriodo.plusDays(i);
            labelsGraficoOsRecentes.add(dia.format(formatterDiaMes)); // Adiciona a data formatada como label
            dadosGraficoOsRecentes.add(osCriadasPorDiaMap.getOrDefault(dia, 0L)); // Adiciona a contagem (ou 0 se não houver OS nesse dia)
        }

        model.addAttribute("labelsGraficoOsRecentes", labelsGraficoOsRecentes);
        model.addAttribute("dadosGraficoOsRecentes", dadosGraficoOsRecentes);

		return "inicio"; // Retorna o nome do template da página inicial
	}

    // Mapeamentos para as outras páginas (redirecionando para os controllers específicos)
	@RequestMapping("/clientes")
	public String clientes() {
        return "redirect:/cliente/listar"; 
	}
	@RequestMapping("/estoque")
	public String estoque() {
        return "redirect:/produto/listar";
	}
	@RequestMapping("/vendas")
	public String vendas() {
        return "redirect:/vendas/listar";
	}
	@RequestMapping("/ordens")
	public String ordens() {
        return "redirect:/ordens/listar";
	}
	@RequestMapping("/cadastro_cliente")
	public String cadastro_cliente() {
        return "redirect:/cliente/cadastrarForm";
	}
	@RequestMapping("/cadastro_produto")
	public String cadastro_produto() {
        return "redirect:/produto/cadastrarForm";
	}
	@RequestMapping("/cadastro_OS")
	public String cadastro_OS() {
        return "redirect:/ordens/cadastrarForm";
	}
}