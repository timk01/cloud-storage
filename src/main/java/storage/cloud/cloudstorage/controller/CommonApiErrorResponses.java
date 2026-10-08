package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request"
        ),
        @ApiResponse(
                responseCode = "401",
                description = "User is not authorized"
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Unknown server error"
        )
})
public @interface CommonApiErrorResponses {
}
