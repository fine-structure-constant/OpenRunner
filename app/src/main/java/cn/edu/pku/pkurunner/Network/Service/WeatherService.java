package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.NmcWeatherResponse;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {
    @GET("https://nmc.cn/rest/weather")
    Observable<NmcWeatherResponse> getWeather(@Query("stationid") String stationId);
}
