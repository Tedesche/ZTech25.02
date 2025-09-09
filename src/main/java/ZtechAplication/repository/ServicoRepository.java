package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ZtechAplication.model.Produto;
import ZtechAplication.model.Servico;

public interface ServicoRepository extends CrudRepository<Servico, Integer>, JpaSpecificationExecutor<Produto>  {
	
	
	
	Optional<Servico> findById(Integer idProduto);
}
