package cn.edu.pku.pkurunner.Broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.R;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class WeatherUpdateReceiver extends BroadcastReceiver {

    /* renamed from: a, reason: collision with root package name */
    private Observer f6850a;

    public WeatherUpdateReceiver(Observer<Weather> observer) {
        this.f6850a = observer;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, context.getString(R.string.i_refreshing_weather), 0).show();
        Network.getWeather().observeOn(AndroidSchedulers.mainThread()).subscribe(this.f6850a);
    }
}
