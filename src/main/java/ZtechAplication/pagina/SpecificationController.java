package ZtechAplication.pagina;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate; 
import java.util.ArrayList; 
import java.util.List; 

import ZtechAplication.model.Cliente;
import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Produto;
import ZtechAplication.model.Venda; 
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter; 
import java.time.format.DateTimeParseException; 


public class SpecificationController {

	// METODO DE BUSCA ESPECIFICADA PARA CLIENTE
	// POR AHORA BUSCA APENAS POR STRING, ENTÃO COLOQUE APENAS CAMPOS DE STRING
	public static Specification<Cliente> comTermoCli (String termo){
		return (root, query, cb) -> {
			if ( termo == null || termo.trim().isEmpty() ) {
				// Retorna todos os clientes se o termo de busca for nulo ou vazio
				return cb.isTrue(cb.literal(true)); 
			} 

			String likeTerm = "%" + termo.toLowerCase() + "%";
			// Garante que os joins com entidades relacionadas sejam feitos para evitar o problema N+1
            // e para permitir a busca em campos dessas entidades.
            // O JoinType.LEFT garante que clientes sem email/telefone/endereço ainda sejam retornados se outros campos corresponderem.
            // Adicionar query.distinct(true) pode ser útil se os joins causarem duplicatas.
            // if (query.getResultType().equals(Cliente.class)) { // Evita fetch em subqueries de contagem
            //     root.fetch("email", jakarta.persistence.criteria.JoinType.LEFT);
            //     root.fetch("telefone", jakarta.persistence.criteria.JoinType.LEFT);
            //     root.fetch("endereco", jakarta.persistence.criteria.JoinType.LEFT);
            // }


			return cb.or( // Combina os critérios de busca com OR
					cb.like(cb.lower(root.get("nomeCliente")), likeTerm), // Busca por nome do cliente
					cb.like(cb.lower(root.get("cpf")), likeTerm), // Busca por CPF
					// Para buscar em campos de entidades relacionadas, é preciso fazer o join (implícito ou explícito)
					cb.like(cb.lower(root.get("email").get("endEmail")), likeTerm), // Busca por email
					cb.like(cb.lower(root.get("telefone").get("telefone")), likeTerm), // Busca por telefone
					cb.like(cb.lower(root.get("endereco").get("bairro")), likeTerm) // Busca por bairro
			);
		};
	}

	// METODO DE BUSCA ESPECIFICADA PARA PRODUTO
	public static Specification<Produto> comTermoProd (String termo){
		return (root, query, cb) -> {
			if ( termo == null || termo.trim().isEmpty() ) {
				// Retorna todos os produtos se o termo de busca for nulo ou vazio
				return cb.isTrue(cb.literal(true)); 
			} 
			
			String likeTerm = "%" + termo.toLowerCase() + "%";
			// Garante que os joins sejam feitos para buscar em Categoria e Marca
            // e para evitar N+1 problemas ao carregar os dados relacionados.
            // if (query.getResultType().equals(Produto.class)) { // Evita fetch em subqueries de contagem
            //    root.fetch("categoria", jakarta.persistence.criteria.JoinType.LEFT);
            //    root.fetch("marca", jakarta.persistence.criteria.JoinType.LEFT);
            // }


			return cb.or( // Combina os critérios de busca com OR
					cb.like(cb.lower(root.get("nome")), likeTerm), // Busca por nome do produto
					cb.like(cb.lower(root.get("descricao")), likeTerm), // Busca por descrição
					cb.like(cb.lower(root.get("categoria").get("nome")), likeTerm), // Busca por nome da categoria
					cb.like(cb.lower(root.get("marca").get("nome")), likeTerm) // Busca por nome da marca
			);
		};
	}

    // NOVO MÉTODO DE BUSCA ESPECIFICADA PARA VENDA
    public static Specification<Venda> comTermoVenda(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) {
                // Retorna todas as vendas se o termo de busca for nulo ou vazio
                return cb.isTrue(cb.literal(true)); 
            }

            // Adiciona JOIN FETCH para Cliente e Produto para otimizar a consulta
            // e permitir filtros em campos dessas entidades relacionadas.
            // O if previne que o fetch seja aplicado em subqueries de contagem (necessário para paginação).
            if (query.getResultType().equals(Venda.class)) { // Só aplica fetch na query principal de seleção
                 root.fetch("cliente", jakarta.persistence.criteria.JoinType.LEFT);
                 root.fetch("produto", jakarta.persistence.criteria.JoinType.LEFT);
            }


            List<Predicate> predicates = new ArrayList<>();
            String likeTerm = "%" + termo.toLowerCase() + "%";

            // Busca por nomes do cliente associado à venda
            predicates.add(cb.like(cb.lower(root.get("cliente").get("nomeCliente")), likeTerm));
            // Busca por nome do produto associado à venda
            predicates.add(cb.like(cb.lower(root.get("produto").get("nome")), likeTerm));

            // Tenta converter o termo para Integer para buscar por ID da Venda
            try {
                Integer idVenda = Integer.parseInt(termo);
                predicates.add(cb.equal(root.get("idVenda"), idVenda));
            } catch (NumberFormatException e) {
                // Se não for um número, ignora esta condição de busca
            }

            // Tenta converter o termo para LocalDate para buscar por dataInicio
            // Suporta os formatos "yyyy-MM-dd" (ISO) e "dd/MM/yyyy"
            try {
                LocalDate dataBusca;
                if (termo.matches("\\d{4}-\\d{2}-\\d{2}")) { // Verifica formato AAAA-MM-DD
                    dataBusca = LocalDate.parse(termo, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (termo.matches("\\d{2}/\\d{2}/\\d{4}")) { // Verifica formato DD/MM/AAAA
                    dataBusca = LocalDate.parse(termo, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (termo.matches("\\d{2}/\\d{2}")) { // Formato DD/MM
                    // Adiciona o ano atual ao termo
                    String termoCompleto = termo + "/" + Year.now().getValue(); //completa o ano da data especulada para o ano atual
                    dataBusca = LocalDate.parse(termoCompleto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else {
                    // Se não corresponder a nenhum formato conhecido, lança exceção para ser capturada
                    throw new DateTimeParseException("Formato de data não suportado para busca", termo, 0);
                }
                predicates.add(cb.equal(root.get("dataInicio"), dataBusca)); // Busca pela data exata
            } catch (DateTimeParseException e) {
                // Se não for uma data em um formato esperado, ignora esta condição de busca
            }

            // Combina todos os predicados com OR. A venda será retornada se corresponder a QUALQUER um dos critérios.
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
    
 // NOVO MÉTODO DE BUSCA ESPECIFICADA PARA VENDA
    public static Specification<OrdemServico> comTermoOS(String termo) {
        return (root, query, cb) -> {
            if (termo == null || termo.trim().isEmpty()) {
                // Retorna todas as vendas se o termo de busca for nulo ou vazio
                return cb.isTrue(cb.literal(true)); 
            }

            // Adiciona JOIN FETCH para Cliente e Produto para otimizar a consulta
            // e permitir filtros em campos dessas entidades relacionadas.
            // O if previne que o fetch seja aplicado em subqueries de contagem (necessário para paginação).
            if (query.getResultType().equals(Venda.class)) { // Só aplica fetch na query principal de seleção
                 root.fetch("cliente", jakarta.persistence.criteria.JoinType.LEFT);
                 root.fetch("servico", jakarta.persistence.criteria.JoinType.LEFT);
                 root.fetch("produto", jakarta.persistence.criteria.JoinType.LEFT);
            }


            List<Predicate> predicates = new ArrayList<>();
            String likeTerm = "%" + termo.toLowerCase() + "%";

            // Busca por nomes do cliente associado à OS
            predicates.add(cb.like(cb.lower(root.get("status")), likeTerm));
            // Busca por nomes do cliente associado à OS
            predicates.add(cb.like(cb.lower(root.get("cliente").get("nomeCliente")), likeTerm));
            // Busca por nomes do cliente associado à OS
            predicates.add(cb.like(cb.lower(root.get("cliente").get("cpf")), likeTerm));
            // Busca por nome do servico associado à OS
            predicates.add(cb.like(cb.lower(root.get("produto").get("nome")), likeTerm));
            // Busca por nome do produto associado à OS
            predicates.add(cb.like(cb.lower(root.get("servico").get("nome")), likeTerm));

            // Tenta converter o termo para Integer para buscar por ID da Venda
            try {
                Integer idOS = Integer.parseInt(termo);
                predicates.add(cb.equal(root.get("idOS"), idOS));
            } catch (NumberFormatException e) {
                // Se não for um número, ignora esta condição de busca
            }

            // Tenta converter o termo para LocalDate para buscar por dataInicio
            // Suporta os formatos "yyyy-MM-dd" (ISO) e "dd/MM/yyyy"
            try {
                LocalDate dataBusca;
                if (termo.matches("\\d{4}-\\d{2}-\\d{2}")) { // Verifica formato AAAA-MM-DD
                    dataBusca = LocalDate.parse(termo, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (termo.matches("\\d{2}/\\d{2}/\\d{4}")) { // Verifica formato DD/MM/AAAA
                    dataBusca = LocalDate.parse(termo, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (termo.matches("\\d{2}/\\d{2}")) { // Formato DD/MM para o ano atual
                    // Adiciona o ano atual ao termo
                    String termoCompleto = termo + "/" + Year.now().getValue();
                    dataBusca = LocalDate.parse(termoCompleto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }else {
                    // Se não corresponder a nenhum formato conhecido, lança exceção para ser capturada
                    throw new DateTimeParseException("Formato de data não suportado para busca", termo, 0);
                }
                predicates.add(cb.equal(root.get("dataInicio"), dataBusca)); // Busca pela data exata
                predicates.add(cb.equal(root.get("dataFim"), dataBusca)); 
            } catch (DateTimeParseException e) {
                // Se não for uma data em um formato esperado, ignora esta condição de busca
            }

            // Combina todos os predicados com OR. A venda será retornada se corresponder a QUALQUER um dos critérios.
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
    
    
    public static Specification<Produto> comFiltroSequencial(String termoPrincipal, String termoSecundario) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            System.out.println("Nome recebido: " + termoPrincipal);
            System.out.println("Nome recebido: " + termoSecundario);
//            termoPrincipal = "celulares";
            // Aplica o primeiro filtro (termo principal)
            if (termoPrincipal != null && !termoPrincipal.trim().isEmpty()) {
                String likeTermPrincipal = "%" + termoPrincipal.toLowerCase() + "%";
                Predicate principalPredicate = cb.or(
                    cb.like(cb.lower(root.get("nome")), likeTermPrincipal),
                    cb.like(cb.lower(root.get("descricao")), likeTermPrincipal),
                    cb.like(cb.lower(root.get("categoria").get("nome")), likeTermPrincipal),
                    cb.like(cb.lower(root.get("marca").get("nome")), likeTermPrincipal)
                );
                predicates.add(principalPredicate);
            }
            
            // Aplica o segundo filtro APENAS nos resultados do primeiro
            if (termoSecundario != null && !termoSecundario.trim().isEmpty()) {
                String likeTermSecundario = "%" + termoSecundario.toLowerCase() + "%";
                Predicate secundarioPredicate = cb.or(
                    cb.like(cb.lower(root.get("nome")), likeTermSecundario),
                    cb.like(cb.lower(root.get("descricao")), likeTermSecundario),
                    cb.like(cb.lower(root.get("categoria").get("nome")), likeTermSecundario),
                    cb.like(cb.lower(root.get("marca").get("nome")), likeTermSecundario)
                );
                predicates.add(secundarioPredicate);
            }
            
            if (predicates.isEmpty()) {
                return cb.isTrue(cb.literal(true));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    
    
}