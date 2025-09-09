package ZtechAplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import ZtechAplication.model.Categoria;
import ZtechAplication.model.Marca;

public interface MarcaRepository extends CrudRepository<Marca, Integer>, JpaRepository<Marca, Integer> {
	Optional<Marca> findByNome(String cate);
//	List<Marca> findByAll();

}
