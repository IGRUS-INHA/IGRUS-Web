package igrus.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IgrusWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(IgrusWebApplication.class, args);
	}

}
