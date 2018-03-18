package com.example.demo;

import java.net.URI;
import java.time.LocalDateTime;

import com.example.demo.error.ErrorHandler;
import com.example.demo.exception.EntityNotFound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@EnableMongoAuditing
@EnableConfigurationProperties(AppProperties.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}


/*
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    UserDetailsRepository userDetailsRepository() {
        return new MapReactiveUserDetailsService(user("rob").build(), user("josh").roles("USER","ADMIN").build());
    }

    private User.UserBuilder user(String username) {
        return User.withDefaultPasswordEncoder().username(username).password("password").roles("USER");
    }

    @Bean
    SecurityWebFilterChain springSecurity(ServerHttpSecurity http) {
        return http
                .authorizeExchange()
                    .pathMatchers("/users/me").authenticated()
                    .pathMatchers("/users/{username}").access((auth,context) ->
                        auth
                                .map( a-> a.getName().equals(context.getVariables().get("username")))
                                .map(AuthorizationDecision::new)
                    )
                    .anyExchange().hasRole("ADMIN")
                    .and()
                .build();
    }
}*/
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
        return http
                .authorizeExchange()
                .pathMatchers(HttpMethod.GET, "/posts/**").permitAll()
                //.pathMatchers(HttpMethod.DELETE, "/posts/**").hasRole("ADMIN")
               // .pathMatchers("/posts/**")
              //  .authenticated()

                //.pathMatchers("/users/{user}/**").access(this::currentUserMatchesPath)
                .anyExchange().permitAll()
                .and().csrf().disable()
                .build();
    }

    private Mono<AuthorizationDecision> currentUserMatchesPath(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .map(a -> context.getVariables().get("user").equals(a.getName()))
                .map(granted -> new AuthorizationDecision(granted));
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsRepository() {
        UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
        UserDetails admin = User.withDefaultPasswordEncoder().username("admin").password("password").roles("USER", "ADMIN").build();
        return new MapReactiveUserDetailsService(user, admin);
    }

}

@Component
@Slf4j
class DataInitializer implements CommandLineRunner {

    private final PostRepository posts;

    public DataInitializer(PostRepository posts) {
        this.posts = posts;
    }

    @Override
    public void run(String[] args) {
       /* log.info("start data initialization  ...");
        this.posts
                .deleteAll()
                .thenMany(
                        Flux
                                .just("Postone", "Posttwo")
                                .flatMap(
                                        title -> this.posts.save(Post.builder().title(title).content("content of " + title).build())
                                )
                )
                .log()
                .subscribe(
                        null,
                        null,
                        () -> log.info("done initialization...")
                );*/

    }

}

@Component
class PostRouterFunction {

    @Bean
    public RouterFunction<ServerResponse> routes(PostHandler postHandler) {
        return route(GET("/posts/{id}").and(accept(MediaType.APPLICATION_JSON)), postHandler::findById)
                .andRoute(GET("/posts/title/{title}"), postHandler::findByTitle)
                .andRoute(POST("/posts"),postHandler::create);
    }

}

@Component
class PostHandler {

    PostRepository postRepository;

    ErrorHandler errorHandler;

    public PostHandler(final PostRepository postRepository, final ErrorHandler errorHandler) {
        this.postRepository = postRepository;
        this.errorHandler = errorHandler;
    }

    public Mono<ServerResponse> findById(ServerRequest serverRequest){
        return getServerResponseMono(serverRequest)
                .onErrorResume(
                errorHandler::throwableErrorOccurred
                );
    }

    private Mono<ServerResponse> getServerResponseMono(final ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        return postRepository.findById(id)
            .flatMap(post -> ServerResponse.ok().body(Mono.just(post),Post.class))
                .switchIfEmpty(
                        Mono.error(new EntityNotFound("Required Object Not Found"))
                );
    }

    public Mono<ServerResponse> findByTitle(ServerRequest serverRequest){
        String title = serverRequest.pathVariable("title");
        Flux<Post> response = postRepository.findByTitle(title);
        return ServerResponse.ok().body(response,Post.class);
    }

    public Mono<ServerResponse> create(final ServerRequest serverRequest) {
        Mono<Post> postMono = serverRequest.bodyToMono(Post.class);
        return postMono
                .flatMap(post -> postRepository.save(post))
                .flatMap(post -> ServerResponse.created(URI.create("/posts/" + post.getId())).build());

    }
}

interface PostRepository extends ReactiveMongoRepository<Post, String> {
    Flux<Post> findByTitle(String title);
}

@Document
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Post {

    @Id
    private String id;
    private String title;
    private String content;

    @CreatedDate
    private LocalDateTime createdDate;
}
