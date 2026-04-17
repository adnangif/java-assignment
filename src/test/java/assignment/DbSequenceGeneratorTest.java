package assignment;

import assignment.service.SequenceGenerator;
import assignment.service.UnknownSequenceTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DbSequenceGeneratorTest {

    @Autowired
    SequenceGenerator generator;

    @Test
    void returnsMonotonicValuesPerType() {
        long a = generator.nextValue("ORDER");
        long b = generator.nextValue("ORDER");
        long c = generator.nextValue("ORDER");

        assertThat(b).isEqualTo(a + 1);
        assertThat(c).isEqualTo(b + 1);
    }

    @Test
    void sequencesAreIndependentPerType() {
        long userFirst = generator.nextValue("USER");
        long invoiceFirst = generator.nextValue("INVOICE");
        long invoiceSecond = generator.nextValue("INVOICE");
        long userSecond = generator.nextValue("USER");

        assertThat(userSecond).isEqualTo(userFirst + 1);
        assertThat(invoiceSecond).isEqualTo(invoiceFirst + 1);
        assertThat(userSecond).isNotEqualTo(invoiceFirst);
    }

    @Test
    void unknownTypeThrows() {
        assertThatThrownBy(() -> generator.nextValue("NOT_REGISTERED"))
                .isInstanceOf(UnknownSequenceTypeException.class);
    }

    @Test
    void firstValueStartsAtOne() {
        long first = generator.nextValue("INVOICE");
        assertThat(first).isEqualTo(1);
    }

    @Test
    void nullTypeThrows() {
        assertThatThrownBy(() -> generator.nextValue(null))
                .isInstanceOf(UnknownSequenceTypeException.class);
    }

    @Test
    void emptyStringTypeThrows() {
        assertThatThrownBy(() -> generator.nextValue(""))
                .isInstanceOf(UnknownSequenceTypeException.class);
    }

    @Test
    void caseSensitiveTypeLookup() {
        assertThatThrownBy(() -> generator.nextValue("order"))
                .isInstanceOf(UnknownSequenceTypeException.class);
    }

    @Test
    void concurrentMultiTypeSequences() throws Exception {
        int threadsPerType = 8;
        int callsPerThread = 100;
        int totalPerType = threadsPerType * callsPerThread;

        long orderBaseline = generator.nextValue("ORDER");
        long userBaseline = generator.nextValue("USER");

        Set<Long> orderSeen = ConcurrentHashMap.newKeySet();
        Set<Long> userSeen = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();

        int totalThreads = threadsPerType * 2;
        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch ready = new CountDownLatch(totalThreads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(totalThreads);

        for (int t = 0; t < threadsPerType; t++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    for (int i = 0; i < callsPerThread; i++) {
                        orderSeen.add(generator.nextValue("ORDER"));
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    for (int i = 0; i < callsPerThread; i++) {
                        userSeen.add(generator.nextValue("USER"));
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
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(failures.get()).isZero();

        assertThat(orderSeen).hasSize(totalPerType);
        List<Long> sortedOrder = new ArrayList<>(orderSeen).stream().sorted().toList();
        for (int i = 0; i < sortedOrder.size(); i++) {
            assertThat(sortedOrder.get(i)).isEqualTo(orderBaseline + 1 + i);
        }

        assertThat(userSeen).hasSize(totalPerType);
        List<Long> sortedUser = new ArrayList<>(userSeen).stream().sorted().toList();
        for (int i = 0; i < sortedUser.size(); i++) {
            assertThat(sortedUser.get(i)).isEqualTo(userBaseline + 1 + i);
        }
    }
}
