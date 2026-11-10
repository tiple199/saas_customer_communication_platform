package vn.lnt.saas_customer_communication_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaasCustomerCommunicationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaasCustomerCommunicationPlatformApplication.class, args);
	}

}
