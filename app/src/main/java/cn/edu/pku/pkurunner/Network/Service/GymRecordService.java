package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.GymRecord;
import cn.edu.pku.pkurunner.Network.DataPack;
import io.reactivex.Observable;
import java.util.ArrayList;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GymRecordService {
    @GET("record2/{userId}")
    Observable<DataPack<ArrayList<GymRecord.Inner>>> getGymRecords(@Path("userId") String str);

    @FormUrlEncoded
    @POST("record2/{userId}/{recordId}")
    Observable<DataPack<GymRecord.Inner>> verifyGymRecord(@Path("userId") String str, @Path("recordId") int index, @Field("token") String str2);
}
