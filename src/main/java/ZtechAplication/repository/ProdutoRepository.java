package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository; // Alterado aqui
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.CrudRepository; // Removido

import ZtechAplication.model.Produto;

public interface ProdutoRepository extends 
				JpaRepository<Produto, Integer>, JpaSpecificationExecutor<Produto> { // Alterado CrudRepository para JpaRepository
	
    // O método findById(Integer idProduto) já é fornecido pelo JpaRepository.
    // Se não houver customização, pode ser removido para evitar redundância.
    @Override // Boa prática adicionar Override se estiver sobrescrevendo um método da interface pai
	Optional<Produto> findById(Integer idProduto);

    @Query("SELECT p FROM Produto p "
			+ "LEFT JOIN FETCH p.marca m " // Adicionado alias m
			+ "LEFT JOIN FETCH p.categoria c") // Adicionado alias c
    List<Produto> findAllWithRelationships(); // Este método é bom para listagens não paginadas.
                                            // Para listagens paginadas, o fetch pode ser feito na Specification ou na query do findAll(Pageable).
}