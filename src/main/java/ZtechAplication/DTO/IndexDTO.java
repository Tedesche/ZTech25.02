package ZtechAplication.DTO;

import java.util.List;

public class IndexDTO {
	private List<ClienteDTO> cliDTO;
	private List<FuncionarioDTO> funDTO;
	private List<OrdemServicoDTO> ordDTO;
	private List<ProdutoDTO> proDTO;
	private List<VendaDTO> venDTO;
	
	//GETERS AND SETERS DAS CLASSES
	public List<ClienteDTO> getCliDTO() {
		return cliDTO;
	}
	public void setCliDTO(List<ClienteDTO> cliDTO) {
		this.cliDTO = cliDTO;
	}
	public List<FuncionarioDTO> getFunDTO() {
		return funDTO;
	}
	public void setFunDTO(List<FuncionarioDTO> funDTO) {
		this.funDTO = funDTO;
	}
	public List<OrdemServicoDTO> getOrdDTO() {
		return ordDTO;
	}
	public void setOrdDTO(List<OrdemServicoDTO> ordDTO) {
		this.ordDTO = ordDTO;
	}
	public List<ProdutoDTO> getProDTO() {
		return proDTO;
	}
	public void setProDTO(List<ProdutoDTO> proDTO) {
		this.proDTO = proDTO;
	}
	public List<VendaDTO> getVenDTO() {
		return venDTO;
	}
	public void setVenDTO(List<VendaDTO> venDTO) {
		this.venDTO = venDTO;
	}
	
	
}
