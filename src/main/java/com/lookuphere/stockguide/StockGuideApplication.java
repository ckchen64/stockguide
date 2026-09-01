package com.lookuphere.stockguide;

//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//@SpringBootApplication
//public class StockguideApplication {
//
//	public static void main(String[] args) {
//		SpringApplication.run(StockguideApplication.class, args);
//	}
//
//}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // ⚠️ [필수 추가]

// (SPR-SEC-M3-1) [핵심] 스프링 부트가 켜질 때 시큐리티 보안 자동 설정(SecurityAutoConfiguration)을 아예 제외(exclude)하라고 지침을 내립니다.
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class StockGuideApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockGuideApplication.class, args);
		System.out.println("🚀 [인프라 통보] 스프링 시큐리티 검문소가 원천 해제되어 서버가 무사히 궤도에 진입했습니다!");
	}
}