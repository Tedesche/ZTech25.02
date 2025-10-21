package ZtechAplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                .requestMatchers("/styles/**", "/img/**", "/js/**").permitAll() 
                // Permite acesso irrestrito ao H2 Console
                .requestMatchers(toH2Console()).permitAll()
                // Permite acesso à página de login e à raiz
                .requestMatchers("/", "/login").permitAll()
                // Qualquer outra requisição precisa de autenticação
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

    @Bean
    public UserDetailsService userDetailsService() {
        // Por enquanto, vou criar um usuário em memória para testar
        UserDetails user = User.builder()
            .username("user")
            // A senha é "1234", mas armazenada de forma criptografada
            .password(passwordEncoder().encode("1234")) 
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(user);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Define o encoder de senhas que será usado na aplicação
        return new BCryptPasswordEncoder();
    }
}