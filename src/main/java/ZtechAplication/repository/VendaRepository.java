package ZtechAplication.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import ZtechAplication.model.Venda;

public interface VendaRepository extends JpaRepository<Venda, Integer>, JpaSpecificationExecutor<Venda> {

    Optional<Venda> findById(Integer id); //

    @Query("SELECT v FROM Venda v " +
           "LEFT JOIN FETCH v.produto p " +
           "LEFT JOIN FETCH v.cliente c")
    List<Venda> findAllWithRelationships();
    
    @Query("SELECT COUNT(v) FROM Venda v " +
    	       "WHERE YEAR(v.dataInicio) = :ano AND MONTH(v.dataInicio) = :mes")
    	long countByYearAndMonth(@Param("ano") int ano, @Param("mes") int mes);
    
 // ... dentro da interface VendaRepository
    @Query("SELECT SUM(v.lucro) " +
           "FROM Venda v " +
           "WHERE YEAR(v.dataInicio) = :ano " +
           "  AND MONTH(v.dataInicio) = :mes")
    BigDecimal sumLucroByYearAndMonth(
        @Param("ano") int ano, 
        @Param("mes") int mes
    );
}