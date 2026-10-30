package ZtechAplication.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_Produto")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProduto;
    
    @Column(length = 50, nullable = false)
    private String nome;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal custo;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    
    @Column(nullable = false)
    private Integer quantidade;

    // NOVO: Atributo para controlar a margem de segurança do estoque
    @Column(nullable = false)
    private Integer quantidadeMinima;
    
    @Column(columnDefinition = "TEXT")
    private String descricao;
    
    @ManyToOne
    @JoinColumn(name = "fk_Categoria", nullable = false)
    private Categoria categoria;
    
    @ManyToOne
    @JoinColumn(name = "fk_Marca", nullable = false)
    private Marca marca;
    
    @ManyToOne
    private Venda vendas;

    // NOVO: Método de validação exigido pelo teste
    public boolean verificaEstoque() {
        if (this.quantidade == null || this.quantidadeMinima == null) {
            return false; // Prevenção contra NullPointerException caso os valores não sejam setados
        }
        return this.quantidade <= this.quantidadeMinima;
    }

    // Métodos auxiliares de controle
    public void adicionarQuantidade(int quantidade) {
        this.quantidade += quantidade;
    }

    public void removerQuantidade(int quantidade) {
        if (this.quantidade >= quantidade) {
            this.quantidade -= quantidade;
        } else {
            throw new IllegalStateException("Quantidade em estoque insuficiente");
        }
    }
    
    // Getters e Setters
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

    // NOVO: Getter e Setter para a quantidade mínima
    public Integer getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(Integer quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public Venda getVendas() {
        return vendas;
    }

    public void setVendas(Venda vendas) {
        this.vendas = vendas;
    }
}