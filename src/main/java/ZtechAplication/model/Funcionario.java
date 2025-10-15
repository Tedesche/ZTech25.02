package ZtechAplication.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
@Table(name = "tb_Funcionario")
public class Funcionario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idFun;
	
	@Column(name = "nome_Fun" , length = 50)
	private String nomeFuncionario;
	
	@Column(name = "CPF_Fun" ,length = 20)
    private String cpf;
	
    @Column(name = "data_adm")
    private LocalDate dataAdm;
	
	@Column(length = 15)
    private String status_Fun;

	@Column(name = "nivel_acess" , length = 15)
    private String nivelAces;

    @OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Email email;
    
    @OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Endereco endereco;
    
    @OneToOne(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
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
	
}
