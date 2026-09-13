package cn.edu.pku.pkurunner.RecordList;

import android.os.Bundle;
import android.util.Pair;
import androidx.annotation.NonNull;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Exception.DataException;
import cn.edu.pku.pkurunner.Exception.ServerException;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Photo.PhotoCompression;
import cn.edu.pku.pkurunner.Photo.PhotoFile;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.RecordListContract;
import cn.edu.pku.pkurunner.RecordList.RecordListPresenter;
import cn.edu.pku.pkurunner.Utils.ClientUpdateNotice;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import cn.edu.pku.pkurunner.m;
import id.zelory.compressor.Compressor;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.xutils.common.Callback;

public class RecordListPresenter implements RecordListContract.Presenter {

    /* renamed from: a, reason: collision with root package name */
    private RecordListContract.View f7039a;

    /* renamed from: b, reason: collision with root package name */
    private Disposable f7040b;

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void C(Boolean bool) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void F(Boolean bool) {
    }

    @Override // cn.edu.pku.pkurunner.Contract.BasePresenter
    public void start() {
        start(false);
    }

    static /* synthetic */ class a {

        /* renamed from: a, reason: collision with root package name */
        static final /* synthetic */ int[] f7041a;

        static {
            int[] iArr = new int[RecordListContract.View.PhotoStatus.values().length];
            f7041a = iArr;
            try {
                iArr[RecordListContract.View.PhotoStatus.RecentShot.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f7041a[RecordListContract.View.PhotoStatus.UseLast.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f7041a[RecordListContract.View.PhotoStatus.Ignored.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f7041a[RecordListContract.View.PhotoStatus.Cancelled.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void B(Throwable th) {
        this.f7039a.makeSnackBar(R.string.p_record_delete_error, 0, th.getMessage());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void D(Throwable th) {
        if ((th instanceof ServerException) && ((ServerException) th).getErrorCode() == 15) {
            ClientUpdateNotice.showVersionLowDialog(this.f7039a.getContext()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.i0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    RecordListPresenter.C((Boolean) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void E(int i2, Record record) {
        String string = this.f7039a.getContext().getString(record.isVerified() ? R.string.p_record_verify_success : R.string.p_record_verify_failure);
        RecordListContract.View view = this.f7039a;
        Object[] objArr = new Object[2];
        objArr[0] = string;
        objArr[1] = record.isVerified() ? "" : ServerException.getLocalizedMessage(record.getInvalidReason());
        view.makeSnackBar(R.string.p_record_verify_result, -1, objArr);
        this.f7039a.getRecordCardAdapter().notifyItemChanged(i2);
        Data.refreshUserStatus().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.g0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.K((UserStatus) obj);
            }
        }, new Consumer() { // from class: v.h0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.D((Throwable) obj);
            }
        });
        this.f7039a.dismissWaitDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void G(Throwable th, Void r4) {
        this.f7039a.makeSnackBar(R.string.p_record_error_other, 0, th.getMessage());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void H(final Throwable th) {
        if (th instanceof ServerException) {
            ServerException serverException = (ServerException) th;
            if (serverException.getErrorCode() == 15) {
                this.f7039a.makeSnackBar(R.string.p_record_error_version_low, 0, new Object[0]);
                ClientUpdateNotice.showVersionLowDialog(this.f7039a.getContext()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.b0
                    @Override // io.reactivex.functions.Consumer
                    public final void accept(Object obj) {
                        RecordListPresenter.F((Boolean) obj);
                    }
                });
            } else if (serverException.getErrorCode() == 4) {
                this.f7039a.makeSnackBar(R.string.p_record_error_repeatedrequest, 0, th.getMessage());
            } else {
                this.f7039a.makeSnackBar(R.string.p_record_error_server, 0, th.getMessage());
            }
        } else if (th instanceof DataException) {
            this.f7039a.makeSnackBar(R.string.p_record_error_database, 0, th.getMessage());
        } else {
            Network.interceptIfSocketTimeout(th, new Callback.Callable() { // from class: v.d0
                @Override // org.xutils.common.Callback.Callable
                public final void call(Object obj) {
                    RecordListPresenter.this.G(th, (Void) obj);
                }
            });
        }
        this.f7039a.dismissWaitDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void K(UserStatus userStatus) {
        this.f7039a.getMainActivity().refreshUserStatusNotice();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void M(Record record) {
        this.f7039a.dismissWaitDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable("record", record);
        this.f7039a.showRecordDetailSheet(bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void N(Throwable th) {
        this.f7039a.dismissWaitDialog();
        this.f7039a.makeToast(R.string.p_record_detail_error_track, 1, th.getMessage());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void O(ArrayList arrayList) {
        if (this.f7039a.getRecordCardAdapter() != null) {
            this.f7039a.getRecordCardAdapter().notifyDataInvalid();
            this.f7039a.getRecordCardAdapter().notifyDataSetChanged();
            RecordListContract.View view = this.f7039a;
            view.toggleNoDataNotice(view.getRecordCardAdapter().getItemCount() == 0);
        }
        this.f7039a.makeToast(R.string.p_record_sync_data_success, 0, new Object[0]);
        this.f7039a.cancelRefresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void P(Throwable th, Void r4) {
        this.f7039a.makeToast(R.string.p_record_sync_data_fail, 0, th.getMessage());
        this.f7039a.cancelRefresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void Q(final Throwable th) {
        Network.interceptIfSocketTimeout(th, new Callback.Callable() { // from class: v.e0
            @Override // org.xutils.common.Callback.Callable
            public final void call(Object obj) {
                RecordListPresenter.this.P(th, (Void) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource S(Record record, RecordListContract.View.PhotoStatus photoStatus) {
        int i2 = a.f7041a[photoStatus.ordinal()];
        return i2 != 1 ? i2 != 2 ? i2 != 3 ? i2 != 4 ? Observable.error(new IllegalStateException("???")) : Observable.error(new Throwable(IaaaWrapper.RESULT_CANCEL)) : Data.setPhotoForRecord(record, null) : Y(record) : X(record);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource T(Boolean bool) {
        this.f7039a.showWaitingDialog();
        this.f7039a.setWaitingDialogMessage(R.string.p_record_waiting_message_before_sdk);
        return Observable.just(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource U(Boolean bool) {
        return this.f7039a.launchIaaaLogin();
    }

    private void W() {
        if (this.f7039a.getRecordCardAdapter().getItemCount() > 0) {
            this.f7039a.scrollRecyclerViewToTop();
            this.f7039a.getRecordCardAdapter().notifyFirstElementElevation();
        }
    }

    private Observable X(final Record record) {
        try {
            final File createPhotoFile = PhotoFile.createPhotoFile(this.f7039a.getExternalPhotoDir());
            return this.f7039a.callSystemCamera(createPhotoFile).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.a0
                @Override // io.reactivex.functions.Function
                public final Object apply(Object obj) {
                    ObservableSource R;
                    R = RecordListPresenter.this.R(createPhotoFile, record, (Boolean) obj);
                    return R;
                }
            }).subscribeOn(AndroidSchedulers.mainThread());
        } catch (IOException e2) {
            e2.printStackTrace();
            return Observable.error(new Throwable(String.format(this.f7039a.getContext().getString(R.string.p_record_photo_error_file, e2.getMessage()), e2.getMessage())));
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void deleteRecord(final int i2, final int i3) {
        this.f7040b = this.f7039a.showConfirmDialog(R.string.p_record_delete_title, R.string.p_record_delete_content).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.r
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource y2;
                y2 = RecordListPresenter.this.y(i3, (Boolean) obj);
                return y2;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.c0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource z2;
                z2 = RecordListPresenter.this.z(i2, i3, (Boolean) obj);
                return z2;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.j0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.A(i3, (Boolean) obj);
            }
        }, new Consumer() { // from class: v.k0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.B((Throwable) obj);
            }
        });
    }

    /* renamed from: handleIaaaLogin, reason: merged with bridge method [inline-methods] */
    public void V(Pair<String, String> pair, final int i2, final int i3) {
        this.f7039a.setWaitingDialogMessage(R.string.p_record_waiting_message_after_sdk);
        Data.changeUserToken((String) pair.second).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.o0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource I;
                I = RecordListPresenter.I((Boolean) obj);
                return I;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.p0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource J;
                J = RecordListPresenter.J(i2, (Boolean) obj);
                return J;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.q0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.E(i3, (Record) obj);
            }
        }, new Consumer() { // from class: v.s
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.H((Throwable) obj);
            }
        });
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void refreshList() {
        this.f7039a.getRecordCardAdapter().notifyDataInvalid();
        this.f7039a.getRecordCardAdapter().notifyDataSetChanged();
        RecordListContract.View view = this.f7039a;
        view.toggleNoDataNotice(view.getRecordCardAdapter().getItemCount() == 0);
        this.f7039a.toggleLoadingNotice(false);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void start(boolean z2) {
        if (Data.isValid()) {
            this.f7039a.toggleLoadingNotice(true);
            refreshList();
            if (z2) {
                W();
            }
        }
    }

    public RecordListPresenter(@NonNull RecordListContract.View view) {
        this.f7039a = view;
        view.setPresenter(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void A(int i2, Boolean bool) {
        if (bool.booleanValue()) {
            this.f7039a.makeSnackBar(R.string.p_record_delete_success, -1, new Object[0]);
            this.f7039a.getRecordCardAdapter().notifyDataInvalid();
            this.f7039a.getRecordCardAdapter().notifyItemRemoved(i2);
            return;
        }
        this.f7039a.makeSnackBar(R.string.p_record_delete_error_other, 0, new Object[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource I(Boolean bool) {
        return Data.login();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource J(int i2, Boolean bool) {
        return Data.uploadRecordToServer(i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void L(Record record, int i2, final ObservableEmitter observableEmitter) {
        if (record.isDetailed()) {
            observableEmitter.onNext(record);
            return;
        }
        Observable<Record> observeOn = Data.getSingleRecordFromServer(i2).observeOn(AndroidSchedulers.mainThread());
        Objects.requireNonNull(observableEmitter);
        observeOn.subscribe(new Consumer() { // from class: v.f0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                observableEmitter.onNext((Record) obj);
            }
        }, new m(observableEmitter));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource R(File file, Record record, Boolean bool) {
        try {
            if (!bool.booleanValue()) {
                return Observable.error(new Throwable(this.f7039a.getContext().getString(R.string.p_record_photo_error_take)));
            }
            return Data.setPhotoForRecord(record, PhotoCompression.compressPhoto(this.f7039a.getExternalPhotoDir(), new Compressor(this.f7039a.getContext()), file.getName()));
        } catch (IOException e2) {
            e2.printStackTrace();
            return Observable.error(new Throwable(this.f7039a.getContext().getString(R.string.p_record_photo_error_compress)));
        }
    }

    private Observable Y(Record record) {
        String lastUsedPhoto = Data.getLastUsedPhoto();
        if ("".equals(lastUsedPhoto)) {
            return Observable.error(new Throwable(this.f7039a.getContext().getString(R.string.p_record_photo_error_no_last)));
        }
        return Data.setPhotoForRecord(record, lastUsedPhoto);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource y(int i2, Boolean bool) {
        if (bool.booleanValue()) {
            return this.f7039a.showConfirmDialog(R.string.p_record_delete_title_reconfirm, R.string.p_record_delete_content_reconfirm);
        }
        this.f7039a.makeToast(R.string.p_record_delete_canceled, 0, new Object[0]);
        this.f7039a.getRecordCardAdapter().notifyItemChanged(i2);
        this.f7040b.dispose();
        return Observable.just(Boolean.FALSE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource z(int i2, int i3, Boolean bool) {
        if (bool.booleanValue()) {
            return Data.deleteRecordById(i2);
        }
        this.f7039a.makeToast(R.string.p_record_delete_canceled, 0, new Object[0]);
        this.f7039a.getRecordCardAdapter().notifyItemChanged(i3);
        this.f7040b.dispose();
        return Observable.just(Boolean.FALSE);
    }

    public void handleIaaaLoginError(Throwable th) {
        if (!IaaaWrapper.RESULT_CANCEL.equals(th.getMessage())) {
            this.f7039a.makeToast(R.string.p_record_error_other, 0, th.getLocalizedMessage());
        }
        this.f7039a.dismissWaitDialog();
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void showRecordDetail(final int i2) {
        final Record singleRecord = Data.getSingleRecord(i2);
        if (singleRecord == null) {
            this.f7039a.makeToast(R.string.p_record_detail_error, 1, new Object[0]);
            return;
        }
        this.f7039a.showWaitingDialog();
        this.f7039a.setWaitingDialogMessage(R.string.p_record_waiting_message_after_sdk);
        Observable.create(new ObservableOnSubscribe() { // from class: v.l0
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListPresenter.L(singleRecord, i2, observableEmitter);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.m0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.M((Record) obj);
            }
        }, new Consumer() { // from class: v.n0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.N((Throwable) obj);
            }
        });
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void syncData() {
        if (Data.getUser().isOffline().booleanValue()) {
            this.f7039a.cancelRefresh();
        } else {
            Data.getRecordsFromServer().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.y
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    RecordListPresenter.this.O((ArrayList) obj);
                }
            }, new Consumer() { // from class: v.z
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    RecordListPresenter.this.Q((Throwable) obj);
                }
            });
        }
    }

    @Override // cn.edu.pku.pkurunner.RecordList.RecordListContract.Presenter
    public void uploadVerifyRecord(final int i2, final int i3) {
        final Record singleRecord = Data.getSingleRecord(i2);
        this.f7039a.showPhotoDialog(R.string.p_record_prepare_take_photo_title, R.string.p_record_prepare_take_photo_message).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.t
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource S;
                S = RecordListPresenter.this.S(singleRecord, (RecordListContract.View.PhotoStatus) obj);
                return S;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.u
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource T;
                T = RecordListPresenter.this.T((Boolean) obj);
                return T;
            }
        }).delay(1500L, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: v.v
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource U;
                U = RecordListPresenter.this.U((Boolean) obj);
                return U;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.w
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.V((Pair) obj, i2, i3);
            }
        }, new Consumer() { // from class: v.x
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                RecordListPresenter.this.handleIaaaLoginError((Throwable) obj);
            }
        });
    }
}
