package cn.edu.pku.pkurunner.TaskList;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.annotation.StringRes;
import cn.edu.pku.pkurunner.Contract.BasePresenter;
import cn.edu.pku.pkurunner.Contract.BaseView;

public interface TaskListContract {

    public interface Presenter extends BasePresenter {
        void refreshList();

        void syncData();
    }

    public interface View extends BaseView<Presenter> {
        void cancelRefresh();

        void dismissWaitingDialog();

        Context getContext();

        LayoutInflater getLayoutInflater();

        TaskCardAdapter getTaskCardAdapter();

        void makeSnackBar(@StringRes int index, int index2, Object... objArr);

        void makeToast(@StringRes int index, int index2, Object... objArr);

        void showCongratulationsDialog(@StringRes int index, @StringRes int index2);

        void showTaskDetailDialog(String str, String str2);

        void showWaitingDialog(int index);

        void toggleNotice(Boolean bool);

        void updateWaitingDialog(int index);
    }
}
