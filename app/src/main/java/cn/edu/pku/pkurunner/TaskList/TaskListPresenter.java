package cn.edu.pku.pkurunner.TaskList;

import androidx.annotation.NonNull;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.TaskListContract;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.ArrayList;

public class TaskListPresenter implements TaskListContract.Presenter {

    private TaskListContract.View view;

    public /* synthetic */ void c(ArrayList arrayList) {
        if (this.view.getTaskCardAdapter() != null) {
            refreshList();
        }
        this.view.makeToast(R.string.p_task_data_success, 0, new Object[0]);
        this.view.cancelRefresh();
    }

    public /* synthetic */ void d(Throwable th) {
        this.view.makeToast(R.string.p_task_data_failure, 0, new Object[0]);
        this.view.cancelRefresh();
    }

    @Override
    public void refreshList() {
        this.view.getTaskCardAdapter().notifyDataInvalid();
        this.view.getTaskCardAdapter().notifyDataSetChanged();
        TaskListContract.View view = this.view;
        view.toggleNotice(Boolean.valueOf(view.getTaskCardAdapter().getItemCount() == 0));
    }

    public TaskListPresenter(@NonNull TaskListContract.View view) {
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {
        refreshList();
    }

    @Override
    public void syncData() {
        if (Data.getUser().isOffline().booleanValue()) {
            this.view.makeToast(R.string.p_task_offline_unusable, 0, new Object[0]);
            this.view.cancelRefresh();
        } else {
            Data.getTasksFromServer().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    TaskListPresenter.this.c((ArrayList) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    TaskListPresenter.this.d((Throwable) obj);
                }
            });
        }
    }
}
