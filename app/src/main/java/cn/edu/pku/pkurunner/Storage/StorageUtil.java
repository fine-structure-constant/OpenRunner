package cn.edu.pku.pkurunner.Storage;

import cn.edu.pku.pkurunner.Storage.StorageUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public abstract class StorageUtil {

    public interface Producer<T> {
        T produce() throws Throwable;
    }

    public static String sizeToReadableString(long j2) {
        char character;
        double value = j2;
        char[] cArr = {'B', 'K', 'M', 'G', 'T'};
        int index = 0;
        while (true) {
            if (index >= 5) {
                character = 'T';
                break;
            }
            character = cArr[index];
            if (value < 1024.0d) {
                break;
            }
            value /= 1024.0d;
            index++;
        }
        return String.format("%.1f %c", Double.valueOf(value), Character.valueOf(character));
    }

    public static <T> Observable<T> NetworkMethodWrapper(final Producer<T> producer) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                StorageUtil.b(producer, observableEmitter);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static /* synthetic */ void b(Producer producer, ObservableEmitter observableEmitter) {
        try {
            observableEmitter.onNext(producer.produce());
        } catch (Throwable th) {
            observableEmitter.onError(th);
        }
    }
}
