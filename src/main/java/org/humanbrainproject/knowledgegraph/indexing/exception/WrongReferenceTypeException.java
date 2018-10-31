package org.humanbrainproject.knowledgegraph.indexing.exception;

public class WrongReferenceTypeException extends RuntimeException{
    public WrongReferenceTypeException() {
    }

    public WrongReferenceTypeException(String s) {
        super(s);
    }

    public WrongReferenceTypeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public WrongReferenceTypeException(Throwable throwable) {
        super(throwable);
    }

    public WrongReferenceTypeException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}