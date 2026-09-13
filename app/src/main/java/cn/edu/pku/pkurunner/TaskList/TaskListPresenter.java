package cn.edu.pku.pkurunner.TaskList;

import androidx.annotation.NonNull;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.TaskListContract;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.ArrayList;

public class TaskListPresenter implements TaskListContract.Presenter {

    /* renamed from: a, reason: collision with root package name */
    private TaskListContract.View f7116a;

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void c(ArrayList arrayList) {
        if (this.f7116a.getTaskCardAdapter() != null) {
            refreshList();
        }
        this.f7116a.makeToast(R.string.p_task_data_success, 0, new Object[0]);
        this.f7116a.cancelRefresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void d(Throwable th) {
        this.f7116a.makeToast(R.string.p_task_data_failure, 0, new Object[0]);
        this.f7116a.cancelRefresh();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.Presenter
    public void refreshList() {
        this.f7116a.getTaskCardAdapter().notifyDataInvalid();
        this.f7116a.getTaskCardAdapter().notifyDataSetChanged();
        TaskListContract.View view = this.f7116a;
        view.toggleNotice(Boolean.valueOf(view.getTaskCardAdapter().getItemCount() == 0));
    }

    public TaskListPresenter(@NonNull TaskListContract.View view) {
        this.f7116a = view;
        view.setPresenter(this);
    }

    @Override // cn.edu.pku.pkurunner.Contract.BasePresenter
    public void start() {
        refreshList();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.Presenter
    public void syncData() {
        if (Data.getUser().isOffline().booleanValue()) {
            this.f7116a.makeToast(R.string.p_task_offline_unusable, 0, new Object[0]);
            this.f7116a.cancelRefresh();
        } else {
            Data.getTasksFromServer().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.TaskList.o
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    TaskListPresenter.this.c((ArrayList) obj);
                }
            }, new Consumer() { // from class: cn.edu.pku.pkurunner.TaskList.p
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    TaskListPresenter.this.d((Throwable) obj);
                }
            });
        }
    }
}
