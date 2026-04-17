package assignment.service;

import assignment.domain.SequenceRegistry;
import assignment.repository.SequenceRegistryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbSequenceGenerator implements SequenceGenerator {

    private final SequenceRegistryRepository repository;

    public DbSequenceGenerator(SequenceRegistryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public long nextValue(String sequenceType) {
        SequenceRegistry row = repository.findAndLock(sequenceType)
                .orElseThrow(() -> new UnknownSequenceTypeException(sequenceType));
        return row.advance();
    }
}
