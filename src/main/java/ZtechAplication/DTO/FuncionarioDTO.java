package ZtechAplication.DTO;

public class FuncionarioDTO {
	
	private Integer idFun;
	private String nomeFuncionario;
	private String cpf;
	
	private String endEmail;
	private String telefone;
	private String status_Fun;
	private String nivelAces;
	
	private String rua;
	private String cep;
	private String bairro;
	private String cidade;
	private int numeroCasa;
	public Integer getIdFuncionario() {
		return idFun;
	}
	public void setIdFuncionario(Integer idFuncionario) {
		this.idFun = idFuncionario;
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
	public String getStatus() {
		return status_Fun;
	}
	public void setStatus(String status) {
		this.status_Fun = status;
	}
	public String getNivelAces() {
		return nivelAces;
	}
	public void setNivelAces(String nivelAces) {
		this.nivelAces = nivelAces;
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

	
}
