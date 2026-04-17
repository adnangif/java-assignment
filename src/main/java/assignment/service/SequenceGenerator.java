package assignment.service;

public interface SequenceGenerator {

    /**
     * Returns the next unique value for the given sequence type.
     * Uniqueness holds across threads, JVMs and restarts, provided all
     * callers share the same database.
     *
     * @throws UnknownSequenceTypeException if the type is not registered
     */
    long nextValue(String sequenceType);
}
