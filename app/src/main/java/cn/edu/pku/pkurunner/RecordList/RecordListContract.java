package cn.edu.pku.pkurunner.RecordList;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import androidx.annotation.StringRes;
import cn.edu.pku.pkurunner.Contract.BasePresenter;
import cn.edu.pku.pkurunner.Contract.BaseView;
import cn.edu.pku.pkurunner.MainActivity;
import io.reactivex.Observable;
import java.io.File;

public interface RecordListContract {

    public interface Presenter extends BasePresenter {
        void deleteRecord(int i2, int i3);

        void refreshList();

        void showRecordDetail(int i2);

        void start(boolean z2);

        void syncData();

        void uploadVerifyRecord(int i2, int i3);
    }

    public interface View extends BaseView<Presenter> {

        public enum PhotoStatus {
            RecentShot,
            UseLast,
            Ignored,
            Cancelled
        }

        Observable<Boolean> callSystemCamera(File file);

        void cancelRefresh();

        void dismissWaitDialog();

        Context getContext();

        File getExternalPhotoDir();

        MainActivity getMainActivity();

        RecordCardAdapter getRecordCardAdapter();

        Observable<Pair<String, String>> launchIaaaLogin();

        void makeSnackBar(@StringRes int i2, int i3, Object... objArr);

        void makeToast(@StringRes int i2, int i3, Object... objArr);

        void scrollRecyclerViewToTop();

        void setWaitingDialogMessage(@StringRes int i2);

        Observable<Boolean> showConfirmDialog(@StringRes int i2, @StringRes int i3);

        Observable<PhotoStatus> showPhotoDialog(@StringRes int i2, @StringRes int i3);

        void showRecordDetailSheet(Bundle bundle);

        void showWaitingDialog();

        void toggleLoadingNotice(boolean z2);

        void toggleNoDataNotice(boolean z2);
    }
}
