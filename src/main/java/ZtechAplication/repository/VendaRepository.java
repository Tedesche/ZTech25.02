package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ZtechAplication.model.Venda;

public interface VendaRepository extends JpaRepository<Venda, Integer>, JpaSpecificationExecutor<Venda> {

    Optional<Venda> findById(Integer id); //

    @Query("SELECT v FROM Venda v " +
           "LEFT JOIN FETCH v.produto p " +
           "LEFT JOIN FETCH v.cliente c")
    List<Venda> findAllWithRelationships();
}