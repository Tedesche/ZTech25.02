package ZtechAplication.pagina;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import ZtechAplication.model.Cliente;
import ZtechAplication.model.Funcionario;
import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Venda;

public class SpecificationController {

    // =================================================================================
    // 1. CLIENTE
    // =================================================================================
    public static Specification<Cliente> comTermoCli(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) return cb.isTrue(cb.literal(true));
            String likeTerm = "%" + termo.toLowerCase() + "%";

            // Usa JoinType.LEFT para não excluir clientes que tenham email/endereço nulos
            return cb.or(
                cb.like(cb.lower(root.get("nomeCliente")), likeTerm),
                cb.like(cb.lower(root.get("cpf")), likeTerm),
                cb.like(cb.lower(root.join("email", JoinType.LEFT).get("endEmail")), likeTerm),
                cb.like(cb.lower(root.join("telefone", JoinType.LEFT).get("telefone")), likeTerm),
                cb.like(cb.lower(root.join("endereco", JoinType.LEFT).get("bairro")), likeTerm)
            );
        };
    }

    // =================================================================================
    // 2. FUNCIONÁRIO
    // =================================================================================
    public static Specification<Funcionario> comTermoFun(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) return cb.isTrue(cb.literal(true));
            String likeTerm = "%" + termo.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("nomeFuncionario")), likeTerm),
                cb.like(cb.lower(root.get("cpf")), likeTerm),
                cb.like(cb.lower(root.join("email", JoinType.LEFT).get("endEmail")), likeTerm),
                cb.like(cb.lower(root.join("telefone", JoinType.LEFT).get("telefone")), likeTerm),
                cb.like(cb.lower(root.join("endereco", JoinType.LEFT).get("bairro")), likeTerm)
            );
        };
    }

    // =================================================================================
    // 3. PRODUTO (BUSCA SIMPLES + FILTROS AVANÇADOS)
    // =================================================================================
    
    // Método antigo (apenas texto) - Mantido para compatibilidade se necessário
    public static Specification<Produto> comTermoProd(String termo) {
        return comFiltrosProduto(termo, null, null);
    }

    /**
     * Filtro Avançado de Produtos: Combina Busca Texto + Categoria + Marca
     */
    public static Specification<Produto> comFiltrosProduto(String termo, Integer idCategoria, Integer idMarca) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro por Termo (Barra de Pesquisa) - Lógica OR
            if (termo != null && !termo.trim().isEmpty()) {
                String likeTerm = "%" + termo.toLowerCase() + "%";
                Predicate buscaTexto = cb.or(
                    cb.like(cb.lower(root.get("nome")), likeTerm),
                    cb.like(cb.lower(root.get("descricao")), likeTerm),
                    cb.like(cb.lower(root.get("categoria").get("nome")), likeTerm), // join implícito
                    cb.like(cb.lower(root.get("marca").get("nome")), likeTerm)      // join implícito
                );
                predicates.add(buscaTexto);
            }

            // 2. Filtro Exato por Categoria (Dropdown) - Lógica AND
            if (idCategoria != null) {
                predicates.add(cb.equal(root.get("categoria").get("idCategoria"), idCategoria));
            }

            // 3. Filtro Exato por Marca (Dropdown) - Lógica AND
            if (idMarca != null) {
                predicates.add(cb.equal(root.get("marca").get("idMarca"), idMarca));
            }

            if (predicates.isEmpty()) return cb.isTrue(cb.literal(true));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =================================================================================
    // 4. VENDA
    // =================================================================================
    public static Specification<Venda> comTermoVenda(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) return cb.isTrue(cb.literal(true));

            // Otimização de performance (Fetch), mas apenas na query principal
            if (query.getResultType().equals(Venda.class)) {
                root.fetch("cliente", JoinType.LEFT);
                root.fetch("produto", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            String likeTerm = "%" + termo.toLowerCase() + "%";

            // Busca Texto (Nome Cliente / Nome Produto)
            predicates.add(cb.like(cb.lower(root.join("cliente", JoinType.LEFT).get("nomeCliente")), likeTerm));
            predicates.add(cb.like(cb.lower(root.join("produto", JoinType.LEFT).get("nome")), likeTerm));

            // Busca ID e Data (Métodos auxiliares abaixo)
            adicionarBuscaPorId(termo, root.get("idVenda"), predicates, cb);
            adicionarBuscaPorData(termo, root.get("dataInicio"), predicates, cb);

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    // =================================================================================
    // 5. ORDEM DE SERVIÇO
    // =================================================================================
    public static Specification<OrdemServico> comTermoOS(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) return cb.isTrue(cb.literal(true));

            // Correção: Agora verifica OrdemServico.class (antes estava Venda.class)
            if (query.getResultType().equals(OrdemServico.class)) {
                root.fetch("cliente", JoinType.LEFT);
                root.fetch("servico", JoinType.LEFT);
                root.fetch("produto", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            String likeTerm = "%" + termo.toLowerCase() + "%";

            // Busca Texto (Status, Cliente, Produto, Serviço)
            predicates.add(cb.like(cb.lower(root.get("status")), likeTerm));
            
            var joinCliente = root.join("cliente", JoinType.LEFT);
            predicates.add(cb.like(cb.lower(joinCliente.get("nomeCliente")), likeTerm));
            predicates.add(cb.like(cb.lower(joinCliente.get("cpf")), likeTerm));
            
            predicates.add(cb.like(cb.lower(root.join("produto", JoinType.LEFT).get("nome")), likeTerm));
            predicates.add(cb.like(cb.lower(root.join("servico", JoinType.LEFT).get("nome")), likeTerm));

            // Busca ID e Datas
            adicionarBuscaPorId(termo, root.get("idOS"), predicates, cb);
            adicionarBuscaPorData(termo, root.get("dataInicio"), predicates, cb);
            adicionarBuscaPorData(termo, root.get("dataFim"), predicates, cb);

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    // =================================================================================
    // MÉTODOS AUXILIARES (PRIVADOS)
    // =================================================================================

    private static void adicionarBuscaPorId(String termo, jakarta.persistence.criteria.Path<?> pathId, 
                                            List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb) {
        try {
            Integer id = Integer.parseInt(termo);
            predicates.add(cb.equal(pathId, id));
        } catch (NumberFormatException ignored) { }
    }

    private static void adicionarBuscaPorData(String termo, jakarta.persistence.criteria.Path<LocalDate> pathData, 
                                              List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb) {
        try {
            LocalDate dataBusca = null;
            if (termo.matches("\\d{4}-\\d{2}-\\d{2}")) { // AAAA-MM-DD
                dataBusca = LocalDate.parse(termo, DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (termo.matches("\\d{2}/\\d{2}/\\d{4}")) { // DD/MM/AAAA
                dataBusca = LocalDate.parse(termo, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (termo.matches("\\d{2}/\\d{2}")) { // DD/MM (Ano Atual)
                String termoCompleto = termo + "/" + Year.now().getValue();
                dataBusca = LocalDate.parse(termoCompleto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }

            if (dataBusca != null) {
                predicates.add(cb.equal(pathData, dataBusca));
            }
        } catch (DateTimeParseException ignored) { }
    }
}