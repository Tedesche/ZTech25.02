package ZtechAplication.model;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_USUARIO") // Mapeia para a tabela TB_USUARIO do schema.sql
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO") // Mapeia para a coluna ID_USUARIO
    private Long id;

    @Column(name = "USERNAME") // Mapeia para a coluna USERNAME
    private String username; 
    
    @Column(name = "PASSWORD") // Mapeia para a coluna PASSWORD
    private String password; 

    // Relacionamento One-to-One: Um usuário está ligado a um funcionário
    @OneToOne(fetch = FetchType.EAGER)
    // Liga a coluna FK_FUNCIONARIO (desta tabela) com a coluna ID_FUN (da tabela TB_FUNCIONARIO)
    @JoinColumn(name = "FK_FUNCIONARIO", referencedColumnName = "ID_FUN")
    private Funcionario funcionario;
    
    public Usuario() {}

    public Usuario(String username, String password, Funcionario funcionario) {
        this.username = username;
        this.password = password;
        this.funcionario = funcionario;
    }

    // --- Getters e Setters Padrão ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    
    // --- Métodos da Interface UserDetails ---

    @Override
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Pega o "nivelAces" do Funcionário e o transforma em uma
     * "Role" (papel) que o Spring Security entende.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // CORREÇÃO: Usa o getter do seu Funcionario.java -> getNivelAces()
        if (this.funcionario == null || this.funcionario.getNivelAces() == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_DEFAULT")); // Papel padrão
        }
        
        // Ex: Se nivelAces="administrador", a Role será "ROLE_ADMINISTRADOR"
        String role = "ROLE_" + this.funcionario.getNivelAces().toUpperCase();
        return List.of(new SimpleGrantedAuthority(role));
    }

    // Métodos de status da conta (todos ativos por padrão)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    
}