package cn.edu.pku.pkurunner.Storage;

import android.content.Context;
import android.content.SharedPreferences;
import cn.edu.pku.pkurunner.Storage.Dropbox;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class Dropbox {

    public static abstract class APIWrapper {

        public static class DropboxException extends Exception {
            public static final int NO_ACCESS_TOKEN = 1;

            /* renamed from: a, reason: collision with root package name */
            private int f7086a;

            public DropboxException(String str, int i2) {
                super(str);
                this.f7086a = i2;
            }

            public int getCode() {
                return this.f7086a;
            }

            public DropboxException(int i2) {
                this.f7086a = i2;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void b(Context context, ObservableEmitter observableEmitter) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("storage-dropbox", 0);
            String string = sharedPreferences.getString("access-token", null);
            if (string == null && (string = Auth.getOAuth2Token()) != null) {
                sharedPreferences.edit().putString("access-token", string).apply();
            }
            if (string == null) {
                observableEmitter.onError(new DropboxException(1));
                return;
            }
            String uid = Auth.getUid();
            String string2 = sharedPreferences.getString("user-id", null);
            if (uid != null && !uid.equals(string2)) {
                sharedPreferences.edit().putString("user-id", uid).apply();
            }
            ClientFactory.init(string);
            observableEmitter.onNext(string);
        }

        public static Observable<String> getToken(final Context context) {
            return Observable.create(new ObservableOnSubscribe() { // from class: x.a
                @Override // io.reactivex.ObservableOnSubscribe
                public final void subscribe(ObservableEmitter observableEmitter) {
                    Dropbox.APIWrapper.b(context, observableEmitter);
                }
            }).subscribeOn(AndroidSchedulers.mainThread());
        }

        public static boolean hasToken(Context context) {
            return context.getSharedPreferences("storage-dropbox", 0).getString("access-token", null) != null;
        }
    }

    public static abstract class ClientFactory {

        /* renamed from: a, reason: collision with root package name */
        private static DbxClientV2 f7087a;

        public static DbxClientV2 getClient() {
            DbxClientV2 dbxClientV2 = f7087a;
            if (dbxClientV2 != null) {
                return dbxClientV2;
            }
            throw new IllegalStateException("Client not initialized.");
        }

        public static void init(String str) {
            if (f7087a == null) {
                f7087a = new DbxClientV2(DbxRequestConfig.newBuilder("PKU-Runner-Android-v1.2").withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient())).build(), str);
            }
        }
    }
}
