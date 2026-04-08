package com.example.template;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.cloud.gcp.sql.enabled=false",
	"spring.jpa.hibernate.ddl-auto=validate",
	"apollo.scraper.scheduling.enabled=false"
})
class TemplateApplicationTests {

	@Test
	void contextLoads() {
	}

}
