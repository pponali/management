@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Price Management API")
                        .version("1.0")
                        .description("This API manages pricing rules, constraints, and templates for the Price Management System. It provides endpoints for creating, updating, and retrieving pricing data, ensuring efficient and accurate price management across various platforms."));
    }
} 