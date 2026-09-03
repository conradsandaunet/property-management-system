package org.conrad.residentservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class ResidentServiceApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ResidentServiceApplication.class, args);
    }

    // Looks in the current working directory first, then in resident-service/ —
    // covers both running from inside the module and from the multi-module root
    // (e.g. IntelliJ's default run configuration working directory vs. mvn -pl).
    private static void loadDotenv() {
        for (String dir : new String[]{".", "resident-service"}) {
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
