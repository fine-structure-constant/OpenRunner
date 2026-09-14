package cn.edu.pku.pkurunner.Map;

import android.content.Context;
import android.view.Menu;
import androidx.annotation.StringRes;
import cn.edu.pku.pkurunner.Contract.BasePresenter;
import cn.edu.pku.pkurunner.Contract.BaseView;
import cn.edu.pku.pkurunner.MainActivity;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.LocationSource;
import io.reactivex.Observable;

public interface MapContract {

    public interface Presenter extends BasePresenter {
        boolean isRunning();

        boolean isRunningPaused();

        boolean onFabPauseClick(boolean z2);

        boolean onFabRunClick(boolean z2);

        void onStep();

        void pauseAutoLocating();

        void pauseGPSAssistant();

        void startAutoLocating();

        void startGPSAssistant();

        void stopAndSwitchToIdle();

        void syncData();

        void syncOptionsMenu(Menu menu);

        void updateUnitPreference();
    }

    public interface View extends BaseView<Presenter> {
        boolean checkKeepAlive();

        void dismissNotification();

        void dismissWaitingDialog();

        AMap getAMap();

        MainActivity getActivityFromContract();

        Context getFragmentContext();

        LocationSource.OnLocationChangedListener getLocationListener();

        void indicatorShowUpAnimation();

        void makeSnackBar(@StringRes int index, int index2, Object... objArr);

        void makeToast(@StringRes int index, int index2, Object... objArr);

        void makeWaitingDialog(@StringRes int index, Object... objArr);

        void notifyGPSInfo();

        void openDevelopSettings();

        void openGPSSettings();

        Observable<Boolean> registerMapCenterHelper();

        void releaseWakeLock();

        void requireWakeLock();

        void setAssistantText(@StringRes int index, double value, Object... objArr);

        void setLocatingPointEnabled(boolean z2);

        void showNotification();

        void switchToPaused();

        void switchToReset();

        void switchToRunning();

        void toggleGPSAssistantIndication(boolean z2);

        void toggleRunningIndication(boolean z2);

        void unregisterMapCenterHelper();

        void updateTextSci(SpeedHelper.SPEED_UNIT speed_unit);

        void updateTextView(double value, double value2, double value3, boolean z2);

        void updateWaitingDialog(@StringRes int index, Object... objArr);
    }
}
