package cn.edu.pku.pkurunner.Network.Service;

import cn.edu.pku.pkurunner.Model.Weather;
import io.reactivex.Observable;
import retrofit2.http.GET;

public interface WeatherService {
    @GET("weather/all")
    Observable<Weather> getWeather();
}
