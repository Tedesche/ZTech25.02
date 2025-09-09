package ZtechAplication.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_Email")
public class Email {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idEmail")  // Mapeia para a coluna existente
	private int idEmail;

    @Column(nullable = false, length = 50)
	private String endEmail;
    
    @OneToOne
    @JoinColumn(name = "fk_Cliente", nullable = false)
    private Cliente cliente;
	
	public int getIdEmail() {
		return idEmail;
	}
	public void setIdEndEmail(int idEmail) {
		this.idEmail = idEmail;
	}
	public String getEndEmail() {
		return endEmail;
	}
	public void setEmail(String endEmail) {
		this.endEmail = endEmail;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	
}
