package cn.edu.pku.pkurunner.Exception;

import android.content.res.Resources;
import android.util.SparseIntArray;
import cn.edu.pku.pkurunner.R;

public class ServerException extends SimpleException {

    private static Resources resources;

    private static final SparseIntArray MESSAGE_RES_IDS;

    public static void setResources(Resources resources) {
        ServerException.resources = resources;
    }

    @Override
    public String getLocalizedMessage() {
        return resources.getString(MESSAGE_RES_IDS.get(this.errorCode, R.string.e_server_n1));
    }

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        MESSAGE_RES_IDS = sparseIntArray;
        sparseIntArray.put(-1, R.string.e_server_n1);
        sparseIntArray.put(0, R.string.e_server_0);
        sparseIntArray.put(1, R.string.e_server_1);
        sparseIntArray.put(2, R.string.e_server_2);
        sparseIntArray.put(3, R.string.e_server_3);
        sparseIntArray.put(4, R.string.e_server_4);
        sparseIntArray.put(5, R.string.e_server_5);
        sparseIntArray.put(6, R.string.e_server_6);
        sparseIntArray.put(7, R.string.e_server_7);
        sparseIntArray.put(8, R.string.e_server_8);
        sparseIntArray.put(9, R.string.e_server_9);
        sparseIntArray.put(10, R.string.e_server_10);
        sparseIntArray.put(11, R.string.e_server_11);
        sparseIntArray.put(12, R.string.e_server_12);
        sparseIntArray.put(13, R.string.e_server_13);
        sparseIntArray.put(14, R.string.e_server_14);
        sparseIntArray.put(15, R.string.e_server_15);
        sparseIntArray.put(16, R.string.e_server_16);
        sparseIntArray.put(17, R.string.e_server_17);
        sparseIntArray.put(18, R.string.e_server_18);
        sparseIntArray.put(19, R.string.e_server_19);
        sparseIntArray.put(20, R.string.e_server_20);
        sparseIntArray.put(21, R.string.e_server_21);
        sparseIntArray.put(22, R.string.e_server_22);
        sparseIntArray.put(23, R.string.e_server_23);
        sparseIntArray.put(24, R.string.e_server_24);
    }

    public static String getLocalizedMessage(int index) {
        return resources.getString(MESSAGE_RES_IDS.get(index, R.string.e_server_n1));
    }

    public ServerException(int index, String str) {
        super(index, str);
    }
}
