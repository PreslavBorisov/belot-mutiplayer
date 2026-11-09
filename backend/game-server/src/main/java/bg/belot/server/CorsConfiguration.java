package bg.belot.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Explicit frontend origin; no wildcard CORS on the public game API. */
@Configuration
public class CorsConfiguration {
 @Bean
 WebMvcConfigurer corsConfigurer(@Value("${BELOT_FRONTEND_ORIGIN:http://localhost:5173}") String frontendOrigin) {
  return new WebMvcConfigurer() {
   @Override public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOrigins(frontendOrigin)
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("Content-Type");
   }
  };
 }
}
