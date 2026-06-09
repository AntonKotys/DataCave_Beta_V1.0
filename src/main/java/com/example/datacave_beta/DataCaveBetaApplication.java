package com.example.datacave_beta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataCaveBetaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataCaveBetaApplication.class, args);
	}

}
