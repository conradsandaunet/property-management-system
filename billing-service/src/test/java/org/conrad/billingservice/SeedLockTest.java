package org.conrad.billingservice;

import org.conrad.security.SeedLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Needs the Postgres from docker-compose (the lock is a Postgres advisory lock).
@SpringBootTest
class SeedLockTest {

    @Autowired SeedLock seedLock;

    @Test
    void onlyOneCallerRunsTheActionAtATime() throws Exception {
        int callers = 8;
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger mostAtOnce = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    seedLock.runExclusively(() -> {
                        int now = inside.incrementAndGet();
                        mostAtOnce.accumulateAndGet(now, Math::max);
                        Thread.sleep(40); // long enough for the others to pile up behind the lock
                        inside.decrementAndGet();
                        completed.incrementAndGet();
                    });
                    return null;
                }));
            }
            go.countDown();
            for (Future<?> f : futures) f.get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(completed.get()).isEqualTo(callers);
        assertThat(mostAtOnce.get()).as("callers inside the lock at the same time").isEqualTo(1);
    }

    @Test
    void theLockIsReleasedWhenTheActionFails() {
        assertThatThrownBy(() -> seedLock.runExclusively(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        // Would block forever if the failed call still held the lock.
        seedLock.runExclusively(() -> { });
    }
}
