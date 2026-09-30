package org.conrad.reservationservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

@SpringBootApplication
@ComponentScan(basePackages = {"org.conrad.reservationservice", "org.conrad.security"})
public class ReservationServiceApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ReservationServiceApplication.class, args);
    }

    private static void loadDotenv() {
        for (String dir : new String[]{".", "reservation-service"}) {
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


