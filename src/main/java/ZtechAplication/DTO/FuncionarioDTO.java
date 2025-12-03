package ZtechAplication.DTO;

import java.time.LocalTime;

import jakarta.persistence.Column;

public class FuncionarioDTO {
	
	private Integer idFun;
	private String nomeFuncionario;
	private String cpf;
	
	private String endEmail;
	private String telefone;
	private String status_Fun;
	private String nivelAces;
    private String dataAdm;
	
	private String rua;
	private String cep;
	private String bairro;
	private String cidade;
	private int numeroCasa;
	
	public Integer getIdFuncionario() {
		return idFun;
	}
	public void setIdFuncionario(Integer idFun) {
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
	public String getEndEmail() {
		return endEmail;
	}
	public void setEndEmail(String endEmail) {
		this.endEmail = endEmail;
	}
	public String getTelefone() {
		return telefone;
	}
	public void setTelefone(String telefone) {
		this.telefone = telefone;
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
	public String getDataAdm() {
		return dataAdm;
	}
	public void setDataAdm(String dataAdm) {
		this.dataAdm = dataAdm;
	}
	public String getRua() {
		return rua;
	}
	public void setRua(String rua) {
		this.rua = rua;
	}
	public String getCep() {
		return cep;
	}
	public void setCep(String cep) {
		this.cep = cep;
	}
	public String getBairro() {
		return bairro;
	}
	public void setBairro(String bairro) {
		this.bairro = bairro;
	}
	public String getCidade() {
		return cidade;
	}
	public void setCidade(String cidade) {
		this.cidade = cidade;
	}
	public int getNumeroCasa() {
		return numeroCasa;
	}
	public void setNumeroCasa(int numeroCasa) {
		this.numeroCasa = numeroCasa;
	}
	public void setIdFun(Integer idFun) {
		this.idFun = idFun;
	}
	
	
}
