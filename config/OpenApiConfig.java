@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Price Management API")
                        .version("1.0")
                        .description("Price Management System API Documentation"));
    }
} 