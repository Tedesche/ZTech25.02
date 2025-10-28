package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository; // Alterado aqui
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
// import org.springframework.data.jpa.repository.Modifying; // Removido se não usado
import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.CrudRepository; // Removido
// import org.springframework.data.repository.query.Param; // Removido se não usado

import ZtechAplication.model.Funcionario;

public interface FuncionarioRepository extends 
				JpaRepository<Funcionario, Integer>, JpaSpecificationExecutor<Funcionario>{ // Alterado CrudRepository para JpaRepository

	@Query("SELECT c FROM Funcionario c "
			+ "LEFT JOIN FETCH c.email "
			+ "LEFT JOIN FETCH c.telefone "
			+ "LEFT JOIN FETCH c.endereco")
    List<Funcionario> findAllWithRelationships(); // Bom para listagens não paginadas ou quando todos os dados são necessários.
	
    // O método findById(Integer id) já é fornecido pelo JpaRepository.
    // Pode ser removido se não houver customização.
	@Override // Boa prática se estiver sobrescrevendo
	Optional<Funcionario> findById(Integer id);
	
	Optional<Funcionario> findByCpf(String cpf);
	// O método deleteByCpf precisaria de @Modifying e @Transactional se fosse uma query de deleção customizada.
    // Se for para usar o delete padrão, você buscaria pelo CPF e depois chamaria delete(cliente).
	// Long deleteByCpf(String cpf); // Assinatura de deleteBy... geralmente retorna long (número de deletados) ou void.
}