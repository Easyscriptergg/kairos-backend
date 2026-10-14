package com.kairos.backend.auth;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.kairos.backend.common.ApiException;

/**
 * Resolves {@link CurrentUser} parameters from the bearer token's subject.
 * Every owned record is read and written through this id, which is what keeps
 * one athlete's data out of another's requests.
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final UserAccountRepository users;

    public CurrentUserResolver(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw ApiException.unauthorized("Sign in to continue.");
        }
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.unauthorized("The access token is not valid.");
        }
        if (!users.existsById(userId)) {
            throw ApiException.unauthorized("This account no longer exists.");
        }
        return userId;
    }
}
