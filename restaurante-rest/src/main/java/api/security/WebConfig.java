package api.security;

import api.service.AuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el AuthInterceptor para todas las rutas /api/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService auth;

    public WebConfig(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new AuthInterceptor(auth)).addPathPatterns("/api/**");
    }
}
