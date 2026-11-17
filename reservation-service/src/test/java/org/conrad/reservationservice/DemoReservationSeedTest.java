package org.conrad.reservationservice;

import org.conrad.reservationservice.config.DemoReservationSeed;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Needs the Postgres from docker-compose (advisory locks are Postgres-specific).
@SpringBootTest
class DemoReservationSeedTest {

    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ResourceRepository resources;
    @Autowired ReservationRepository reservations;

    boolean seededByTest;

    @AfterEach
    void tearDown() {
        if (seededByTest) {
            reservations.deleteAll();
            resources.deleteAll();
        }
    }

    @Test
    void instancesStartingAtTheSameTimeSeedOnlyOnce() throws Exception {
        // Only meaningful on an empty database (CI); never wipe a developer's data.
        assumeTrue(resources.count() == 0, "database already has resources");
        seededByTest = true;
        // Built by hand: the bean itself is switched off in tests (app.seed.demo-data=false).
        DemoReservationSeed seed = new DemoReservationSeed(resources, reservations, entityManager, transactionManager);

        int instances = 4;
        ExecutorService pool = Executors.newFixedThreadPool(instances);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < instances; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    seed.run(new DefaultApplicationArguments());
                    return null;
                }));
            }
            go.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(resources.count()).isEqualTo(5);
        assertThat(reservations.count()).isEqualTo(4);
    }
}
