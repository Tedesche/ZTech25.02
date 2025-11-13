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
@Table(name = "TB_EMAIL") // Nome exato da tabela
public class Email {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_EMAIL")
	private int idEmail;

    @Column(name = "END_EMAIL", nullable = false, length = 50)
	private String endEmail;
    
    @OneToOne
    // --- CORREÇÃO AQUI ---
    // Permite que a FK_CLIENTE seja nula (quando for e-mail de funcionário)
    @JoinColumn(name = "FK_CLIENTE", nullable = true) 
    private Cliente cliente;
    
    @OneToOne
    // --- CORREÇÃO AQUI ---
    // Permite que a FK_FUN seja nula (quando for e-mail de cliente)
    @JoinColumn(name = "FK_FUN", nullable = true)
    private Funcionario funcionario;
    
	
    // Getters e Setters (Corrigidos)
	public int getIdEmail() {
		return idEmail;
	}
	public void setIdEmail(int idEmail) {
		this.idEmail = idEmail;
	}
	public String getEndEmail() {
		return endEmail;
	}
	public void setEndEmail(String endEmail) {
		this.endEmail = endEmail;
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