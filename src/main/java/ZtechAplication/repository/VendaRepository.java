package ZtechAplication.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ZtechAplication.model.Venda;

public interface VendaRepository extends JpaRepository<Venda, Integer>, JpaSpecificationExecutor<Venda> {

    Optional<Venda> findById(Integer id);

    @Query("SELECT v FROM Venda v " +
           "LEFT JOIN FETCH v.produto p " +
           "LEFT JOIN FETCH v.cliente c")
    List<Venda> findAllWithRelationships();
    
    // CORRIGIDO: Usando BETWEEN
    @Query("SELECT COUNT(v) FROM Venda v " +
           "WHERE v.dataInicio BETWEEN :dataInicio AND :dataFim")
    long countByDataInicioBetween(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);
    
    // CORRIGIDO: Usando BETWEEN
    @Query("SELECT SUM(v.lucro) " +
           "FROM Venda v " +
           "WHERE v.dataInicio BETWEEN :dataInicio AND :dataFim")
    BigDecimal sumLucroByDataInicioBetween(
        @Param("dataInicio") LocalDate dataInicio, 
        @Param("dataFim") LocalDate dataFim
    );
}