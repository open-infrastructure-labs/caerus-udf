package com.example.thumbnail;

import com.example.thumbnail.service.ThumbnailGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ThumbnailApplication {

	public static void main(String[] args) {
		/*try {
			ThumbnailGenerator.main_test();
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		SpringApplication.run(ThumbnailApplication.class, args);
	}

}
