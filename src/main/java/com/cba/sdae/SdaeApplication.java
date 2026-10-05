package com.cba.sdae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SdaeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SdaeApplication.class, args);
	}

}

