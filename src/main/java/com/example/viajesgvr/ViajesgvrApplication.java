package com.example.viajesgvr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ViajesgvrApplication {

	public static void main(String[] args) {
		SpringApplication.run(ViajesgvrApplication.class, args);
	}

}