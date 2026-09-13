package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Network.DataPack;
import cn.edu.pku.pkurunner.Network.Model.Version;
import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface LoginService {
    @GET("public/client/android/curr_version")
    Observable<Version> getLatestVersion();

    @GET("https://raw.githubusercontent.com/pku-runner/pku-runner.github.io/android/public/client/android/curr_version")
    Observable<Version> getLatestVersionForOffline();

    @GET("public/client/android/min_version")
    Observable<Version> getMinVersion();

    @FormUrlEncoded
    @POST("user")
    Observable<DataPack<User.Inner>> login(@Field("access_token") String str);
}
