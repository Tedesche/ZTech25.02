package ZtechAplication.DTO;

public class ClienteDTO {
	@Override
	public String toString() {
		return "ClienteDTO [idCliente=" + idCliente + ", nomeCliente=" + nomeCliente + ", cpf=" + cpf + ", endEmail="
				+ endEmail + ", telefone=" + telefone + ", rua=" + rua + ", cep=" + cep + ", bairro=" + bairro
				+ ", cidade=" + cidade + ", numeroCasa=" + numeroCasa + "]";
	}
	private Integer idCliente;
	private String nomeCliente;
	private String cpf;
	
	private String endEmail;
	private String telefone;
	
	private String rua;
	private String cep;
	private String bairro;
	private String cidade;
	private int numeroCasa;
	
	
	
	public Integer getIdCliente() {
		return idCliente;
	}
	public void setIdCliente(Integer idCliente) {
		this.idCliente = idCliente;
	}
	public String getNomeCliente() {
		return nomeCliente;
	}
	public void setNomeCliente(String nome) {
		this.nomeCliente = nome;
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
