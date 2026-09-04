package com.lookuphere.stockguide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class StockGuideApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockGuideApplication.class, args);
	}
}