package assignment;

import assignment.service.SequenceGenerator;
import assignment.service.UnknownSequenceTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        long userSecond = generator.nextValue("USER");

        assertThat(userSecond).isEqualTo(userFirst + 1);
        // USER and INVOICE counters don't interfere
        assertThat(invoiceFirst).isNotEqualTo(userFirst + 1000);
    }

    @Test
    void unknownTypeThrows() {
        assertThatThrownBy(() -> generator.nextValue("NOT_REGISTERED"))
                .isInstanceOf(UnknownSequenceTypeException.class);
    }
}
