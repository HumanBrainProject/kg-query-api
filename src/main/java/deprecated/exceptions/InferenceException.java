package deprecated.exceptions;

public class InferenceException extends RuntimeException {

    public InferenceException() {
    }

    public InferenceException(String s) {
        super(s);
    }

    public InferenceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InferenceException(Throwable throwable) {
        super(throwable);
    }

    public InferenceException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
