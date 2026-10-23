package org.conrad.gateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

@SpringBootApplication
@ComponentScan(basePackages = {"org.conrad.gateway", "org.conrad.security"})
public class GatewayApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(GatewayApplication.class, args);
    }

    // Looks in the current working directory first, then in gateway/ —
    private static void loadDotenv() {
        for (String dir : new String[]{".", "gateway"}) {
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
