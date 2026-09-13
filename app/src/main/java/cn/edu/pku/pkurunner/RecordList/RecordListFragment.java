package cn.edu.pku.pkurunner.RecordList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
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

    /* renamed from: d, reason: collision with root package name */
    private View f7022d;

    /* renamed from: e, reason: collision with root package name */
    private SwipeRefreshLayout f7023e;

    /* renamed from: f, reason: collision with root package name */
    private RecordCardAdapter f7024f;

    /* renamed from: g, reason: collision with root package name */
    private TextView f7025g;

    /* renamed from: h, reason: collision with root package name */
    private TextView f7026h;

    /* renamed from: i, reason: collision with root package name */
    private ImageView f7027i;

    /* renamed from: j, reason: collision with root package name */
    private ProgressDialog f7028j;

    /* renamed from: k, reason: collision with root package name */
    private boolean f7029k;

    /* renamed from: l, reason: collision with root package name */
    private FloatingActionButton f7030l;

    /* renamed from: m, reason: collision with root package name */
    private RecordListContract.Presenter f7031m;

    /* renamed from: n, reason: collision with root package name */
    private ObservableEmitter f7032n;

    /* renamed from: o, reason: collision with root package name */
    private ObservableEmitter f7033o;

    /* renamed from: p, reason: collision with root package name */
    private SharedPreferences f7034p;

    static class AppBarStateChangeWrapper implements AppBarLayout.OnOffsetChangedListener {

        /* renamed from: a, reason: collision with root package name */
        private State f7035a = State.IDLE;

        /* renamed from: b, reason: collision with root package name */
        private a f7036b;

        public enum State {
            EXPANDED,
            COLLAPSED,
            IDLE
        }

        interface a {
            void a(AppBarLayout appBarLayout, State state);
        }

        @Override // com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener, com.google.android.material.appbar.AppBarLayout.BaseOnOffsetChangedListener
        public void onOffsetChanged(AppBarLayout appBarLayout, int i2) {
            if (i2 == 0) {
                State state = this.f7035a;
                State state2 = State.EXPANDED;
                if (state != state2) {
                    this.f7036b.a(appBarLayout, state2);
                }
                this.f7035a = state2;
                return;
            }
            if (Math.abs(i2) >= appBarLayout.getTotalScrollRange() * 0.6d) {
                State state3 = this.f7035a;
                State state4 = State.COLLAPSED;
                if (state3 != state4) {
                    this.f7036b.a(appBarLayout, state4);
                }
                this.f7035a = state4;
                return;
            }
            State state5 = this.f7035a;
            State state6 = State.IDLE;
            if (state5 != state6) {
                this.f7036b.a(appBarLayout, state6);
            }
            this.f7035a = state6;
        }

        public AppBarStateChangeWrapper(a aVar) {
            this.f7036b = aVar;
        }
    }

    class a extends RecyclerView.OnScrollListener {
        a() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrollStateChanged(RecyclerView recyclerView, int i2) {
            super.onScrollStateChanged(recyclerView, i2);
            if (i2 == 0) {
                RecordListFragment.this.f7029k = false;
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrolled(RecyclerView recyclerView, int i2, int i3) {
            super.onScrolled(recyclerView, i2, i3);
            RecordListFragment.this.f7029k = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean C(SwipeRefreshLayout swipeRefreshLayout, View view) {
        return this.f7029k;
    }

    private Rect y(View view) {
        int[] iArr = new int[2];
        view.getLocationInWindow(iArr);
        int i2 = iArr[0];
        int width = view.getWidth() + i2;
        int i3 = iArr[1];
        return new Rect(i2, i3, width, view.getHeight() + i3);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public RecordCardAdapter getRecordCardAdapter() {
        return this.f7024f;
    }

    @Override // cn.edu.pku.pkurunner.Contract.BaseView
    public void setPresenter(@NonNull RecordListContract.Presenter presenter) {
        this.f7031m = presenter;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void A(ObservableEmitter observableEmitter) {
        this.f7033o = observableEmitter;
        IaaaWrapper.LaunchIaaaLogin(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void B() {
        this.f7031m.syncData();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void D(boolean z2) {
        this.f7023e.setEnabled(z2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void F(AppBarLayout appBarLayout, AppBarStateChangeWrapper.State state) {
        if (state == AppBarStateChangeWrapper.State.COLLAPSED) {
            this.f7030l.setClickable(false);
            this.f7030l.hide();
        } else {
            this.f7030l.setClickable(true);
            this.f7030l.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void H(ObservableEmitter observableEmitter, DialogInterface dialogInterface) {
        observableEmitter.onNext(Boolean.FALSE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void I(int i2, int i3, final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(getContext()).setTitle(i2).setMessage(i3).setPositiveButton(R.string.f_record_dialog_positive, new DialogInterface.OnClickListener() { // from class: v.i
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                RecordListFragment.J(observableEmitter, dialogInterface, i4);
            }
        }).setNegativeButton(R.string.f_record_dialog_negative, new DialogInterface.OnClickListener() { // from class: v.j
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                RecordListFragment.K(observableEmitter, dialogInterface, i4);
            }
        }).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: v.k
            @Override // android.content.DialogInterface.OnCancelListener
            public final void onCancel(DialogInterface dialogInterface) {
                RecordListFragment.H(observableEmitter, dialogInterface);
            }
        }).create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void J(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void K(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(Boolean.FALSE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void L(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.RecentShot);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void M(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.UseLast);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void N(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.Ignored);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void O(ObservableEmitter observableEmitter, DialogInterface dialogInterface) {
        observableEmitter.onNext(RecordListContract.View.PhotoStatus.Cancelled);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void P(int i2, int i3, final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(getMainActivity()).setTitle(i2).setMessage(i3).setPositiveButton(R.string.f_record_start_camera, new DialogInterface.OnClickListener() { // from class: v.e
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                RecordListFragment.L(observableEmitter, dialogInterface, i4);
            }
        }).setNeutralButton(R.string.f_record_use_last_photo, new DialogInterface.OnClickListener() { // from class: v.f
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                RecordListFragment.M(observableEmitter, dialogInterface, i4);
            }
        }).setNegativeButton(R.string.f_record_ignore_photo, new DialogInterface.OnClickListener() { // from class: v.g
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i4) {
                RecordListFragment.N(observableEmitter, dialogInterface, i4);
            }
        }).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: v.h
            @Override // android.content.DialogInterface.OnCancelListener
            public final void onCancel(DialogInterface dialogInterface) {
                RecordListFragment.O(observableEmitter, dialogInterface);
            }
        }).create().show();
    }

    private void R() {
        RecordCardAdapter.b bVar;
        FragmentActivity activity;
        if (this.f7024f.getItemCount() == 0 || (bVar = (RecordCardAdapter.b) ((RecyclerView) this.f7022d.findViewById(R.id.f_recordlist_recyclerview)).findViewHolderForLayoutPosition(0)) == null || (activity = getActivity()) == null) {
            return;
        }
        if (this.f7034p.getBoolean("upload", true)) {
            if (T(activity, bVar)) {
                this.f7034p.edit().putBoolean("upload", false).apply();
            }
        } else if (this.f7034p.getBoolean("detail", true)) {
            if (Q(activity, bVar)) {
                this.f7034p.edit().putBoolean("detail", false).apply();
            }
        } else if (this.f7034p.getBoolean("remove", true) && S(activity, bVar)) {
            this.f7034p.edit().putBoolean("remove", false).apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void z(File file, ObservableEmitter observableEmitter) {
        this.f7032n = observableEmitter;
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (intent.resolveActivity(getMainActivity().getPackageManager()) == null) {
            makeToast(R.string.f_record_error_start_camera, 0, new Object[0]);
        } else if (file != null) {
            intent.putExtra("output", FileProvider.getUriForFile(getContext(), "cn.edu.pku.pkurunner.fileprovider", file));
            startActivityForResult(intent, 1001);
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public Observable<Boolean> callSystemCamera(final File file) {
        return Observable.create(new ObservableOnSubscribe() { // from class: v.c
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.z(file, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void cancelRefresh() {
        this.f7023e.setRefreshing(false);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void dismissWaitDialog() {
        ProgressDialog progressDialog = this.f7028j;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.f7028j = null;
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public Observable<Pair<String, String>> launchIaaaLogin() {
        return Observable.create(new ObservableOnSubscribe() { // from class: v.d
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.A(observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void makeSnackBar(@StringRes int i2, int i3, Object... objArr) {
        Snackbar.make(this.f7022d, getString(i2, objArr), i3).show();
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int i2, int i3, Intent intent) {
        if (i2 == 602) {
            this.f7032n.onNext(Boolean.valueOf(i3 == -1));
            this.f7032n = null;
        } else {
            if (i2 != 1651) {
                return;
            }
            if (i3 == -1) {
                Bundle extras = intent.getExtras();
                if (IaaaWrapper.RESULT_CANCEL.equals(extras.getString(IaaaWrapper.EXTRA_iAAA_RESULT))) {
                    this.f7033o.onError(new Throwable(getString(R.string.f_record_error_login_cancelled)));
                } else {
                    this.f7033o.onNext(new Pair(extras.getString(IaaaWrapper.EXTRA_iAAA_UID), extras.getString(IaaaWrapper.EXTRA_iAAA_TOKEN)));
                }
            } else {
                this.f7033o.onError(new Throwable(getString(R.string.f_record_error_login_fail)));
            }
            this.f7033o = null;
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void scrollRecyclerViewToTop() {
        ((RecyclerView) this.f7022d.findViewById(R.id.f_recordlist_recyclerview)).smoothScrollToPosition(0);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void setWaitingDialogMessage(@StringRes int i2) {
        ProgressDialog progressDialog = this.f7028j;
        if (progressDialog != null) {
            progressDialog.setMessage(getString(i2));
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public Observable<Boolean> showConfirmDialog(@StringRes final int i2, @StringRes final int i3) {
        return Observable.create(new ObservableOnSubscribe() { // from class: v.o
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.I(i2, i3, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public Observable<RecordListContract.View.PhotoStatus> showPhotoDialog(@StringRes final int i2, @StringRes final int i3) {
        return Observable.create(new ObservableOnSubscribe() { // from class: v.q
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListFragment.this.P(i2, i3, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void showRecordDetailSheet(Bundle bundle) {
        RecordDetailViewFragment recordDetailViewFragment = new RecordDetailViewFragment();
        recordDetailViewFragment.setArguments(bundle);
        recordDetailViewFragment.show(getFragmentManager(), "Record Detail Fragment");
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void showWaitingDialog() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        this.f7028j = progressDialog;
        progressDialog.setProgressStyle(0);
        this.f7028j.setIndeterminate(false);
        this.f7028j.setCancelable(false);
        this.f7028j.show();
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void toggleLoadingNotice(boolean z2) {
        this.f7026h.setVisibility(z2 ? 0 : 8);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public void toggleNoDataNotice(boolean z2) {
        this.f7025g.setVisibility(z2 ? 0 : 8);
        this.f7027i.setVisibility(z2 ? 0 : 8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void E(View view) {
        ((MainActivity) getActivity()).switchFromRecordListToRunning();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void G(Boolean bool) {
        R();
    }

    private boolean Q(Activity activity, RecordCardAdapter.b bVar) {
        View I = bVar.I();
        if (I == null) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(I), getString(R.string.g_record_t_detail), getString(R.string.g_record_c_detail)).outerCircleColor(R.color.indigo_500).transparentTarget(true));
        return true;
    }

    private boolean S(Activity activity, RecordCardAdapter.b bVar) {
        View H = bVar.H();
        if (H == null || !(bVar instanceof RecordCardAdapter.d)) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(H), getString(R.string.g_record_t_delete), getString(R.string.g_record_c_delete)).outerCircleColor(R.color.amber_500).transparentTarget(true));
        return true;
    }

    private boolean T(Activity activity, RecordCardAdapter.b bVar) {
        View J = bVar.J();
        if (J == null || !(bVar instanceof RecordCardAdapter.d)) {
            return false;
        }
        TapTargetView.showFor(activity, TapTarget.forBounds(y(J), getString(R.string.g_record_t_upload), getString(R.string.g_record_c_upload)).outerCircleColor(R.color.purple_500).transparentTarget(true));
        return true;
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public File getExternalPhotoDir() {
        return getMainActivity().getExternalFilesDir(PhotoFile.PicutreType);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.View
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
        menuInflater.inflate(R.menu.fragment_recordlist, menu);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_recordlist, viewGroup, false);
        this.f7022d = inflate;
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) inflate.findViewById(R.id.f_recordlist_swipeRefreshLayout);
        this.f7023e = swipeRefreshLayout;
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_500, R.color.green_500, R.color.blue_500);
        this.f7023e.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() { // from class: v.l
            @Override // androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
            public final void onRefresh() {
                RecordListFragment.this.B();
            }
        });
        this.f7023e.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() { // from class: v.m
            @Override // androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnChildScrollUpCallback
            public final boolean canChildScrollUp(SwipeRefreshLayout swipeRefreshLayout2, View view) {
                boolean C;
                C = RecordListFragment.this.C(swipeRefreshLayout2, view);
                return C;
            }
        });
        RecyclerView recyclerView = (RecyclerView) this.f7022d.findViewById(R.id.f_recordlist_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new a());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), 1);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        RecordCardAdapter recordCardAdapter = new RecordCardAdapter();
        this.f7024f = recordCardAdapter;
        recordCardAdapter.setPresenter(this.f7031m, new RecordCardAdapter.c() { // from class: cn.edu.pku.pkurunner.RecordList.e
            @Override // cn.edu.pku.pkurunner.RecordList.RecordCardAdapter.c
            public final Resources a() {
                return RecordListFragment.this.getResources();
            }
        });
        recyclerView.setAdapter(this.f7024f);
        new ItemTouchHelper(new ItemTouchHelperCallback(this.f7024f, new ItemTouchHelperCallback.a() { // from class: cn.edu.pku.pkurunner.RecordList.f
            @Override // cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback.a
            public final void a(boolean z2) {
                RecordListFragment.this.D(z2);
            }
        }, new RecordCardAdapter.c() { // from class: cn.edu.pku.pkurunner.RecordList.e
            @Override // cn.edu.pku.pkurunner.RecordList.RecordCardAdapter.c
            public final Resources a() {
                return RecordListFragment.this.getResources();
            }
        })).attachToRecyclerView(recyclerView);
        this.f7025g = (TextView) this.f7022d.findViewById(R.id.f_recordlist_txt_notice);
        this.f7027i = (ImageView) this.f7022d.findViewById(R.id.f_recordlist_img);
        this.f7026h = (TextView) this.f7022d.findViewById(R.id.f_recordlist_txt_loading);
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.v_main_fab_switch);
        this.f7030l = floatingActionButton;
        floatingActionButton.setOnClickListener(new View.OnClickListener() { // from class: v.n
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                RecordListFragment.this.E(view);
            }
        });
        ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).addOnOffsetChangedListener((AppBarLayout.OnOffsetChangedListener) new AppBarStateChangeWrapper(new AppBarStateChangeWrapper.a() { // from class: cn.edu.pku.pkurunner.RecordList.g
            @Override // cn.edu.pku.pkurunner.RecordList.RecordListFragment.AppBarStateChangeWrapper.a
            public final void a(AppBarLayout appBarLayout, RecordListFragment.AppBarStateChangeWrapper.State state) {
                RecordListFragment.this.F(appBarLayout, state);
            }
        }));
        this.f7034p = getContext().getSharedPreferences(IntroActivity.GuidePreferencesName, 0);
        return this.f7022d;
    }

    @Override // androidx.fragment.app.Fragment
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (!z2) {
            Bundle arguments = getArguments();
            boolean z3 = false;
            if (arguments != null) {
                z3 = arguments.getBoolean("newRecord", false);
                arguments.remove("newRecord");
            }
            this.f7031m.start(z3);
            Observable.just(Boolean.TRUE).delay(1L, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.p
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    RecordListFragment.this.G((Boolean) obj);
                }
            });
        }
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.f_m_recordlist_clear_preferences) {
            return super.onOptionsItemSelected(menuItem);
        }
        this.f7034p.edit().remove("upload").remove("detail").remove("remove").apply();
        return true;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        boolean z2 = false;
        if (arguments != null) {
            z2 = arguments.getBoolean("newRecord", false);
            arguments.remove("newRecord");
        }
        this.f7031m.start(z2);
    }
}
