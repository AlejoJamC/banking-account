package com.waes.rabobank.bankingaccount;

import org.springframework.boot.SpringApplication;

public class TestBankingAccountApplication {

	public static void main(String[] args) {
		SpringApplication.from(BankingAccountApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
