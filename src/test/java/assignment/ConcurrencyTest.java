package assignment;

import assignment.service.SequenceGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load-bearing test for the pessimistic-lock design:
 * N threads hammer nextValue("ORDER") concurrently and we assert every
 * returned value is unique and the set is contiguous {start+1 .. start+N*M}.
 *
 * Runs on H2 (FOR UPDATE honoured). In production against MySQL/InnoDB the
 * same guarantees hold via row-level locks.
 */
@SpringBootTest
class ConcurrencyTest {

    private static final int THREADS = 16;
    private static final int CALLS_PER_THREAD = 500;
    private static final int TOTAL = THREADS * CALLS_PER_THREAD;
    private static final String TYPE = "ORDER";

    @Autowired
    SequenceGenerator generator;

    @Test
    void noDuplicatesUnderConcurrentLoad() throws Exception {
        long baseline = generator.nextValue(TYPE); // pin the starting point

        Set<Long> seen = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        for (int t = 0; t < THREADS; t++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    for (int i = 0; i < CALLS_PER_THREAD; i++) {
                        seen.add(generator.nextValue(TYPE));
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).as("workers finished in time").isTrue();
        pool.shutdownNow();

        assertThat(failures.get()).as("no thread saw an exception").isZero();
        assertThat(seen).as("no duplicates").hasSize(TOTAL);

        List<Long> sorted = seen.stream().sorted().toList();
        assertThat(sorted.get(0)).isEqualTo(baseline + 1);
        assertThat(sorted.get(sorted.size() - 1)).isEqualTo(baseline + TOTAL);
        // Contiguous: no gaps between baseline+1 and baseline+TOTAL
        for (int i = 0; i < sorted.size(); i++) {
            assertThat(sorted.get(i)).isEqualTo(baseline + 1 + i);
        }
    }
}
