package storage.cloud.cloudstorage.config;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer customizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {

                    Content errorContent = new Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                    );

                    if (operation.getResponses().containsKey("404")) {
                        operation.getResponses().get("404").setContent(errorContent);
                    }
                    if (operation.getResponses().containsKey("409")) {
                        operation.getResponses().get("409").setContent(errorContent);
                    }
                })
        );
    }
}
