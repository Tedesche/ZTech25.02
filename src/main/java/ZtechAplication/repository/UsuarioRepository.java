package ZtechAplication.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ZtechAplication.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Método que o Spring Security usará para buscar o usuário pelo username
    Optional<Usuario> findByUsername(String username);
}