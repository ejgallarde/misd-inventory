package ph.gov.phlpost.inventory.misddashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MisdInventoryDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(MisdInventoryDashboardApplication.class, args);
	}

}
