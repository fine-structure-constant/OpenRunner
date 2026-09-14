package cn.edu.pku.pkurunner.TaskList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.StackTracePrintingConsumer;
import cn.edu.pku.pkurunner.Model.Task;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.TaskCardAdapter;
import cn.edu.pku.pkurunner.TaskList.TaskListContract;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.xutils.common.util.LogUtil;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.BaseTaskViewHolder> {

    private ArrayList tasks;

    private Boolean dataValid = Boolean.FALSE;

    private TaskListContract.View taskListView;

    private TaskListContract.Presenter presenter;

    private TaskActionCallback taskCallback;

    interface TaskFilter {
        boolean test(Object obj);
    }

    interface TaskActionCallback {
    }

    static class TaskViewHolder extends BaseTaskViewHolder {

        Button redTeamButton;

        Button blueTeamButton;

        public static /* synthetic */ void P(final TaskListContract.Presenter presenter, View view) {
            Network.signUpActivity20180420(true).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    TaskCardAdapter.TaskViewHolder.O(presenter, (Boolean) obj);
                }
            }, new StackTracePrintingConsumer());
        }

        public static /* synthetic */ void R(final TaskListContract.Presenter presenter, View view) {
            Network.signUpActivity20180420(false).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    TaskCardAdapter.TaskViewHolder.Q(presenter, (Boolean) obj);
                }
            }, new StackTracePrintingConsumer());
        }

        @Override
        void H(Task task, final TaskListContract.Presenter presenter) {
            this.redTeamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    TaskCardAdapter.TaskViewHolder.P(presenter, view);
                }
            });
            this.blueTeamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    TaskCardAdapter.TaskViewHolder.R(presenter, view);
                }
            });
        }

        TaskViewHolder(View view) {
            super(view);
            this.redTeamButton = (Button) view.findViewById(R.id.v_a_20180420_btn_red);
            this.blueTeamButton = (Button) view.findViewById(R.id.v_a_20180420_btn_blue);
        }

        public static /* synthetic */ void O(TaskListContract.Presenter presenter, Boolean bool) {
            if (bool.booleanValue()) {
                presenter.syncData();
                LogUtil.d("Sign up red.");
            } else {
                LogUtil.d("Cannot sign up.");
            }
        }

        public static /* synthetic */ void Q(TaskListContract.Presenter presenter, Boolean bool) {
            if (bool.booleanValue()) {
                presenter.syncData();
                LogUtil.d("Sign up blue.");
            } else {
                LogUtil.d("Cannot sign up.");
            }
        }
    }

    static class BaseTaskViewHolder extends RecyclerView.ViewHolder {

        TextView taskNameText;

        TextView taskDescriptionText;

        View.OnClickListener cardClickListener;

        BadgeView rewardBadgeView;

        ConstraintLayout cardContainer;

        void H(Task task, TaskListContract.Presenter presenter) {
        }

        void J(View.OnClickListener onClickListener) {
            this.cardClickListener = onClickListener;
        }

        public /* synthetic */ void I(View view) {
            this.cardClickListener.onClick(view);
        }

        BaseTaskViewHolder(View view) {
            super(view);
            this.taskNameText = (TextView) view.findViewById(R.id.v_task_card_txt_name);
            this.taskDescriptionText = (TextView) view.findViewById(R.id.v_task_card_txt_description);
            this.rewardBadgeView = (BadgeView) view.findViewById(R.id.v_task_card_img_reward);
            this.cardContainer = (ConstraintLayout) view.findViewById(R.id.v_task_card_constraintlayout);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view2) {
                    TaskCardAdapter.BaseTaskViewHolder.this.I(view2);
                }
            });
        }
    }

    private int i(int index) {
        return index != 20180420 ? R.layout.view_task_card : R.layout.view_task_card_20180420;
    }

    public void notifyDataInvalid() {
        this.dataValid = Boolean.FALSE;
    }

    public void setPresenter(TaskListContract.View view, TaskListContract.Presenter presenter, TaskActionCallback callback) {
        this.taskListView = view;
        this.presenter = presenter;
        this.taskCallback = callback;
    }

    private ArrayList d(ArrayList arrayList, TaskFilter filter) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (filter.test(next)) {
                arrayList2.add(next);
            }
        }
        return arrayList2;
    }

    private ArrayList e() {
        if (!this.dataValid.booleanValue()) {
            ArrayList value = d(Data.getTasks(), new TaskFilter() {
                @Override
                public final boolean test(Object obj) {
                    boolean value2;
                    value2 = TaskCardAdapter.f((Task) obj);
                    return value2;
                }
            });
            this.tasks = value;
            Collections.sort(value, new Comparator() {
                @Override
                public final int compare(Object obj, Object obj2) {
                    int g2;
                    g2 = TaskCardAdapter.g((Task) obj, (Task) obj2);
                    return g2;
                }
            });
            this.dataValid = Boolean.TRUE;
        }
        return this.tasks;
    }

    public /* synthetic */ void h(Task task, View view) {
        this.taskListView.showTaskDetailDialog("Detail", String.format("Id: %d\n%s\n%s\n%s", Integer.valueOf(task.getId()), task.getName(), task.getDescription(), task.getRequirement()));
    }

    @Override
    public void onBindViewHolder(BaseTaskViewHolder holder, int index) {
        final Task task = (Task) e().get(index);
        holder.taskNameText.setText(task.getId() + task.getName());
        holder.taskDescriptionText.setText(task.getDescription());
        holder.J(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                TaskCardAdapter.this.h(task, view);
            }
        });
        holder.rewardBadgeView.setBadgeSeries("daily");
        int status = task.getStatus();
        if (status == 1) {
            holder.rewardBadgeView.setStatus(false);
        } else if (status == 2) {
            holder.rewardBadgeView.setStatus(true);
        }
        holder.H(task, this.presenter);
    }

    @Override
    public BaseTaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int index) {
        return j(LayoutInflater.from(viewGroup.getContext()).inflate(i(index), viewGroup, false), index);
    }

    public static /* synthetic */ boolean f(Task task) {
        if (task.getStatus() != 0) {
            return true;
        }
        return false;
    }

    public static /* synthetic */ int g(Task task, Task task2) {
        boolean z2;
        boolean z3 = false;
        if (task.getActivityId() != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        if (task2.getActivityId() != 0) {
            z3 = true;
        }
        if (z2 == z3) {
            return task.getId() - task2.getId();
        }
        if (!z2) {
            return 1;
        }
        return -1;
    }

    private BaseTaskViewHolder j(View view, int index) {
        if (index != 20180420) {
            return new BaseTaskViewHolder(view);
        }
        return new TaskViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return e().size();
    }

    @Override
    public int getItemViewType(int index) {
        return ((Task) e().get(index)).getActivityId();
    }
}
