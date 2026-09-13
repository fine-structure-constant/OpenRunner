package cn.edu.pku.pkurunner.TaskList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Model.Task;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.TaskCardAdapter;
import cn.edu.pku.pkurunner.TaskList.TaskListContract;
import cn.edu.pku.pkurunner.i1;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.xutils.common.util.LogUtil;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.d> {

    /* renamed from: c, reason: collision with root package name */
    private ArrayList f7096c;

    /* renamed from: d, reason: collision with root package name */
    private Boolean f7097d = Boolean.FALSE;

    /* renamed from: e, reason: collision with root package name */
    private TaskListContract.View f7098e;

    /* renamed from: f, reason: collision with root package name */
    private TaskListContract.Presenter f7099f;

    /* renamed from: g, reason: collision with root package name */
    private b f7100g;

    interface a {
        boolean test(Object obj);
    }

    interface b {
    }

    static class c extends d {

        /* renamed from: x, reason: collision with root package name */
        Button f7101x;

        /* renamed from: y, reason: collision with root package name */
        Button f7102y;

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void P(final TaskListContract.Presenter presenter, View view) {
            Network.signUpActivity20180420(true).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.TaskList.g
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    TaskCardAdapter.c.O(presenter, (Boolean) obj);
                }
            }, new i1());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void R(final TaskListContract.Presenter presenter, View view) {
            Network.signUpActivity20180420(false).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.TaskList.f
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    TaskCardAdapter.c.Q(presenter, (Boolean) obj);
                }
            }, new i1());
        }

        @Override // cn.edu.pku.pkurunner.TaskList.TaskCardAdapter.d
        void H(Task task, final TaskListContract.Presenter presenter) {
            this.f7101x.setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.d
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    TaskCardAdapter.c.P(presenter, view);
                }
            });
            this.f7102y.setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.e
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    TaskCardAdapter.c.R(presenter, view);
                }
            });
        }

        c(View view) {
            super(view);
            this.f7101x = (Button) view.findViewById(R.id.v_a_20180420_btn_red);
            this.f7102y = (Button) view.findViewById(R.id.v_a_20180420_btn_blue);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void O(TaskListContract.Presenter presenter, Boolean bool) {
            if (bool.booleanValue()) {
                presenter.syncData();
                LogUtil.d("Sign up red.");
            } else {
                LogUtil.d("Cannot sign up.");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void Q(TaskListContract.Presenter presenter, Boolean bool) {
            if (bool.booleanValue()) {
                presenter.syncData();
                LogUtil.d("Sign up blue.");
            } else {
                LogUtil.d("Cannot sign up.");
            }
        }
    }

    static class d extends RecyclerView.ViewHolder {

        /* renamed from: s, reason: collision with root package name */
        TextView f7103s;

        /* renamed from: t, reason: collision with root package name */
        TextView f7104t;

        /* renamed from: u, reason: collision with root package name */
        View.OnClickListener f7105u;

        /* renamed from: v, reason: collision with root package name */
        BadgeView f7106v;

        /* renamed from: w, reason: collision with root package name */
        ConstraintLayout f7107w;

        void H(Task task, TaskListContract.Presenter presenter) {
        }

        void J(View.OnClickListener onClickListener) {
            this.f7105u = onClickListener;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void I(View view) {
            this.f7105u.onClick(view);
        }

        d(View view) {
            super(view);
            this.f7103s = (TextView) view.findViewById(R.id.v_task_card_txt_name);
            this.f7104t = (TextView) view.findViewById(R.id.v_task_card_txt_description);
            this.f7106v = (BadgeView) view.findViewById(R.id.v_task_card_img_reward);
            this.f7107w = (ConstraintLayout) view.findViewById(R.id.v_task_card_constraintlayout);
            view.setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.h
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    TaskCardAdapter.d.this.I(view2);
                }
            });
        }
    }

    private int i(int i2) {
        return i2 != 20180420 ? R.layout.view_task_card : R.layout.view_task_card_20180420;
    }

    public void notifyDataInvalid() {
        this.f7097d = Boolean.FALSE;
    }

    public void setPresenter(TaskListContract.View view, TaskListContract.Presenter presenter, b bVar) {
        this.f7098e = view;
        this.f7099f = presenter;
        this.f7100g = bVar;
    }

    private ArrayList d(ArrayList arrayList, a aVar) {
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (aVar.test(next)) {
                arrayList2.add(next);
            }
        }
        return arrayList2;
    }

    private ArrayList e() {
        if (!this.f7097d.booleanValue()) {
            ArrayList d2 = d(Data.getTasks(), new a() { // from class: cn.edu.pku.pkurunner.TaskList.b
                @Override // cn.edu.pku.pkurunner.TaskList.TaskCardAdapter.a
                public final boolean test(Object obj) {
                    boolean f2;
                    f2 = TaskCardAdapter.f((Task) obj);
                    return f2;
                }
            });
            this.f7096c = d2;
            Collections.sort(d2, new Comparator() { // from class: cn.edu.pku.pkurunner.TaskList.c
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    int g2;
                    g2 = TaskCardAdapter.g((Task) obj, (Task) obj2);
                    return g2;
                }
            });
            this.f7097d = Boolean.TRUE;
        }
        return this.f7096c;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void h(Task task, View view) {
        this.f7098e.showTaskDetailDialog("Detail", String.format("Id: %d\n%s\n%s\n%s", Integer.valueOf(task.getId()), task.getName(), task.getDescription(), task.getRequirement()));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(d dVar, int i2) {
        final Task task = (Task) e().get(i2);
        dVar.f7103s.setText(task.getId() + task.getName());
        dVar.f7104t.setText(task.getDescription());
        dVar.J(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.a
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TaskCardAdapter.this.h(task, view);
            }
        });
        dVar.f7106v.setBadgeSeries("daily");
        int status = task.getStatus();
        if (status == 1) {
            dVar.f7106v.setStatus(false);
        } else if (status == 2) {
            dVar.f7106v.setStatus(true);
        }
        dVar.H(task, this.f7099f);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public d onCreateViewHolder(ViewGroup viewGroup, int i2) {
        return j(LayoutInflater.from(viewGroup.getContext()).inflate(i(i2), viewGroup, false), i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ boolean f(Task task) {
        if (task.getStatus() != 0) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
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

    private d j(View view, int i2) {
        if (i2 != 20180420) {
            return new d(view);
        }
        return new c(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return e().size();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int i2) {
        return ((Task) e().get(i2)).getActivityId();
    }
}
