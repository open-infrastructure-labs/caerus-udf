package org.openinfralabs.caerus.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@SpringBootApplication
@EnableSwagger2
public class UdfServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UdfServiceApplication.class, args);
	}

	@Bean
	public Docket swaggerConfiguration() {
		// return a prepared Docker instance
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.paths(PathSelectors.any())
				.apis(RequestHandlerSelectors.basePackage("org.openinfralabs.caerus"))
				.build()
				.apiInfo(apiDetails());
	}

	private ApiInfo apiDetails() {
		return new ApiInfo(
				"Caerus UDF Registry APIs",
				"Rest APIs for managing UDF registry backed by redis",
				"1.0",
				"Free to use",
				new springfox.documentation.service.Contact("Yong Wang", "https://openinfralabs.org/", "yong.wang@futurewei.com"),
				"Apache License 2.0",
				"https://openinfralabs.org/",
				Collections.emptyList());
	}

}
