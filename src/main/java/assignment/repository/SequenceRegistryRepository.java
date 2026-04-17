package assignment.repository;

import assignment.domain.SequenceRegistry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SequenceRegistryRepository extends JpaRepository<SequenceRegistry, String> {

    // Issues SELECT ... FOR UPDATE on the sequence row so concurrent callers
    // for the same type serialise at the DB layer.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SequenceRegistry s WHERE s.sequenceType = :type")
    Optional<SequenceRegistry> findAndLock(@Param("type") String type);
}
