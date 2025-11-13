package ZtechAplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// import referente ao acesso ao banco de dados h2
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

// --- IMPORTES ADICIONADOS PARA O 2FA ---
import ZtechAplication.service.DetalhesUsuarioServiceImpl; 
import ZtechAplication.config.CustomAuthenticationSuccessHandler; 
// --- FIM DOS IMPORTES ---

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // --- INJEÇÃO DE DEPENDÊNCIA ADICIONADA ---
    // Agora a configuração de segurança conhece os seus serviços
    private final DetalhesUsuarioServiceImpl detalheUsuarioService;
    private final CustomAuthenticationSuccessHandler successHandler; 

    public SecurityConfig(DetalhesUsuarioServiceImpl detalheUsuarioService, 
                          CustomAuthenticationSuccessHandler successHandler) {
        this.detalheUsuarioService = detalheUsuarioService;
        this.successHandler = successHandler; 
    }
    // --- FIM DA INJEÇÃO ---

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- ADICIONADO ---
            // Diz ao Spring para usar seu serviço de detalhes do usuário
            .userDetailsService(detalheUsuarioService) 
            
            .authorizeHttpRequests(authorize -> authorize
                // Permite acesso a recursos estáticos (CSS, JS, imagens, etc.)
                // Adicionamos /js/** para garantir
                .requestMatchers("/styles/**", "/img/**", "/script/**", "/js/**").permitAll() 
                
                // Permite acesso irrestrito ao H2 Console
                .requestMatchers(toH2Console()).permitAll()
                
                // --- ALTERADO ---
                // Adicionamos as novas páginas de verificação do 2FA
                .requestMatchers("/", "/login", "/inicio", "/login-verificacao", "/verificar-otp").permitAll() 
                
                // Permissões que você já tinha (mantidas)
                .requestMatchers("/index").permitAll()
                .requestMatchers("/api/marcas", "/api/categorias").permitAll()
                .requestMatchers("/api/marcas/salvar" ).permitAll()
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Define a URL da página de login personalizada
                .loginPage("/login")
                
                // --- ESTA É A CORREÇÃO PRINCIPAL ---
                // Trocamos o redirecionamento direto pelo seu Handler de 2FA
                // .defaultSuccessUrl("/inicio", true) // <-- LINHA ANTIGA REMOVIDA
                .successHandler(successHandler) // <-- LINHA NOVA ADICIONADA
                .permitAll()
            )
            .csrf(csrf -> csrf
                // Desabilita o CSRF APENAS para o H2 Console
                .ignoringRequestMatchers(toH2Console())
                
                // --- ADICIONADO ---
                // Permite que o formulário da página de 2FA (/verificar-otp) funcione
                .ignoringRequestMatchers("/verificar-otp") 
            )
            .headers(headers -> headers
                // Permite que o H2 Console seja exibido em frames
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .logout(logout -> logout
                // Configuração de logout (já existia)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}