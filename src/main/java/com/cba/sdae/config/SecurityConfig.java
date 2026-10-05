package com.cba.sdae.config;

import com.cba.sdae.filter.BruteForceProtectionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SdaeUserDetailsService userDetailsService;
    private final BruteForceProtectionFilter bruteForceProtectionFilter;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;

    public SecurityConfig(SdaeUserDetailsService userDetailsService,
                          BruteForceProtectionFilter bruteForceProtectionFilter,
                          CustomAuthenticationFailureHandler authenticationFailureHandler) {
        this.userDetailsService = userDetailsService;
        this.bruteForceProtectionFilter = bruteForceProtectionFilter;
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    /**
     * CORRIGIDO (S2): NoOpPasswordEncoder foi substituído por BCrypt.
     * Toda senha nova deve ser gravada já com encoder.encode(senhaPura).
     * Senhas legadas em texto plano no banco deixam de autenticar — rode uma
     * migração (ver DataSeeder / script de migração) antes de subir esta mudança.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // ── Público ────────────────────────────────────────────
                        .requestMatchers("/", "/welcome", "/login", "/cadastro", "/cadastro/salvar").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico", "/webjars/**").permitAll()

                        // ── API de autenticação ────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()

                        // ── Telas MVC de administração ─────────────────────────
                        // ADENDO: faltava esta regra na primeira versão do arquivo —
                        // sem ela, /admin/** só exigia "estar logado" (qualquer
                        // papel), não ser ADMINISTRADOR de fato.
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMINISTRADOR")

                        // Telas MVC de escrita: mesmo critério da API REST.
                        // Sem isto, um OPERADOR autenticado conseguia POST em
                        // /sensores/salvar, /esteiras/deletar etc.
                        .requestMatchers("/sensores/novo", "/sensores/editar/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/sensores/salvar", "/sensores/deletar/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers("/esteiras/nova", "/esteiras/editar/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/esteiras/salvar", "/esteiras/deletar/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/alertas/deletar/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")

                        // ── CORRIGIDO (S1): API deixa de ser permitAll ─────────
                        // Leitura (GET) liberada para qualquer usuário autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/esteiras/**", "/api/sensores/**", "/api/alertas/**", "/api/dashboard", "/api/dashboard/**")
                        .authenticated()

                        // Endpoint usado por dispositivos (ESP32) para enviar leitura.
                        // Não depende de sessão/login — o ESP32 não consegue fazer
                        // formLogin. A autenticação é por API key, validada dentro
                        // de SensorRestController#registrarLeitura (cabeçalho
                        // X-API-Key comparado com a chave gerada para o sensor).
                        .requestMatchers(HttpMethod.POST, "/api/sensores/*/leitura").permitAll()
                        .requestMatchers("/api/test/**").permitAll()

                        // Escrita em esteiras/sensores: só ENGENHEIRO ou ADMINISTRADOR.
                        .requestMatchers(HttpMethod.POST, "/api/esteiras/**", "/api/sensores/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/esteiras/**", "/api/sensores/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/esteiras/**", "/api/sensores/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/esteiras/**", "/api/sensores/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")

                        // Alertas: exclusão restrita a ENGENHEIRO/ADMINISTRADOR.
                        .requestMatchers(HttpMethod.DELETE, "/api/alertas/**")
                        .hasAnyRole("ENGENHEIRO", "ADMINISTRADOR")

                        // Funcionários: gestão de pessoas é só ADMINISTRADOR
                        // (CORRIGIDO (S7) em conjunto com CadastroController —
                        // o cadastro público nunca deve resultar em ADMINISTRADOR).
                        .requestMatchers("/api/funcionarios/**").hasRole("ADMINISTRADOR")

                        // Qualquer outra rota exige login.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/welcome")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                )
                .csrf(csrf -> {
                    CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    csrfRepo.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
                    csrf.csrfTokenRepository(csrfRepo)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .ignoringRequestMatchers("/api/auth/login", "/api/sensores/*/leitura", "/api/test/**");
                })
                .addFilterBefore(bruteForceProtectionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    FilterChain filterChain)
                            throws ServletException, IOException {
                        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                        if (token != null) {
                            token.getToken();
                        }
                        filterChain.doFilter(request, response);
                    }
                }, CsrfFilter.class)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'self';")
                        )
                        // CORRIGIDO: no Spring Security 6 o valor é um enum, não uma String.
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // CORRIGIDO: ContentTypeOptions não tem enable(); já vem ligado por padrão.
                        .contentTypeOptions(Customizer.withDefaults())
                        // CORRIGIDO: o enum ReferrerPolicy fica em ReferrerPolicyHeaderWriter.
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)
                        )
                        .permissionsPolicy(permissions -> permissions
                                .policy("geolocation=(), microphone=(), camera=()")
                        )
                );

        return http.build();
    }
}