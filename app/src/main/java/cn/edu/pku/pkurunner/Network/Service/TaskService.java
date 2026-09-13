package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.Task;
import cn.edu.pku.pkurunner.Network.DataPack;
import io.reactivex.Observable;
import java.util.Map;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TaskService {
    @GET("badge/user/{userId}")
    Observable<DataPack<Map<String, Task>>> getList(@Path("userId") String str);
}
