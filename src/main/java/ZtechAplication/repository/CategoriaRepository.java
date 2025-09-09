package ZtechAplication.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import ZtechAplication.model.Categoria;
import ZtechAplication.model.Cliente;
import ZtechAplication.model.Marca;

public interface CategoriaRepository extends CrudRepository<Categoria, Integer>, JpaRepository<Categoria, Integer>{
	Optional<Categoria> findByNome(String cate);

}
