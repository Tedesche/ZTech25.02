package ZtechAplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "ZtechAplication.model")  // ajuste conforme seu pacote
@EnableJpaRepositories("ZtechAplication.repository") // ajuste conforme seu pacote
public class ZtechAplicationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZtechAplicationApplication.class, args);
	}

}
