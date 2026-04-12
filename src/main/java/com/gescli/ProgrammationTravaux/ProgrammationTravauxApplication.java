package com.gescli.ProgrammationTravaux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProgrammationTravauxApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProgrammationTravauxApplication.class, args);
	}

}
