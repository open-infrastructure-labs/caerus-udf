package com.example.thumbnail;

import com.example.thumbnail.service.ThumbnailGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import function.Handler;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ThumbnailApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThumbnailApplication.class, args);
	}

	@Bean
	Handler getHandler() {
		return new Handler();
	}
}
