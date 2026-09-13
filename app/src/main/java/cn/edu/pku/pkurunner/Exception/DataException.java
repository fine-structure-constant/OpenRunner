package cn.edu.pku.pkurunner.Exception;

public class DataException extends SimpleException {
    public static final int Database_Delete_Failure = 8;
    public static final int Database_Open_Failure = 1;
    public static final int Database_Update_Failure = 4;
    public static final int Database_Write_Failure = 2;
    public static final int No_User_Has_The_ID = 128;
    public static final int Record_Has_Uploaded = 64;
    public static final int Record_Not_Found = 32;
    public static final int Record_Not_Uploaded = 16;

    public DataException(int i2) {
        super(i2);
    }

    public DataException(int i2, String str) {
        super(i2, str);
    }

    public DataException(int i2, String str, Throwable th) {
        super(i2, str, th);
    }
}
