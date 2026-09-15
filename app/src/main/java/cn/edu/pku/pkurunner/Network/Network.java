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
import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.DataPack;
import cn.edu.pku.pkurunner.Network.Model.AMapReverseEncoding;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import cn.edu.pku.pkurunner.Network.Model.Version;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Network.Service.GymRecordService;
import cn.edu.pku.pkurunner.Network.Service.LoginService;
import cn.edu.pku.pkurunner.Network.Service.RecordService;
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

    private static WeatherService weatherService = null;
    public static final String announcementUrl = "https://pkunewyouth.pku.edu.cn/public/htmls/requirement.html?t=" + System.currentTimeMillis();

    private static LoginService loginService = null;

    private static RecordService recordService = null;

    private static GymRecordService gymRecordService = null;

    public static final String photoBaseUrl = "https://pkunewyouth.pku.edu.cn/";
    public static Weather weather;

    private static class DataPackUnwrapFunction implements Function {
        private DataPackUnwrapFunction() {
        }

        @Override
        public Object apply(Object value) {
            DataPack dataPack = (DataPack) value;
            if (dataPack.isSuccess()) {
                return Observable.just(dataPack.getData());
            }
            return Observable.error(new ServerException(dataPack.getCode(), dataPack.getMessage()));
        }
    }

    public static Observable<ArrayList<GymRecord>> getGymRecords() {
        return gymRecordService.getGymRecords(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource n2;
                n2 = Network.n((ArrayList) obj);
                return n2;
            }
        });
    }

    public static Observable<Version> getLatestVersion(boolean z2) {
        return (z2 ? loginService.getLatestVersionForOffline() : loginService.getLatestVersion()).subscribeOn(Schedulers.newThread());
    }

    public static Observable<Version> getMinVersion() {
        return loginService.getMinVersion().subscribeOn(Schedulers.newThread());
    }

    public static Observable<ArrayList<Record>> getRecords(String str) {
        return recordService.getRecords(str).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource o2;
                o2 = Network.o((ArrayList) obj);
                return o2;
            }
        });
    }

    public static Observable<String> getReverseEncoding(Point point) {
        return recordService.reverseEncoding(String.format("%.6f,%.6f", Double.valueOf(point.getLongitude()), Double.valueOf(point.getLatitude())), BuildConfig.AMAP_WEB_KEY, 1000).subscribeOn(Schedulers.newThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((AMapReverseEncoding) obj).getStreetName();
            }
        });
    }

    public static Observable<Record> getSingleRecord(String str, int index) {
        return recordService.getSingleRecord(str, index).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource p2;
                p2 = Network.p((Record.Inner) obj);
                return p2;
            }
        });
    }

    public static Observable<UserStatus> getUserStatus() {
        return recordService.getStatus(Data.getUser().getId()).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction());
    }

    public static Observable<Weather> getWeather() {
        return weatherService.getWeather("fElIR").map(Weather::fromNmc).subscribeOn(Schedulers.newThread()).doOnNext(new Consumer() {
            @Override
            public final void accept(Object obj) {
                Network.weather = (Weather) obj;
            }
        });
    }

    public static void init(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public final Response intercept(Interceptor.Chain chain) throws java.io.IOException {
                Response s2;
                s2 = Network.s(chain);
                return s2;
            }
        });
        Retrofit build = new Retrofit.Builder().baseUrl(photoBaseUrl).addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava2CallAdapterFactory.create()).client(builder.build()).build();
        weatherService = (WeatherService) build.create(WeatherService.class);
        loginService = (LoginService) build.create(LoginService.class);
        recordService = (RecordService) build.create(RecordService.class);
        gymRecordService = (GymRecordService) build.create(GymRecordService.class);
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
        return loginService.login(str).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource t2;
                t2 = Network.t((User.Inner) obj);
                return t2;
            }
        });
    }

    public static /* synthetic */ ObservableSource n(ArrayList arrayList) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(new GymRecord((GymRecord.Inner) it.next()));
        }
        return Observable.just(arrayList2);
    }

    public static /* synthetic */ ObservableSource o(ArrayList arrayList) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(new Record((Record.Inner) it.next()));
        }
        return Observable.just(arrayList2);
    }

    public static /* synthetic */ ObservableSource p(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    public static /* synthetic */ ObservableSource t(User.Inner inner) {
        return Observable.just(new User(inner));
    }

    public static Observable<GymRecord> uploadGymRecordGetOut(int index, String str) {
        return gymRecordService.verifyGymRecord(Data.getUser().getId(), index, str).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
            @Override
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
                    return recordService.uploadRecordWithoutPhoto(Data.getUser().getId(), record.getDuration(), record.getDistance(), record.getDate().getTime(), jSONArray.toString(), jSONObject.toString(), record.getStep(), SecUtil.getAbstract(Data.getUser().getId(), String.valueOf(record.getDate().getTime()))).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
                        @Override
                        public final Object apply(Object obj) {
                            ObservableSource w2;
                            w2 = Network.w((Record.Inner) obj);
                            return w2;
                        }
                    });
                }
                return recordService.uploadRecord(Data.getUser().getId(), record.getDuration(), record.getDistance(), record.getDate().getTime(), jSONArray.toString(), jSONObject.toString(), record.getStep(), SecUtil.getAbstract(Data.getUser().getId(), String.valueOf(record.getDate().getTime())), RequestBody.create(MediaType.parse("image/jpeg"), file)).subscribeOn(Schedulers.newThread()).flatMap(new DataPackUnwrapFunction()).flatMap(new Function() {
                    @Override
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

    public static /* synthetic */ ObservableSource v(GymRecord.Inner inner) {
        return Observable.just(new GymRecord(inner));
    }

    public static /* synthetic */ ObservableSource w(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    public static /* synthetic */ ObservableSource x(Record.Inner inner) {
        return Observable.just(new Record(inner));
    }

    public static /* synthetic */ Response s(Interceptor.Chain chain) throws java.io.IOException {
        Request.Builder newBuilder = chain.request().newBuilder();
        if (Data.getUser() != null) {
            newBuilder = newBuilder.header("Authorization", Data.getUser().getToken());
        }
        return chain.proceed(newBuilder.header("Platform", "Android").header("Manufacturer", Build.MANUFACTURER).header("ClientVersion", BuildConfig.VERSION_NAME).build());
    }

}
