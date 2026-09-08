package org.conrad.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;

@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(BillingServiceApplication.class, args);
    }

    private static void loadDotenv() {
        for (String dir : new String[]{".", "billing-service"}) {
            if (new File(dir, ".env").isFile()) {
                Dotenv dotenv = Dotenv.configure().directory(dir).ignoreIfMissing().load();
                dotenv.entries().forEach(entry -> {
                    if (System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
                return;
            }
        }
    }

}
