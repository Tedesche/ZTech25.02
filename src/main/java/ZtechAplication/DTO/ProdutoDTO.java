package ZtechAplication.DTO;

import java.math.BigDecimal;

public class ProdutoDTO {
	private Integer idProduto;
	private Integer IdMarca;
	private Integer IdCategoria;
	private String nome;
	private BigDecimal custo;
	private BigDecimal valor;
	private Integer quantidade;
	private String descricao;
	private String categoria;
	private String marca;
	
	
	
	//getters and setters
	public Integer getIdProduto() {
		return idProduto;
	}
	public void setIdProduto(Integer idProduto) {
		this.idProduto = idProduto;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public BigDecimal getCusto() {
		return custo;
	}
	public void setCusto(BigDecimal custo) {
		this.custo = custo;
	}
	public BigDecimal getValor() {
		return valor;
	}
	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}
	
	public Integer getQuantidade() {
		return quantidade;
	}
	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}
	public String getDescricao() {
		return descricao;
	}
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	public String getCategoria() {
		return categoria;
	}
	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
	public String getMarca() {
		return marca;
	}
	public void setMarca(String marca) {
		this.marca = marca;
	}
	public Integer getIdMarca() {
		return IdMarca;
	}
	public void setIdMarca(Integer idMarca) {
		IdMarca = idMarca;
	}
	public Integer getIdCategoria() {
		return IdCategoria;
	}
	public void setIdCategoria(Integer idCategoria) {
		IdCategoria = idCategoria;
	}
	
	
	

}
