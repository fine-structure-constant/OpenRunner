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

    private View rootView;

    private TaskCardAdapter taskCardAdapter;

    private TextView emptyHintText;

    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean refreshing;

    private ProgressDialog progressDialog;

    private TaskListContract.Presenter presenter;

    class TaskListScrollListener extends RecyclerView.OnScrollListener {
        TaskListScrollListener() {
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int index) {
            super.onScrollStateChanged(recyclerView, index);
            if (index == 0) {
                TaskListFragment.this.refreshing = false;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int index, int index2) {
            super.onScrolled(recyclerView, index, index2);
            TaskListFragment.this.refreshing = true;
        }
    }

    public /* synthetic */ boolean n(SwipeRefreshLayout swipeRefreshLayout, View view) {
        return this.refreshing;
    }

    public static /* synthetic */ void p(DialogInterface dialogInterface, int index) {
    }

    @Override
    public TaskCardAdapter getTaskCardAdapter() {
        return this.taskCardAdapter;
    }

    @Override
    public void setPresenter(@NonNull TaskListContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public /* synthetic */ void m() {
        this.presenter.syncData();
    }

    public /* synthetic */ void o() {
        this.swipeRefreshLayout.setRefreshing(true);
        this.presenter.syncData();
    }

    @Override
    public void cancelRefresh() {
        this.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void dismissWaitingDialog() {
        ProgressDialog progressDialog = this.progressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.progressDialog = null;
        }
    }

    @Override
    public void makeSnackBar(@StringRes int index, int index2, Object... objArr) {
        Snackbar.make(this.rootView, getString(index, objArr), index2).show();
    }

    @Override
    public void showCongratulationsDialog(@StringRes int index, @StringRes int index2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View inflate = getActivity().getLayoutInflater().inflate(R.layout.fragment_task_complete_dialog, (ViewGroup) null);
        ((TextView) inflate.findViewById(R.id.f_task_complete_txt_title)).setText(index);
        ((TextView) inflate.findViewById(R.id.f_task_complete_txt_message)).setText(index2);
        builder.setView(inflate).setPositiveButton(getString(R.string.f_task_complete_dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                TaskListFragment.p(dialogInterface, index3);
            }
        }).create().show();
    }

    @Override
    public void showTaskDetailDialog(String str, String str2) {
        new AlertDialog.Builder(getContext()).setTitle(str).setMessage(str2).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    @Override
    public void showWaitingDialog(int index) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        this.progressDialog = progressDialog;
        progressDialog.setProgressStyle(1);
        this.progressDialog.setMessage("正在同步数据");
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setMax(index);
        this.progressDialog.show();
    }

    @Override
    public void toggleNotice(Boolean bool) {
        this.emptyHintText.setVisibility(bool.booleanValue() ? 0 : 8);
    }

    @Override
    public void updateWaitingDialog(int index) {
        this.progressDialog.setProgress(index);
    }

    @Override
    public void makeToast(@StringRes int index, int index2, Object... objArr) {
        Toast.makeText(getContext(), getString(index, objArr), index2).show();
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_task, menu);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_tasklist, viewGroup, false);
        this.rootView = inflate;
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) inflate.findViewById(R.id.f_tasklist_swipeRefreshLayout);
        this.swipeRefreshLayout = swipeRefreshLayout;
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_500, R.color.green_500, R.color.blue_500);
        this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public final void onRefresh() {
                TaskListFragment.this.m();
            }
        });
        this.swipeRefreshLayout.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public final boolean canChildScrollUp(SwipeRefreshLayout swipeRefreshLayout2, View view) {
                boolean n2;
                n2 = TaskListFragment.this.n(swipeRefreshLayout2, view);
                return n2;
            }
        });
        RecyclerView recyclerView = (RecyclerView) this.rootView.findViewById(R.id.f_tasklist_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new TaskListScrollListener());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), 1);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        TaskCardAdapter taskCardAdapter = new TaskCardAdapter();
        this.taskCardAdapter = taskCardAdapter;
        taskCardAdapter.setPresenter(this, this.presenter, new TaskCardAdapter.TaskActionCallback() {
        });
        recyclerView.setAdapter(this.taskCardAdapter);
        this.emptyHintText = (TextView) this.rootView.findViewById(R.id.f_tasklist_txt_notice);
        this.swipeRefreshLayout.post(new Runnable() {
            @Override
            public final void run() {
                TaskListFragment.this.o();
            }
        });
        return this.rootView;
    }

    @Override
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (!z2) {
            this.presenter.start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        menuItem.getItemId();
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.presenter.start();
    }
}
