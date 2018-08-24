package org.humanbrainproject.knowledgegraph.exceptions;

public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException() {
    }

    public InvalidPayloadException(String s) {
        super(s);
    }

    public InvalidPayloadException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidPayloadException(Throwable throwable) {
        super(throwable);
    }

    public InvalidPayloadException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
