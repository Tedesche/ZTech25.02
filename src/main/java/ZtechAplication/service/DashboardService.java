package ZtechAplication.service;

import ZtechAplication.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private VendaRepository vendaRepository;

    // Método que o Dashboard chama para saber o total de vendas registradas
    public long contarTotalDeVendas() {
        return vendaRepository.count();
    }
}