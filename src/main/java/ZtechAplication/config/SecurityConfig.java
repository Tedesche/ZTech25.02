package ZtechAplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder; // 1. Importe o NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// import referente ao acesso ao banco de dados h2
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Permite acesso a recursos estáticos (CSS, JS, imagens, etc.)
                .requestMatchers("/styles/**", "/img/**", "/script/**").permitAll()
                // Permite acesso irrestrito ao H2 Console
                .requestMatchers(toH2Console()).permitAll()
                // Permite acesso à página de login e à raiz
                .requestMatchers("/", "/login", "/inicio").permitAll()
             // 2. Permite que usuários logados vejam a página /index
                .requestMatchers("/index").permitAll()
                // Qualquer outra requisição precisa de autenticação

                .requestMatchers("/api/marcas", "/api/categorias").permitAll()

                //teste do metodo para cadastrar marca
                .requestMatchers("/api/marcas/salvar" ).permitAll()
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Define a URL da página de login personalizada
                .loginPage("/login")
                // Define a URL para onde o usuário é redirecionado após o login bem-sucedido
                .defaultSuccessUrl("/inicio", true)
                .permitAll()
            ).csrf(csrf -> csrf
                    // Desabilita o CSRF APENAS para o H2 Console
                    .ignoringRequestMatchers(toH2Console())
                )
                .headers(headers -> headers
                    // Permite que o H2 Console seja exibido em frames (necessário)
                    .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
            .logout(logout -> logout
                // Habilita a funcionalidade de logout
                .logoutUrl("/logout")
                // URL para redirecionar após o logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    // 2. REMOVA o método userDetailsService() que criava o usuário em memória.
    //    O Spring irá automaticamente usar a sua implementação DetalhesUsuarioServiceImpl.

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 3. Altere o encoder para não fazer criptografia.
        //    Isso é inseguro para produção, mas atende ao seu requisito.
        return NoOpPasswordEncoder.getInstance();
    }
}