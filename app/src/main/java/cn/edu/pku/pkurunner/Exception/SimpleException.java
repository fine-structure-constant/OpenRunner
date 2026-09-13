package cn.edu.pku.pkurunner.Exception;

public abstract class SimpleException extends RuntimeException {
    protected int errorCode;

    public SimpleException(int i2) {
        super("SimpleException");
        this.errorCode = i2;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public SimpleException(int i2, String str) {
        super(str);
        this.errorCode = i2;
    }

    public SimpleException(int i2, String str, Throwable th) {
        super(str, th);
        this.errorCode = i2;
    }
}
