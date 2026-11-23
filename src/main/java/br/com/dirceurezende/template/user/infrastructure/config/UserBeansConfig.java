package br.com.dirceurezende.template.user.infrastructure.config;

import br.com.dirceurezende.template.user.application.port.in.DeleteUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.GetUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.ListUsersUseCase;
import br.com.dirceurezende.template.user.application.port.in.LoginUseCase;
import br.com.dirceurezende.template.user.application.port.in.LogoutUseCase;
import br.com.dirceurezende.template.user.application.port.in.RefreshTokenUseCase;
import br.com.dirceurezende.template.user.application.port.in.LoadUserCredentialsUseCase;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.RequestWelcomeUseCase;
import br.com.dirceurezende.template.user.application.port.in.SendWelcomeUseCase;
import br.com.dirceurezende.template.user.application.port.out.UserEventPublisher;
import br.com.dirceurezende.template.user.application.port.out.WelcomeNotifier;
import br.com.dirceurezende.template.user.application.usecase.RequestWelcomeUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.SendWelcomeUseCaseImpl;
import br.com.dirceurezende.template.user.application.port.in.UpdateUserUseCase;
import br.com.dirceurezende.template.user.application.port.out.AccessTokenIssuer;
import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.application.usecase.DeleteUserUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.GetUserUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.ListUsersUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.LoadUserCredentialsUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.LoginUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.LogoutUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.RefreshTokenUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.RegisterUserUseCaseImpl;
import br.com.dirceurezende.template.user.application.usecase.UpdateUserUseCaseImpl;
import br.com.dirceurezende.template.user.infrastructure.adapter.out.security.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Objects;

/**
 * Único lugar onde a application encontra o Spring.
 * Os casos de uso são POJOs; aqui eles viram beans e ganham transação.
 */
@Configuration(proxyBeanMethods = false)
class UserBeansConfig {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository users, PasswordHasher hasher,
                                            Clock clock, PlatformTransactionManager txManager) {
        var useCase = new RegisterUserUseCaseImpl(users, hasher, clock);
        var tx = new TransactionTemplate(txManager);
        return command -> Objects.requireNonNull(tx.execute(_ -> useCase.register(command)));
    }

    @Bean
    GetUserUseCase getUserUseCase(UserRepository users, PlatformTransactionManager txManager) {
        var useCase = new GetUserUseCaseImpl(users);
        var tx = readOnly(txManager);
        return id -> Objects.requireNonNull(tx.execute(_ -> useCase.byId(id)));
    }

    @Bean
    LoadUserCredentialsUseCase loadUserCredentialsUseCase(UserRepository users, PlatformTransactionManager txManager) {
        var useCase = new LoadUserCredentialsUseCaseImpl(users);
        var tx = readOnly(txManager);
        return email -> Objects.requireNonNull(tx.execute(_ -> useCase.byEmail(email)));
    }

    @Bean
    ListUsersUseCase listUsersUseCase(UserRepository users, PlatformTransactionManager txManager) {
        var useCase = new ListUsersUseCaseImpl(users);
        var tx = readOnly(txManager);
        return (page, size) -> Objects.requireNonNull(tx.execute(_ -> useCase.list(page, size)));
    }

    @Bean
    UpdateUserUseCase updateUserUseCase(UserRepository users, PlatformTransactionManager txManager) {
        var useCase = new UpdateUserUseCaseImpl(users);
        var tx = new TransactionTemplate(txManager);
        return command -> Objects.requireNonNull(tx.execute(_ -> useCase.update(command)));
    }

    @Bean
    DeleteUserUseCase deleteUserUseCase(UserRepository users, RefreshTokenStore refreshTokens,
                                        PlatformTransactionManager txManager) {
        var useCase = new DeleteUserUseCaseImpl(users, refreshTokens);
        var tx = new TransactionTemplate(txManager);
        return id -> Objects.requireNonNull(tx.execute(_ -> useCase.delete(id)));
    }

    @Bean
    LoginUseCase loginUseCase(UserRepository users, PasswordHasher hasher, AccessTokenIssuer accessTokens,
                              RefreshTokenStore refreshTokens, Clock clock, JwtProperties props,
                              PlatformTransactionManager txManager) {
        var useCase = new LoginUseCaseImpl(users, hasher, accessTokens, refreshTokens, clock, props.refreshTtl());
        var tx = new TransactionTemplate(txManager);
        return command -> Objects.requireNonNull(tx.execute(_ -> useCase.login(command)));
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(UserRepository users, AccessTokenIssuer accessTokens,
                                            RefreshTokenStore refreshTokens, Clock clock, JwtProperties props,
                                            PlatformTransactionManager txManager) {
        var useCase = new RefreshTokenUseCaseImpl(users, accessTokens, refreshTokens, clock, props.refreshTtl());
        var tx = new TransactionTemplate(txManager);
        return token -> Objects.requireNonNull(tx.execute(_ -> useCase.refresh(token)));
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokenStore refreshTokens, PlatformTransactionManager txManager) {
        var useCase = new LogoutUseCaseImpl(refreshTokens);
        var tx = new TransactionTemplate(txManager);
        return token -> tx.executeWithoutResult(_ -> useCase.logout(token));
    }

    @Bean
    RequestWelcomeUseCase requestWelcomeUseCase(UserRepository users, UserEventPublisher events, Clock clock,
                                                PlatformTransactionManager txManager) {
        var useCase = new RequestWelcomeUseCaseImpl(users, events, clock);
        var tx = new TransactionTemplate(txManager); // grava no outbox na mesma transação da leitura
        return id -> Objects.requireNonNull(tx.execute(_ -> useCase.request(id)));
    }

    @Bean
    SendWelcomeUseCase sendWelcomeUseCase(WelcomeNotifier notifier) {
        return new SendWelcomeUseCaseImpl(notifier);
    }

    private static TransactionTemplate readOnly(PlatformTransactionManager txManager) {
        var tx = new TransactionTemplate(txManager);
        tx.setReadOnly(true);
        return tx;
    }
}
