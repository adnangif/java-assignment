package assignment.service;

public class UnknownSequenceTypeException extends RuntimeException {
    public UnknownSequenceTypeException(String sequenceType) {
        super("Unknown sequence type: " + sequenceType);
    }
}
