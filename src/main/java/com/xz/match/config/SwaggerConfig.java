package com.xz.match.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"local"})
@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI().
				info(new Info().title("new match")
						.description("新的api文档")
						.version("1.0")
						.license(new License().name("Apache 2.0").url("https://springdoc.org"))) // 许可证
				.externalDocs(new ExternalDocumentation()
						.description("外部文档")
						.url("https://springshop.wiki.github.org/docs")); // 外链资源
	}

	// http://localhost:8080/swagger-ui/index.html
	// http://127.0.0.1:8080/doc.html
	// 接口文档, 是接口才能用,mapping懂吗
}

