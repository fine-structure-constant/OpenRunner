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
        char c2;
        double d2 = j2;
        char[] cArr = {'B', 'K', 'M', 'G', 'T'};
        int i2 = 0;
        while (true) {
            if (i2 >= 5) {
                c2 = 'T';
                break;
            }
            c2 = cArr[i2];
            if (d2 < 1024.0d) {
                break;
            }
            d2 /= 1024.0d;
            i2++;
        }
        return String.format("%.1f %c", Double.valueOf(d2), Character.valueOf(c2));
    }

    public static <T> Observable<T> NetworkMethodWrapper(final Producer<T> producer) {
        return Observable.create(new ObservableOnSubscribe() { // from class: x.b
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                StorageUtil.b(producer, observableEmitter);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void b(Producer producer, ObservableEmitter observableEmitter) {
        try {
            observableEmitter.onNext(producer.produce());
        } catch (Throwable th) {
            observableEmitter.onError(th);
        }
    }
}
