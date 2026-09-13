package cn.edu.pku.pkurunner.TaskList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.TaskCardAdapter;
import cn.edu.pku.pkurunner.TaskList.TaskListContract;
import com.google.android.material.snackbar.Snackbar;

public class TaskListFragment extends Fragment implements TaskListContract.View {

    /* renamed from: d, reason: collision with root package name */
    private View f7108d;

    /* renamed from: e, reason: collision with root package name */
    private TaskCardAdapter f7109e;

    /* renamed from: f, reason: collision with root package name */
    private TextView f7110f;

    /* renamed from: g, reason: collision with root package name */
    private SwipeRefreshLayout f7111g;

    /* renamed from: h, reason: collision with root package name */
    private boolean f7112h;

    /* renamed from: i, reason: collision with root package name */
    private ProgressDialog f7113i;

    /* renamed from: j, reason: collision with root package name */
    private TaskListContract.Presenter f7114j;

    class a extends RecyclerView.OnScrollListener {
        a() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrollStateChanged(RecyclerView recyclerView, int i2) {
            super.onScrollStateChanged(recyclerView, i2);
            if (i2 == 0) {
                TaskListFragment.this.f7112h = false;
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrolled(RecyclerView recyclerView, int i2, int i3) {
            super.onScrolled(recyclerView, i2, i3);
            TaskListFragment.this.f7112h = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean n(SwipeRefreshLayout swipeRefreshLayout, View view) {
        return this.f7112h;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void p(DialogInterface dialogInterface, int i2) {
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public TaskCardAdapter getTaskCardAdapter() {
        return this.f7109e;
    }

    @Override // cn.edu.pku.pkurunner.Contract.BaseView
    public void setPresenter(@NonNull TaskListContract.Presenter presenter) {
        this.f7114j = presenter;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void m() {
        this.f7114j.syncData();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void o() {
        this.f7111g.setRefreshing(true);
        this.f7114j.syncData();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void cancelRefresh() {
        this.f7111g.setRefreshing(false);
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void dismissWaitingDialog() {
        ProgressDialog progressDialog = this.f7113i;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.f7113i = null;
        }
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void makeSnackBar(@StringRes int i2, int i3, Object... objArr) {
        Snackbar.make(this.f7108d, getString(i2, objArr), i3).show();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void showCongratulationsDialog(@StringRes int i2, @StringRes int i3) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View inflate = getActivity().getLayoutInflater().inflate(R.layout.fragment_task_complete_dialog, (ViewGroup) null);
        ((TextView) inflate.findViewById(R.id.f_task_complete_txt_title)).setText(i2);
        ((TextView) inflate.findViewById(R.id.f_task_complete_txt_message)).setText(i3);
        builder.setView(inflate).setPositiveButton(getString(R.string.f_task_complete_dialog_ok), new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.i
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                TaskListFragment.p(dialogInterface, i4);
            }
        }).create().show();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void showTaskDetailDialog(String str, String str2) {
        new AlertDialog.Builder(getContext()).setTitle(str).setMessage(str2).setPositiveButton("OK", new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.TaskList.j
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void showWaitingDialog(int i2) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        this.f7113i = progressDialog;
        progressDialog.setProgressStyle(1);
        this.f7113i.setMessage("正在同步数据");
        this.f7113i.setIndeterminate(false);
        this.f7113i.setCancelable(false);
        this.f7113i.setMax(i2);
        this.f7113i.show();
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void toggleNotice(Boolean bool) {
        this.f7110f.setVisibility(bool.booleanValue() ? 0 : 8);
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void updateWaitingDialog(int i2) {
        this.f7113i.setProgress(i2);
    }

    @Override // cn.edu.pku.pkurunner.TaskList.TaskListContract.View
    public void makeToast(@StringRes int i2, int i3, Object... objArr) {
        Toast.makeText(getContext(), getString(i2, objArr), i3).show();
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_task, menu);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_tasklist, viewGroup, false);
        this.f7108d = inflate;
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) inflate.findViewById(R.id.f_tasklist_swipeRefreshLayout);
        this.f7111g = swipeRefreshLayout;
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_500, R.color.green_500, R.color.blue_500);
        this.f7111g.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() { // from class: cn.edu.pku.pkurunner.TaskList.k
            @Override // androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
            public final void onRefresh() {
                TaskListFragment.this.m();
            }
        });
        this.f7111g.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() { // from class: cn.edu.pku.pkurunner.TaskList.l
            @Override // androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnChildScrollUpCallback
            public final boolean canChildScrollUp(SwipeRefreshLayout swipeRefreshLayout2, View view) {
                boolean n2;
                n2 = TaskListFragment.this.n(swipeRefreshLayout2, view);
                return n2;
            }
        });
        RecyclerView recyclerView = (RecyclerView) this.f7108d.findViewById(R.id.f_tasklist_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new a());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), 1);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        TaskCardAdapter taskCardAdapter = new TaskCardAdapter();
        this.f7109e = taskCardAdapter;
        taskCardAdapter.setPresenter(this, this.f7114j, new TaskCardAdapter.b() { // from class: cn.edu.pku.pkurunner.TaskList.m
        });
        recyclerView.setAdapter(this.f7109e);
        this.f7110f = (TextView) this.f7108d.findViewById(R.id.f_tasklist_txt_notice);
        this.f7111g.post(new Runnable() { // from class: cn.edu.pku.pkurunner.TaskList.n
            @Override // java.lang.Runnable
            public final void run() {
                TaskListFragment.this.o();
            }
        });
        return this.f7108d;
    }

    @Override // androidx.fragment.app.Fragment
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (!z2) {
            this.f7114j.start();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        menuItem.getItemId();
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        this.f7114j.start();
    }
}
