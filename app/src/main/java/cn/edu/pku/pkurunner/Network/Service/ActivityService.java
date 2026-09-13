package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Network.DataPack;
import io.reactivex.Observable;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ActivityService {
    @POST("activity/20180420/user/{userId}/team/purple")
    Observable<DataPack> clear20180420(@Path("userId") String str);

    @POST("activity/{activityId}/user/{userId}/team/{color}")
    Observable<DataPack> signUp20180420(@Path("activityId") int i2, @Path("userId") String str, @Path("color") String str2);
}
