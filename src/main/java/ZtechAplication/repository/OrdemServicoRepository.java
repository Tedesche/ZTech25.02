package ZtechAplication.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import ZtechAplication.model.OrdemServico;
import ZtechAplication.model.Venda;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Integer>, JpaSpecificationExecutor<OrdemServico> {

	Optional<OrdemServico> findById(Integer id); //

    @Query("SELECT o FROM OrdemServico o " +
           "LEFT JOIN FETCH o.produto p " +
           "LEFT JOIN FETCH o.servico s " +
           "LEFT JOIN FETCH o.cliente c")
    List<OrdemServico> findAllWithRelationships();
	
    @Query("SELECT COUNT(os) FROM OrdemServico os " +
    	       "WHERE YEAR(os.dataInicio) = :ano AND MONTH(os.dataInicio) = :mes")
    	long countByYearAndMonth(@Param("ano") int ano, @Param("mes") int mes);
    
 // ... dentro da interface OrdemServicoRepository
    @Query("SELECT SUM(os.lucro) " +
           "FROM OrdemServico os " +
           "WHERE os.status = :status " +
           "  AND YEAR(os.dataFim) = :ano " +
           "  AND MONTH(os.dataFim) = :mes")
    BigDecimal sumLucroConcluidoByYearAndMonth(
        @Param("status") String status, 
        @Param("ano") int ano, 
        @Param("mes") int mes
    );
}
