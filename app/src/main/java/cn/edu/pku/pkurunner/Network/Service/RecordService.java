package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Network.DataPack;
import cn.edu.pku.pkurunner.Network.Model.AMapReverseEncoding;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import io.reactivex.Observable;
import java.util.ArrayList;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecordService {
    @GET("record/{userId}")
    Observable<DataPack<ArrayList<Record.Inner>>> getRecords(@Path("userId") String str);

    @GET("record/{userId}/{recordId}")
    Observable<DataPack<Record.Inner>> getSingleRecord(@Path("userId") String str, @Path("recordId") int i2);

    @GET("record/status/{userId}")
    Observable<DataPack<UserStatus>> getStatus(@Path("userId") String str);

    @GET("https://restapi.amap.com/v3/geocode/regeo?output=json&extensions=base")
    Observable<AMapReverseEncoding> reverseEncoding(@Query("location") String str, @Query("key") String str2, @Query("radius") int i2);

    @POST("record/{userId}")
    @Multipart
    Observable<DataPack<Record.Inner>> uploadRecord(@Path("userId") String str, @Part("duration") int i2, @Part("distance") int i3, @Part("date") long j2, @Part("detail") String str2, @Part("misc") String str3, @Part("step") int i4, @Part("abstract") String str4, @Part("photo\"; filename=\"image.jpg\" ") RequestBody requestBody);

    @POST("record/{userId}")
    @Multipart
    Observable<DataPack<Record.Inner>> uploadRecordWithoutPhoto(@Path("userId") String str, @Part("duration") int i2, @Part("distance") int i3, @Part("date") long j2, @Part("detail") String str2, @Part("misc") String str3, @Part("step") int i4, @Part("abstract") String str4);
}
