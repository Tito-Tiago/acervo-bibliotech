package com.bibliotech.bibliotech.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {
    @Autowired
    SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/alunos/mais-leitores/export/pdf").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/alunos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/alunos/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/alunos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PUT, "/alunos/{id}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PATCH, "/alunos/inativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/alunos/ativar/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.GET, "/autor/buscar").authenticated() //testar
                        .requestMatchers(HttpMethod.DELETE, "/autor/sem-associacao").permitAll()

                        .requestMatchers(HttpMethod.POST, "/cronograma").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/cronograma").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cronograma/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/cronograma/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.DELETE, "/cronograma/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.POST, "/emprestimos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/emprestimos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/emprestimos/aluno/{idAluno}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/emprestimos/livro/{idLivro}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PATCH, "/emprestimos/renovar/{id}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PATCH, "/emprestimos/cancelar/{id}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PATCH, "/emprestimos/concluir/{id}").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.POST, "/emprestimos/enviar-email").permitAll()

                        .requestMatchers(HttpMethod.POST, "/estanteprateleira").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/estanteprateleira").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PUT, "/estanteprateleira/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.DELETE, "/estanteprateleira/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/estanteprateleira/{id}").hasAnyRole("bibliotecario", "aluno_monitor")

                        .requestMatchers(HttpMethod.POST, "/estantesecao").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.DELETE, "/estantesecao").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/estantesecao/{idSecao}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.POST, "/secoes").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.DELETE, "/secoes/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/secoes").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/secoes/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/secoes/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.POST, "/frequencia-alunos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/frequencia-alunos/export/pdf").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/frequencia-alunos").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.DELETE, "/frequencia-alunos/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.GET, "/generos/buscar").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.DELETE, "/generos/sem-associacao").permitAll()

                        .requestMatchers(HttpMethod.POST, "/ocorrencias").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.POST, "/ocorrencias/export/pdf").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/ocorrencias").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.DELETE, "/ocorrencias/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.POST, "/turmas").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/turmas/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/turmas/mais-leitoras/export/pdf").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/turmas/filtrar").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.PUT, "/turmas/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/turmas/inativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/turmas/ativar/{id}").hasRole("bibliotecario")

                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()  //MUDAR ISSO, permitir apenas bilbiotecario
                        .requestMatchers(HttpMethod.GET, "/usuarios/filtrar").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/usuarios/{id}").authenticated() //a permissao mais abranjente de um mesmo http method e mesmo root de endpoint tem que estar em baixo das menos abranjentes
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/inativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/ativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.POST, "/usuarios/login").permitAll()

                        .requestMatchers(HttpMethod.POST, "/livros").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.POST, "/livros/exemplares").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/livros/filtrar").hasAnyRole("bibliotecario", "aluno_monitor")
                        .requestMatchers(HttpMethod.GET, "/livros/exemplares/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/livros/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/livros/inativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/livros/ativar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/livros/exemplares/extraviar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.PATCH, "/livros/exemplares/atualizar/{id}").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/livros/relatorio/export/pdf").hasRole("bibliotecario")
                        .requestMatchers(HttpMethod.GET, "/livros/relatorio/acervo/export/pdf").hasRole("bibliotecario")


                        .requestMatchers(HttpMethod.GET, "/root").permitAll()

                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/readiness").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();

    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
