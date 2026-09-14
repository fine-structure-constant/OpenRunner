package cn.edu.pku.pkurunner.RecordList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import cn.edu.pku.pkurunner.GuidePage.IntroActivity;
import cn.edu.pku.pkurunner.MainActivity;
import cn.edu.pku.pkurunner.Photo.PhotoFile;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback;
import cn.edu.pku.pkurunner.RecordList.RecordCardAdapter;
import cn.edu.pku.pkurunner.RecordList.RecordListContract;
import cn.edu.pku.pkurunner.RecordList.RecordListFragment;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class RecordListFragment extends Fragment implements RecordListContract.View {

    private View rootView;

    private SwipeRefreshLayout swipeRefreshLayout;

    private RecordCardAdapter recordCardAdapter;

    private TextView noticeText;

    private TextView emptyHintText;

    private ImageView emptyImageView;

    private ProgressDialog progressDialog;

    private boolean appBarExpanded;

    private FloatingActionButton startRunningFab;

    private RecordListContract.Presenter presenter;

    private ObservableEmitter photoResultEmitter;

    private ObservableEmitter loginResultEmitter;

    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean bool) {
            if (RecordListFragment.this.photoResultEmitter != null) {
                RecordListFragment.this.photoResultEmitter.onNext(bool);
                RecordListFragment.this.photoResultEmitter = null;
            }
        }
    });

    private final ActivityResultLauncher<Intent> iaaaLoginLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult activityResult) {
            if (RecordListFragment.this.loginResultEmitter == null) {
                return;
            }
            if (activityResult.getResultCode() == -1 && activityResult.getData() != null) {
                Bundle extras = activityResult.getData().getExtras();
                if (IaaaWrapper.RESULT_CANCEL.equals(extras.getString(IaaaWrapper.EXTRA_iAAA_RESULT))) {
                    RecordListFragment.this.loginResultEmitter.onError(new Throwable(getString(R.string.f_record_error_login_cancelled)));
                } else {
                    RecordListFragment.this.loginResultEmitter.onNext(new Pair(extras.getString(IaaaWrapper.EXTRA_iAAA_UID), extras.getString(IaaaWrapper.EXTRA_iAAA_TOKEN)));
                }
            } else {
                RecordListFragment.this.loginResultEmitter.onError(new Throwable(getString(R.string.f_record_error_login_fail)));
            }
            RecordListFragment.this.loginResultEmitter = null;
        }
    });

    static class AppBarStateChangeWrapper implements AppBarLayout.OnOffsetChangedListener {

        private State currentState = State.IDLE;

        private OnAppBarStateChanged stateChangeListener;

        public enum State {
            EXPANDED,
            COLLAPSED,
            IDLE
        }

        interface OnAppBarStateChanged {
            void onStateChanged(AppBarLayout appBarLayout, State state);
        }

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int index) {
            if (index == 0) {
                State state = this.currentState;
                State state2 = State.EXPANDED;
                if (state != state2) {
                    this.stateChangeListener.onStateChanged(appBarLayout, state2);
                }
                this.currentState = state2;
                return;
            }
            if (Math.abs(index) >= appBarLayout.getTotalScrollRange() * 0.6d) {
                State state3 = this.currentState;
                State state4 = State.COLLAPSED;
                if (state3 != state4) {
                    this.stateChangeListener.onStateChanged(appBarLayout, state4);
                }
                this.currentState = state4;
                return;
            }
            State state5 = this.currentState;
            State state6 = State.IDLE;
            if (state5 != state6) {
                this.stateChangeListener.onStateChanged(appBarLayout, state6);
            }
            this.currentState = state6;
        }

        public AppBarStateChangeWrapper(OnAppBarStateChanged stateChangeListener) {
            this.stateChangeListener = stateChangeListener;
        }
    }

    class ScrollStateResetListener extends RecyclerView.OnScrollListener {
        ScrollStateResetListener() {
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int index) {
            super.onScrollStateChanged(recyclerView, index);
            if (index == 0) {
                RecordListFragment.this.appBarExpanded = false;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int index, int index2) {
            super.onScrolled(recyclerView, index, index2);
            RecordListFragment.this.appBarExpanded = true;
        }
    }

    public /* synthetic */ boolean C(SwipeRefreshLayout swipeRefreshLayout, View view) {
        return this.appBarExpanded;
    }

    private Rect y(View view) {
        int[] iArr = new int[2];
        view.getLocationInWindow(iArr);
        int index = iArr[0];
        int width = view.getWidth() + index;
        int index2 = iArr[1];
        return new Rect(index, index2, width, view.getHeight() + index2);
    }

    @Override
    public RecordCardAdapter getRecordCardAdapter() {
        return this.recordCardAdapter;
    }

    @Override
    public void setPresenter(@NonNull RecordListContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public /* synthetic */ void A(ObservableEmitter observableEmitter) {
        this.loginResultEmitter = observableEmitter;
        this.iaaaLoginLauncher.launch(IaaaWrapper.createIaaaIntent(getActivity()));
    }

    public /* synthetic */ void B() {
        this.presenter.syncData();
    }

    public /* synthetic */ void D(boolean z2) {
        this.swipeRefreshLayout.setEnabled(z2);
    }

    public /* synthetic */ void F(AppBarLayout appBarLayout, AppBarStateChangeWrapper.State state) {
        if (state == AppBarStateChangeWrapper.State.COLLAPSED) {
            this.startRunningFab.setClickable(false);
            this.startRunningFab.hide();
        } else {
            this.startRunningFab.setClickable(true);
            this.startRunningFab.show();
        }
    }

    public static /* synthetic */ void H(ObservableEmitter observableEmitter, DialogInterface dialogInterface) {
        observableEmitter.onNext(Boolean.FALSE);
    }

    public /* synthetic */ void I(int index, int index2, final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(getContext()).setTitle(index).setMessage(index2).setPositiveButton(R.string.f_record_dialog_positive, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                RecordListFragment.J(observableEmitter, dialogInterface, index3);
            }
        }).setNegativeButton(R.string.f_record_dialog_negative, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                RecordListFragment.K(observableEmitter, dialogInterface, index3);
            }
        }).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                RecordListFragment.H(observableEmitter, dialogInterface);
            }
        }).create().show();
    }

    public static /* synthetic */ void J(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ void K(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(Boolean.FALSE);
    }

    public static /* synthetic */ void L(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.RecentShot);
    }

    public static /* synthetic */ void M(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.UseLast);
    }

    public static /* synthetic */ void N(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.Ignored);
    }

    public static /* synthetic */ void O(ObservableEmitter observableEmitter, DialogInterface dialogInterface) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.Cancelled);
    }

    public /* synthetic */ void P(int index, int index2, final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(getMainActivity()).setTitle(index).setMessage(index2).setPositiveButton(R.string.f_record_start_camera, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                RecordListFragment.L(observableEmitter, dialogInterface, index3);
            }
        }).setNeutralButton(R.string.f_record_use_last_photo, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                RecordListFragment.M(observableEmitter, dialogInterface, index3);
            }
        }).setNegativeButton(R.string.f_record_ignore_photo, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index3) {
                RecordListFragment.N(observableEmitter, dialogInterface, index3);
            }
        }).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                RecordListFragment.O(observableEmitter, dialogInterface);
            }
        }).create().show();
    }

    private void R() {
        RecordCardAdapter.UploadedRecordViewHolder holder;
        FragmentActivity activity;
        if (this.recordCardAdapter.getItemCount() == 0 || (holder = (RecordCardAdapter.UploadedRecordViewHolder) ((RecyclerView) this.rootView.findViewById(R.id.f_recordlist_recyclerview)).findViewHolderForLayoutPosition(0)) == null || (activity = getActivity()) == null) {
            return;
        }
        if (this.sharedPreferences.getBoolean("upload", true)) {
            if (T(activity, holder)) {
                this.sharedPreferences.edit().putBoolean("upload", false).apply();
            }
        } else if (this.sharedPreferences.getBoolean("detail", true)) {
            if (Q(activity, holder)) {
                this.sharedPreferences.edit().putBoolean("detail", false).apply();
            }
        } else if (this.sharedPreferences.getBoolean("remove", true) && S(activity, holder)) {
            this.sharedPreferences.edit().putBoolean("remove", false).apply();
        }
    }

    public /* synthetic */ void z(File file, ObservableEmitter observableEmitter) {
        this.photoResultEmitter = observableEmitter;
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (intent.resolveActivity(getMainActivity().getPackageManager()) == null) {
            makeToast(R.string.f_record_error_start_camera, 0, new Object[0]);
        } else if (file != null) {
            this.takePictureLauncher.launch(FileProvider.getUriForFile(getContext(), "cn.edu.pku.openrunner.fileprovider", file));
        }
    }

    @Override
    public Observable<Boolean> callSystemCamera(final File file) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.z(file, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void cancelRefresh() {
        this.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void dismissWaitDialog() {
        ProgressDialog progressDialog = this.progressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.progressDialog = null;
        }
    }

    @Override
    public Observable<Pair<String, String>> launchIaaaLogin() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.A(observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void makeSnackBar(@StringRes int index, int index2, Object... objArr) {
        Snackbar.make(this.rootView, getString(index, objArr), index2).show();
    }

    @Override
    public void scrollRecyclerViewToTop() {
        ((RecyclerView) this.rootView.findViewById(R.id.f_recordlist_recyclerview)).smoothScrollToPosition(0);
    }

    @Override
    public void setWaitingDialogMessage(@StringRes int index) {
        ProgressDialog progressDialog = this.progressDialog;
        if (progressDialog != null) {
            progressDialog.setMessage(getString(index));
        }
    }

    @Override
    public Observable<Boolean> showConfirmDialog(@StringRes final int index, @StringRes final int index2) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.I(index, index2, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<RecordListContract.View.PhotoStatus> showPhotoDialog(@StringRes final int index, @StringRes final int index2) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.P(index, index2, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void showRecordDetailSheet(Bundle bundle) {
        RecordDetailViewFragment recordDetailViewFragment = new RecordDetailViewFragment();
        recordDetailViewFragment.setArguments(bundle);
        recordDetailViewFragment.show(getFragmentManager(), "Record Detail Fragment");
    }

    @Override
    public void showWaitingDialog() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        this.progressDialog = progressDialog;
        progressDialog.setProgressStyle(0);
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.show();
    }

    @Override
    public void toggleLoadingNotice(boolean z2) {
        this.emptyHintText.setVisibility(z2 ? 0 : 8);
    }

    @Override
    public void toggleNoDataNotice(boolean z2) {
        this.noticeText.setVisibility(z2 ? 0 : 8);
        this.emptyImageView.setVisibility(z2 ? 0 : 8);
    }

    public /* synthetic */ void E(View view) {
        ((MainActivity) getActivity()).switchFromRecordListToRunning();
    }

    public /* synthetic */ void G(Boolean bool) {
        R();
    }

    private boolean Q(Activity activity, RecordCardAdapter.UploadedRecordViewHolder holder) {
        View I = holder.I();
        if (I == null) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(I), getString(R.string.g_record_t_detail), getString(R.string.g_record_c_detail)).outerCircleColor(R.color.indigo_500).transparentTarget(true));
        return true;
    }

    private boolean S(Activity activity, RecordCardAdapter.UploadedRecordViewHolder holder) {
        View H = holder.H();
        if (H == null || !(holder instanceof RecordCardAdapter.RecordViewHolder)) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(H), getString(R.string.g_record_t_delete), getString(R.string.g_record_c_delete)).outerCircleColor(R.color.amber_500).transparentTarget(true));
        return true;
    }

    private boolean T(Activity activity, RecordCardAdapter.UploadedRecordViewHolder holder) {
        View J = holder.J();
        if (J == null || !(holder instanceof RecordCardAdapter.RecordViewHolder)) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(J), getString(R.string.g_record_t_upload), getString(R.string.g_record_c_upload)).outerCircleColor(R.color.purple_500).transparentTarget(true));
        return true;
    }

    @Override
    public File getExternalPhotoDir() {
        return getMainActivity().getExternalFilesDir(PhotoFile.PicutreType);
    }

    @Override
    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
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
        menuInflater.inflate(R.menu.fragment_recordlist, menu);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_recordlist, viewGroup, false);
        this.rootView = inflate;
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) inflate.findViewById(R.id.f_recordlist_swipeRefreshLayout);
        this.swipeRefreshLayout = swipeRefreshLayout;
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_500, R.color.green_500, R.color.blue_500);
        this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public final void onRefresh() {
                RecordListFragment.this.B();
            }
        });
        this.swipeRefreshLayout.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public final boolean canChildScrollUp(SwipeRefreshLayout swipeRefreshLayout2, View view) {
                boolean C;
                C = RecordListFragment.this.C(swipeRefreshLayout2, view);
                return C;
            }
        });
        RecyclerView recyclerView = (RecyclerView) this.rootView.findViewById(R.id.f_recordlist_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new ScrollStateResetListener());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), 1);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        RecordCardAdapter recordCardAdapter = new RecordCardAdapter();
        this.recordCardAdapter = recordCardAdapter;
        recordCardAdapter.setPresenter(this.presenter, new RecordCardAdapter.ResourcesProvider() {
            @Override
            public final Resources getResources() {
                return RecordListFragment.this.getResources();
            }
        });
        recyclerView.setAdapter(this.recordCardAdapter);
        new ItemTouchHelper(new ItemTouchHelperCallback(this.recordCardAdapter, new ItemTouchHelperCallback.SwipeStateListener() {
            @Override
            public final void onSwipeStateChanged(boolean active) {
                RecordListFragment.this.D(active);
            }
        }, new RecordCardAdapter.ResourcesProvider() {
            @Override
            public final Resources getResources() {
                return RecordListFragment.this.getResources();
            }
        })).attachToRecyclerView(recyclerView);
        this.noticeText = (TextView) this.rootView.findViewById(R.id.f_recordlist_txt_notice);
        this.emptyImageView = (ImageView) this.rootView.findViewById(R.id.f_recordlist_img);
        this.emptyHintText = (TextView) this.rootView.findViewById(R.id.f_recordlist_txt_loading);
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.v_main_fab_switch);
        this.startRunningFab = floatingActionButton;
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                RecordListFragment.this.E(view);
            }
        });
        ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).addOnOffsetChangedListener((AppBarLayout.OnOffsetChangedListener) new AppBarStateChangeWrapper(new AppBarStateChangeWrapper.OnAppBarStateChanged() {
            @Override
            public final void onStateChanged(AppBarLayout appBarLayout, RecordListFragment.AppBarStateChangeWrapper.State state) {
                RecordListFragment.this.F(appBarLayout, state);
            }
        }));
        this.sharedPreferences = getContext().getSharedPreferences(IntroActivity.GuidePreferencesName, 0);
        return this.rootView;
    }

    @Override
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (!z2) {
            Bundle arguments = getArguments();
            boolean z3 = false;
            if (arguments != null) {
                z3 = arguments.getBoolean("newRecord", false);
                arguments.remove("newRecord");
            }
            this.presenter.start(z3);
            Observable.just(Boolean.TRUE).delay(1L, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    RecordListFragment.this.G((Boolean) obj);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.f_m_recordlist_clear_preferences) {
            return super.onOptionsItemSelected(menuItem);
        }
        this.sharedPreferences.edit().remove("upload").remove("detail").remove("remove").apply();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        boolean z2 = false;
        if (arguments != null) {
            z2 = arguments.getBoolean("newRecord", false);
            arguments.remove("newRecord");
        }
        this.presenter.start(z2);
    }
}
