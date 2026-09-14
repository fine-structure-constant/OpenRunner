package cn.edu.pku.pkurunner.View;

import android.widget.ImageView;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.ViewTarget;
import java.util.Date;

public interface ProgressableView {
    void reset();

    void setActiveMode(boolean z2);

    void setCollapseMode(boolean z2);

    void setDayMode(boolean z2);

    void setMainBonusProgress(float value);

    void setMainProgress(float value);

    void setMainText(String str);

    void setReferenceTime(Date date);

    void setSecondaryProgress(float value);

    void setSecondaryText(String str);

    void setSleepingIndicatorText(String str);

    <T> ViewTarget<ImageView, T> setWeatherDrawable(RequestBuilder<T> glideRequest);
}
