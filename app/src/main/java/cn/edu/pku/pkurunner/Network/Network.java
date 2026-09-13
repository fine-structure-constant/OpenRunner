package cn.edu.pku.pkurunner.Network;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import cn.edu.pku.pkurunner.BuildConfig;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Exception.ServerException;
import cn.edu.pku.pkurunner.MainApplication;
import cn.edu.pku.pkurunner.Model.GymRecord;
import cn.edu.pku.pkurunner.Model.Point;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Model.Task;
import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.DataPack;
import cn.edu.pku.pkurunner.Network.Model.AMapReverseEncoding;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import cn.edu.pku.pkurunner.Network.Model.Version;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Network.Service.ActivityService;
import cn.edu.pku.pkurunner.Network.Service.GymRecordService;
import cn.edu.pku.pkurunner.Network.Service.LoginService;
import cn.edu.pku.pkurunner.Network.Service.RecordService;
import cn.edu.pku.pkurunner.Network.Service.TaskService;
import cn.edu.pku.pkurunner.Network.Service.WeatherService;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.SecUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class Network {

    /* renamed from: a, reason: collision with root package name */
    private static WeatherService f6984a = null;
    public static final String announcementUrl = "https://pkunewyouth.pku.edu.cn/public/htmls/requirement.html?t=" + System.currentTimeMillis();

    /* renamed from: b, reason: collision with root package name */
    private static TaskService f6985b = null;

    /* renamed from: c, reason: collision with root package name */
    private static LoginService f6986c = null;

    /* renamed from: d, reason: collision with root package name */
    private static RecordService f6987d = null;

    /* renamed from: e, reason: collision with root package name */
    private static GymRecordService f6988e = null;

    /* renamed from: f, reason: collision with root package name */
    private static ActivityService f6989f = null;
    public static final String photoBaseUrl = "https://pkunewyouth.pku.edu.cn/";
    public static Weather weather;

    private static class b implements Function {
        private b() {
        }

        @Override // io.reactivex.functions.Function
        /* renamed from: a, reason: merged with bridge method [inline-methods] */
        public Object apply(Object value) {
            DataPack dataPack = (DataPack) value;
            if (dataPack.isSuccess()) {
                return Observable.just(dataPack.getData());
            }
            return Observable.error(new ServerException(dataPack.getCode(), dataPack.getMessage()));
        }
    }

    public static Observable<Boolean> clearActivity20180420() {
        return f6989f.clear20180420(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new Function() { // from class: s.h
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource m2;
                m2 = Network.m((DataPack) obj);
                return m2;
            }
        });
    }

    public static Observable<ArrayList<GymRecord>> getGymRecords() {
        return f6988e.getGymRecords(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.f
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource n2;
                n2 = Network.n((ArrayList) obj);
                return n2;
            }
        });
    }

    public static Observable<Version> getLatestVersion(boolean z2) {
        return (z2 ? f6986c.getLatestVersionForOffline() : f6986c.getLatestVersion()).subscribeOn(Schedulers.newThread());
    }

    public static Observable<Version> getMinVersion() {
        return f6986c.getMinVersion().subscribeOn(Schedulers.newThread());
    }

    public static Observable<ArrayList<Record>> getRecords(String str) {
        return f6987d.getRecords(str).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.e
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource o2;
                o2 = Network.o((ArrayList) obj);
                return o2;
            }
        });
    }

    public static Observable<String> getReverseEncoding(Point point) {
        return f6987d.reverseEncoding(String.format("%.6f,%.6f", Double.valueOf(point.getLongitude()), Double.valueOf(point.getLatitude())), BuildConfig.AMAP_WEB_KEY, 1000).subscribeOn(Schedulers.newThread()).flatMap(new Function() { // from class: s.d
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                return ((AMapReverseEncoding) obj).getStreetName();
            }
        });
    }

    public static Observable<Record> getSingleRecord(String str, int i2) {
        return f6987d.getSingleRecord(str, i2).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.m
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource p2;
                p2 = Network.p((Record.Inner) obj);
                return p2;
            }
        });
    }

    public static Observable<ArrayList<Task>> getTasks() {
        return f6985b.getList(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.j
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource q2;
                q2 = Network.q((Map) obj);
                return q2;
            }
        });
    }

    public static Observable<UserStatus> getUserStatus() {
        return f6987d.getStatus(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new b());
    }

    public static Observable<Weather> getWeather() {
        return f6984a.getWeather().subscribeOn(Schedulers.newThread()).doOnNext(new Consumer() { // from class: s.l
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                Network.weather = (Weather) obj;
            }
        });
    }

    public static void init(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() { // from class: s.i
            @Override // okhttp3.Interceptor
            public final Response intercept(Interceptor.Chain chain) throws java.io.IOException {
                Response s2;
                s2 = Network.s(chain);
                return s2;
            }
        });
        Retrofit build = new Retrofit.Builder().baseUrl(photoBaseUrl).addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava2CallAdapterFactory.create()).client(builder.build()).build();
        f6984a = (WeatherService) build.create(WeatherService.class);
        f6985b = (TaskService) build.create(TaskService.class);
        f6986c = (LoginService) build.create(LoginService.class);
        f6987d = (RecordService) build.create(RecordService.class);
        f6988e = (GymRecordService) build.create(GymRecordService.class);
        f6989f = (ActivityService) build.create(ActivityService.class);
    }

    public static void interceptIfSocketTimeout(Throwable th, Callback.Callable<Void> callable) {
        if (!(th instanceof SocketTimeoutException)) {
            callable.call(null);
        } else {
            MainApplication context = MainApplication.getContext();
            Toast.makeText(context, context.getString(R.string.g_error_sockettimeout), 1).show();
        }
    }

    public static Observable<User> loginNew(String str) {
        return f6986c.login(str).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.k
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource t2;
                t2 = Network.t((User.Inner) obj);
                return t2;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource n(ArrayList arrayList) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(new GymRecord((GymRecord.Inner) it.next()));
        }
        return Observable.just(arrayList2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource o(ArrayList arrayList) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(new Record((Record.Inner) it.next()));
        }
        return Observable.just(arrayList2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource p(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource q(Map map) {
        return Observable.just(new ArrayList(map.values()));
    }

    public static Observable<Boolean> signUpActivity20180420(boolean z2) {
        return f6989f.signUp20180420(20180420, Data.getUser().getId(), z2 ? "red" : "blue").subscribeOn(Schedulers.newThread()).flatMap(new Function() { // from class: s.g
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource u2;
                u2 = Network.u((DataPack) obj);
                return u2;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource t(User.Inner inner) {
        return Observable.just(new User(inner));
    }

    public static Observable<GymRecord> uploadGymRecordGetOut(int i2, String str) {
        return f6988e.verifyGymRecord(Data.getUser().getId(), i2, str).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.a
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource v2;
                v2 = Network.v((GymRecord.Inner) obj);
                return v2;
            }
        });
    }

    public static Observable<Record> uploadRecord(Record record, File file) {
        record.setStep(Math.round(record.getStep() / 17) * 17);
        JSONArray jSONArray = new JSONArray();
        try {
            Iterator<Point> it = record.getTrack().iterator();
            while (it.hasNext()) {
                jSONArray.put(it.next().toJSONArray());
            }
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("agent", "Android v1.2+");
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
            if (SecUtil.verifyCheckField(record.getUserId(), record.getDate(), record.getCheckField())) {
                if (file == null) {
                    return f6987d.uploadRecordWithoutPhoto(Data.getUser().getId(), record.getDuration(), record.getDistance(), record.getDate().getTime(), jSONArray.toString(), jSONObject.toString(), record.getStep(), SecUtil.getAbstract(Data.getUser().getId(), String.valueOf(record.getDate().getTime()))).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.b
                        @Override // io.reactivex.functions.Function
                        public final Object apply(Object obj) {
                            ObservableSource w2;
                            w2 = Network.w((Record.Inner) obj);
                            return w2;
                        }
                    });
                }
                return f6987d.uploadRecord(Data.getUser().getId(), record.getDuration(), record.getDistance(), record.getDate().getTime(), jSONArray.toString(), jSONObject.toString(), record.getStep(), SecUtil.getAbstract(Data.getUser().getId(), String.valueOf(record.getDate().getTime())), RequestBody.create(MediaType.parse("image/jpeg"), file)).subscribeOn(Schedulers.newThread()).flatMap(new b()).flatMap(new Function() { // from class: s.c
                    @Override // io.reactivex.functions.Function
                    public final Object apply(Object obj) {
                        ObservableSource x2;
                        x2 = Network.x((Record.Inner) obj);
                        return x2;
                    }
                });
            }
            StringBuilder sb = new StringBuilder(12);
            sb.append("Pa");
            sb.append("rs");
            sb.length();
            sb.append("e F");
            sb.append("ail");
            sb.append("ed");
            throw new RuntimeException(sb.toString());
        } catch (JSONException e3) {
            e3.printStackTrace();
            return Observable.error(e3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource v(GymRecord.Inner inner) {
        return Observable.just(new GymRecord(inner));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource w(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource x(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource m(DataPack dataPack) {
        return Observable.just(Boolean.valueOf(dataPack.isSuccess()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ Response s(Interceptor.Chain chain) throws java.io.IOException {
        Request.Builder newBuilder = chain.request().newBuilder();
        if (Data.getUser() != null) {
            newBuilder = newBuilder.header("Authorization", Data.getUser().getToken());
        }
        return chain.proceed(newBuilder.header("Platform", "Android").header("Manufacturer", Build.MANUFACTURER).header("ClientVersion", BuildConfig.VERSION_NAME).build());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource u(DataPack dataPack) {
        return Observable.just(Boolean.valueOf(dataPack.isSuccess()));
    }
}
