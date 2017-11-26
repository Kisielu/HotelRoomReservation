package com.kisielewicz.finanteq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan({"com.kisielewicz.finanteq.domain", "com.kisielewicz.finanteq.repository",
	"com.kisielewicz.finanteq.service", "com.kisielewicz.finanteq.spring.configuration", "com.kisielewicz.finanteq.web",
	"com.kisielewicz.finanteq.helpers"})
@EnableSpringDataWebSupport
@EnableAutoConfiguration(exclude = RepositoryRestMvcAutoConfiguration.class)
public class FinanteqApplication extends SpringBootServletInitializer {
//public class FinanteqApplication implements CommandLineRunner {
//	@Autowired
//	private static ReservationService reservationService;
//
//	@Autowired
//	private static RoomService roomService;

	public static void main(String[] args) {
		SpringApplication.run(FinanteqApplication.class, args);

//		SpringApplication app = new SpringApplication(FinanteqApplication.class);
//		app.setBannerMode(Banner.Mode.OFF);
//		app.run(args);
	}

	@Bean
	public ObjectMapper objectMapper() {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		return objectMapper;
	}

//	@Override
//	public void run(String... strings) throws Exception {
//
//	}
}
