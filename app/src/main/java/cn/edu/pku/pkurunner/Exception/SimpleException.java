package cn.edu.pku.pkurunner.Exception;

public abstract class SimpleException extends RuntimeException {
    protected int errorCode;

    public SimpleException(int index) {
        super("SimpleException");
        this.errorCode = index;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public SimpleException(int index, String str) {
        super(str);
        this.errorCode = index;
    }

    public SimpleException(int index, String str, Throwable th) {
        super(str, th);
        this.errorCode = index;
    }
}
