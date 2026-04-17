package assignment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sequence_registry")
public class SequenceRegistry {

    @Id
    @Column(name = "sequence_type", length = 64)
    private String sequenceType;

    @Column(name = "current_value", nullable = false)
    private long currentValue;

    @Column(name = "increment_by", nullable = false)
    private long incrementBy;

    protected SequenceRegistry() {
    }

    public SequenceRegistry(String sequenceType, long currentValue, long incrementBy) {
        this.sequenceType = sequenceType;
        this.currentValue = currentValue;
        this.incrementBy = incrementBy;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public long getIncrementBy() {
        return incrementBy;
    }

    public long advance() {
        this.currentValue += this.incrementBy;
        return this.currentValue;
    }
}
