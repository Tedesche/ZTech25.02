package ZtechAplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

import ZtechAplication.service.DetalhesUsuarioServiceImpl; 
import ZtechAplication.config.CustomAuthenticationSuccessHandler; 

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DetalhesUsuarioServiceImpl detalheUsuarioService;
    private final CustomAuthenticationSuccessHandler successHandler; 

    public SecurityConfig(DetalhesUsuarioServiceImpl detalheUsuarioService, 
                          CustomAuthenticationSuccessHandler successHandler) {
        this.detalheUsuarioService = detalheUsuarioService;
        this.successHandler = successHandler; 
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(detalheUsuarioService) 
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/styles/**", "/img/**", "/script/**", "/js/**").permitAll() 
                .requestMatchers(toH2Console()).permitAll()
                .requestMatchers("/", "/login", "/inicio", "/login-verificacao", "/verificar-otp").permitAll() 
                .requestMatchers("/index").permitAll()
                .requestMatchers("/api/marcas", "/api/categorias").permitAll() //url para abastecer os textlist em produto
                .requestMatchers("/api/marcas/salvar" ).permitAll()
                .requestMatchers("/api/marcas/salvar" ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(toH2Console())
                .ignoringRequestMatchers("/verificar-otp") 
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .logout(logout -> logout
                // --- CORREÇÃO DEFINITIVA ---
                // Força o logout a aceitar requisições do tipo GET (clique no link)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}