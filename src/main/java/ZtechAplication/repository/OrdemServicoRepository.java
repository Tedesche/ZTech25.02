package ZtechAplication.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ZtechAplication.model.OrdemServico;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Integer>, JpaSpecificationExecutor<OrdemServico> {

    Optional<OrdemServico> findById(Integer id);

    @Query("SELECT o FROM OrdemServico o " +
           "LEFT JOIN FETCH o.produto p " +
           "LEFT JOIN FETCH o.servico s " +
           "LEFT JOIN FETCH o.cliente c")
    List<OrdemServico> findAllWithRelationships();
    
    // CORREÇÃO: Busca por intervalo de datas (Data Inicial até Data Final)
    @Query("SELECT COUNT(os) FROM OrdemServico os " +
           "WHERE os.dataInicio BETWEEN :dataInicio AND :dataFim")
    long countByDataInicioBetween(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);
    
    // CORREÇÃO: Soma lucro por intervalo de datas
    @Query("SELECT SUM(os.lucro) " +
           "FROM OrdemServico os " +
           "WHERE os.status = :status " +
           "  AND os.dataFim BETWEEN :dataInicio AND :dataFim")
    BigDecimal sumLucroConcluidoByDataFimBetween(
        @Param("status") String status, 
        @Param("dataInicio") LocalDate dataInicio, 
        @Param("dataFim") LocalDate dataFim
    );
}