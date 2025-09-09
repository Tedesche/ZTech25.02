package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Venda;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Integer>, JpaSpecificationExecutor<OrdemServico> {

	Optional<OrdemServico> findById(Integer id); //

    @Query("SELECT o FROM OrdemServico o " +
           "LEFT JOIN FETCH o.produto p " +
           "LEFT JOIN FETCH o.servico s " +
           "LEFT JOIN FETCH o.cliente c")
    List<OrdemServico> findAllWithRelationships();
	
}
