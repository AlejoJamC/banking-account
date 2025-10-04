package com.waes.rabobank.bankingaccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BankingAccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingAccountApplication.class, args);
	}

}
