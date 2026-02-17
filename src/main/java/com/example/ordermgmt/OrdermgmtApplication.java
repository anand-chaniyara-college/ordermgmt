package com.example.ordermgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrdermgmtApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdermgmtApplication.class, args);
	}

}
