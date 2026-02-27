package igrus.web;

import igrus.web.common.config.TestExternalServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalServiceConfig.class)
class IgrusWebApplicationTests {

	@Test
	void contextLoads() {
	}

}
