package cn.edu.pku.pkurunner.Utils;

import android.util.Base64;
import io.reactivex.Observable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializeHelper {
    public static Observable<Serializable> stringToObject(String str) {
        try {
            return Observable.just((Serializable) new ObjectInputStream(new ByteArrayInputStream(Base64.decode(str, 0))).readObject());
        } catch (IOException | ClassCastException | ClassNotFoundException e2) {
            return Observable.error(e2);
        }
    }

    public static String objectToString(Serializable serializable) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);
            objectOutputStream.close();
            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0);
        } catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}
