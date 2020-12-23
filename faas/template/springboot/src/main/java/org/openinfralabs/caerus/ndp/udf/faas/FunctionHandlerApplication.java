package org.openinfralabs.caerus.ndp.udf.faas;

import function.Handler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FunctionHandlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunctionHandlerApplication.class, args);
	}

	@Bean
	Handler getHandler() {
		return new Handler();
	}
}
