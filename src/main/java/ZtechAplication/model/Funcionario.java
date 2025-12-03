package ZtechAplication.model;

import java.time.LocalDate;
// Removidos imports não utilizados (LocalTime, ArrayList, List, ManyToOne)
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
// Removido @JoinColumn (não é usado diretamente aqui)
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_FUNCIONARIO") // Mapeia para a tabela TB_FUNCIONARIO do schema.sql
public class Funcionario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_FUN") // Mapeia para a coluna ID_FUN
	private Integer idFun;
	
	@Column(name = "NOME_FUN" , length = 50) // Mapeia para a coluna NOME_FUN
	private String nomeFuncionario;
	
	@Column(name = "CPF_FUN" ,length = 20) // Mapeia para a coluna CPF_FUN
    private String cpf;
	
    @Column(name = "DATA_ADM") // Mapeia para a coluna DATA_ADM
    private LocalDate dataAdm;
	
	@Column(name = "STATUS_FUN", length = 15) // Mapeia para a coluna STATUS_FUN
    private String status_Fun;

	@Column(name = "NIVEL_ACESS" , length = 15) // Mapeia para a coluna NIVEL_ACESS
    private String nivelAces;

	@OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Email email;
    
    @OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Endereco endereco;
    
    @OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Telefone telefone;

	public Integer getIdFun() {
		return idFun;
	}

	public void setIdFun(Integer idFun) {
		this.idFun = idFun;
	}

	public String getNomeFuncionario() {
		return nomeFuncionario;
	}

	public void setNomeFuncionario(String nomeFuncionario) {
		this.nomeFuncionario = nomeFuncionario;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getStatus_Fun() {
		return status_Fun;
	}

	public void setStatus_Fun(String status_Fun) {
		this.status_Fun = status_Fun;
	}

	public String getNivelAces() {
		return nivelAces;
	}

	public void setNivelAces(String nivelAces) {
		this.nivelAces = nivelAces;
	}

	public Email getEmail() {
		return email;
	}

	public void setEmail(Email email) {
		this.email = email;
	}

	public Endereco getEndereco() {
		return endereco;
	}

	public void setEndereco(Endereco endereco) {
		this.endereco = endereco;
	}

	public Telefone getTelefone() {
		return telefone;
	}

	public void setTelefone(Telefone telefone) {
		this.telefone = telefone;
	}

	public LocalDate getDataAdm() {
		return dataAdm;
	}

	public void setDataAdm(LocalDate dataAdm) {
		this.dataAdm = dataAdm;
	}
	
	
}