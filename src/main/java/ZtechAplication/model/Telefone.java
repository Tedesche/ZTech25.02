package ZtechAplication.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_Telefone")
public class Telefone {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idTelefone")
	private int idTelefone;
	private String telefone;
    
	@OneToOne
    // CORREÇÃO: nullable = true para permitir salvar sem Cliente (caso seja de funcionário)
    @JoinColumn(name = "fk_Cliente", nullable = true)
    private Cliente cliente;
	
    @OneToOne
    // CORREÇÃO: nullable = true para permitir salvar sem Funcionário (caso seja de cliente)
    @JoinColumn(name = "fk_Fun", nullable = true)
    private Funcionario funcionario;

    // Getters e Setters
    public Integer getIdTelefone() {
        return idTelefone;
    }

    public void setIdTelefone(Integer idTelefone) {
        this.idTelefone = idTelefone;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
    
	public Funcionario getFuncionario() {
		return funcionario;
	}
    
	public void setFuncionario(Funcionario funcionario) {
		this.funcionario = funcionario;
	}
}