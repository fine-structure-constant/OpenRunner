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
        void deleteRecord(int index, int index2);

        void refreshList();

        void showRecordDetail(int index);

        void start(boolean z2);

        void syncData();

        void uploadVerifyRecord(int index, int index2);
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

        void makeSnackBar(@StringRes int index, int index2, Object... objArr);

        void makeToast(@StringRes int index, int index2, Object... objArr);

        void scrollRecyclerViewToTop();

        void setWaitingDialogMessage(@StringRes int index);

        Observable<Boolean> showConfirmDialog(@StringRes int index, @StringRes int index2);

        Observable<PhotoStatus> showPhotoDialog(@StringRes int index, @StringRes int index2);

        void showRecordDetailSheet(Bundle bundle);

        void showWaitingDialog();

        void toggleLoadingNotice(boolean z2);

        void toggleNoDataNotice(boolean z2);
    }
}
