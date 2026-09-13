package cn.edu.pku.pkurunner.Network;

public class DataPack<T> {
    private int code;
    private T data;
    private String message;
    private boolean success;
    private int version;

    public int getCode() {
        return this.code;
    }

    public T getData() {
        return this.data;
    }

    public String getMessage() {
        return this.message;
    }

    public int getVersion() {
        return this.version;
    }

    public boolean isSuccess() {
        return this.success;
    }
}
