package cn.edu.pku.pkurunner.RecordList;

import android.os.Bundle;
import android.util.Pair;
import androidx.annotation.NonNull;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.EmitterErrorConsumer;
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

    private RecordListContract.View view;

    private Disposable disposable;

    public static /* synthetic */ void C(Boolean bool) {
    }

    public static /* synthetic */ void F(Boolean bool) {
    }

    @Override
    public void start() {
        start(false);
    }

    static /* synthetic */ class PhotoStatusSwitchMap {

        static final /* synthetic */ int[] PHOTO_STATUS_SWITCH_MAP;

        static {
            int[] iArr = new int[RecordListContract.View.PhotoStatus.values().length];
            PHOTO_STATUS_SWITCH_MAP = iArr;
            try {
                iArr[RecordListContract.View.PhotoStatus.RecentShot.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                PHOTO_STATUS_SWITCH_MAP[RecordListContract.View.PhotoStatus.UseLast.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                PHOTO_STATUS_SWITCH_MAP[RecordListContract.View.PhotoStatus.Ignored.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                PHOTO_STATUS_SWITCH_MAP[RecordListContract.View.PhotoStatus.Cancelled.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    public /* synthetic */ void B(Throwable th) {
        this.view.makeSnackBar(R.string.p_record_delete_error, 0, th.getMessage());
    }

    public /* synthetic */ void D(Throwable th) {
        if ((th instanceof ServerException) && ((ServerException) th).getErrorCode() == 15) {
            ClientUpdateNotice.showVersionLowDialog(this.view.getContext()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    RecordListPresenter.C((Boolean) obj);
                }
            });
        }
    }

    public /* synthetic */ void E(int index, Record record) {
        String string = this.view.getContext().getString(record.isVerified() ? R.string.p_record_verify_success : R.string.p_record_verify_failure);
        RecordListContract.View view = this.view;
        Object[] objArr = new Object[2];
        objArr[0] = string;
        objArr[1] = record.isVerified() ? "" : ServerException.getLocalizedMessage(record.getInvalidReason());
        view.makeSnackBar(R.string.p_record_verify_result, -1, objArr);
        this.view.getRecordCardAdapter().notifyItemChanged(index);
        Data.refreshUserStatus().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.K((UserStatus) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.D((Throwable) obj);
            }
        });
        this.view.dismissWaitDialog();
    }

    public /* synthetic */ void G(Throwable th, Void r4) {
        this.view.makeSnackBar(R.string.p_record_error_other, 0, th.getMessage());
    }

    public /* synthetic */ void H(final Throwable th) {
        if (th instanceof ServerException) {
            ServerException serverException = (ServerException) th;
            if (serverException.getErrorCode() == 15) {
                this.view.makeSnackBar(R.string.p_record_error_version_low, 0, new Object[0]);
                ClientUpdateNotice.showVersionLowDialog(this.view.getContext()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        RecordListPresenter.F((Boolean) obj);
                    }
                });
            } else if (serverException.getErrorCode() == 4) {
                this.view.makeSnackBar(R.string.p_record_error_repeatedrequest, 0, th.getMessage());
            } else {
                this.view.makeSnackBar(R.string.p_record_error_server, 0, th.getMessage());
            }
        } else if (th instanceof DataException) {
            this.view.makeSnackBar(R.string.p_record_error_database, 0, th.getMessage());
        } else {
            Network.interceptIfSocketTimeout(th, new Callback.Callable() {
                @Override
                public final void call(Object obj) {
                    RecordListPresenter.this.G(th, (Void) obj);
                }
            });
        }
        this.view.dismissWaitDialog();
    }

    public /* synthetic */ void K(UserStatus userStatus) {
        this.view.getMainActivity().refreshUserStatusNotice();
    }

    public /* synthetic */ void M(Record record) {
        this.view.dismissWaitDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable("record", record);
        this.view.showRecordDetailSheet(bundle);
    }

    public /* synthetic */ void N(Throwable th) {
        this.view.dismissWaitDialog();
        this.view.makeToast(R.string.p_record_detail_error_track, 1, th.getMessage());
    }

    public /* synthetic */ void O(ArrayList arrayList) {
        if (this.view.getRecordCardAdapter() != null) {
            this.view.getRecordCardAdapter().notifyDataInvalid();
            this.view.getRecordCardAdapter().notifyDataSetChanged();
            RecordListContract.View view = this.view;
            view.toggleNoDataNotice(view.getRecordCardAdapter().getItemCount() == 0);
        }
        this.view.makeToast(R.string.p_record_sync_data_success, 0, new Object[0]);
        this.view.cancelRefresh();
    }

    public /* synthetic */ void P(Throwable th, Void r4) {
        this.view.makeToast(R.string.p_record_sync_data_fail, 0, th.getMessage());
        this.view.cancelRefresh();
    }

    public /* synthetic */ void Q(final Throwable th) {
        Network.interceptIfSocketTimeout(th, new Callback.Callable() {
            @Override
            public final void call(Object obj) {
                RecordListPresenter.this.P(th, (Void) obj);
            }
        });
    }

    public /* synthetic */ ObservableSource S(Record record, RecordListContract.View.PhotoStatus photoStatus) {
        int index = PhotoStatusSwitchMap.PHOTO_STATUS_SWITCH_MAP[photoStatus.ordinal()];
        return index != 1 ? index != 2 ? index != 3 ? index != 4 ? Observable.error(new IllegalStateException("???")) : Observable.error(new Throwable(IaaaWrapper.RESULT_CANCEL)) : Data.setPhotoForRecord(record, null) : Y(record) : X(record);
    }

    public /* synthetic */ ObservableSource T(Boolean bool) {
        this.view.showWaitingDialog();
        this.view.setWaitingDialogMessage(R.string.p_record_waiting_message_before_sdk);
        return Observable.just(Boolean.TRUE);
    }

    public /* synthetic */ ObservableSource U(Boolean bool) {
        return this.view.launchIaaaLogin();
    }

    private void W() {
        if (this.view.getRecordCardAdapter().getItemCount() > 0) {
            this.view.scrollRecyclerViewToTop();
            this.view.getRecordCardAdapter().notifyFirstElementElevation();
        }
    }

    private Observable X(final Record record) {
        try {
            final File createPhotoFile = PhotoFile.createPhotoFile(this.view.getExternalPhotoDir());
            return this.view.callSystemCamera(createPhotoFile).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
                @Override
                public final Object apply(Object obj) {
                    ObservableSource R;
                    R = RecordListPresenter.this.R(createPhotoFile, record, (Boolean) obj);
                    return R;
                }
            }).subscribeOn(AndroidSchedulers.mainThread());
        } catch (IOException e2) {
            e2.printStackTrace();
            return Observable.error(new Throwable(String.format(this.view.getContext().getString(R.string.p_record_photo_error_file, e2.getMessage()), e2.getMessage())));
        }
    }

    @Override
    public void deleteRecord(final int index, final int index2) {
        this.disposable = this.view.showConfirmDialog(R.string.p_record_delete_title, R.string.p_record_delete_content).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource y2;
                y2 = RecordListPresenter.this.y(index2, (Boolean) obj);
                return y2;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource z2;
                z2 = RecordListPresenter.this.z(index, index2, (Boolean) obj);
                return z2;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.A(index2, (Boolean) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.B((Throwable) obj);
            }
        });
    }

    public void V(Pair<String, String> pair, final int index, final int index2) {
        this.view.setWaitingDialogMessage(R.string.p_record_waiting_message_after_sdk);
        Data.changeUserToken((String) pair.second).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource I;
                I = RecordListPresenter.I((Boolean) obj);
                return I;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource J;
                J = RecordListPresenter.J(index, (Boolean) obj);
                return J;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.E(index2, (Record) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.H((Throwable) obj);
            }
        });
    }

    @Override
    public void refreshList() {
        this.view.getRecordCardAdapter().notifyDataInvalid();
        this.view.getRecordCardAdapter().notifyDataSetChanged();
        RecordListContract.View view = this.view;
        view.toggleNoDataNotice(view.getRecordCardAdapter().getItemCount() == 0);
        this.view.toggleLoadingNotice(false);
    }

    @Override
    public void start(boolean z2) {
        if (Data.isValid()) {
            this.view.toggleLoadingNotice(true);
            refreshList();
            if (z2) {
                W();
            }
        }
    }

    public RecordListPresenter(@NonNull RecordListContract.View view) {
        this.view = view;
        view.setPresenter(this);
    }

    public /* synthetic */ void A(int index, Boolean bool) {
        if (bool.booleanValue()) {
            this.view.makeSnackBar(R.string.p_record_delete_success, -1, new Object[0]);
            this.view.getRecordCardAdapter().notifyDataInvalid();
            this.view.getRecordCardAdapter().notifyItemRemoved(index);
            return;
        }
        this.view.makeSnackBar(R.string.p_record_delete_error_other, 0, new Object[0]);
    }

    public static /* synthetic */ ObservableSource I(Boolean bool) {
        return Data.login();
    }

    public static /* synthetic */ ObservableSource J(int index, Boolean bool) {
        return Data.uploadRecordToServer(index);
    }

    public static /* synthetic */ void L(Record record, int index, final ObservableEmitter observableEmitter) {
        if (record.isDetailed()) {
            observableEmitter.onNext(record);
            return;
        }
        Observable<Record> observeOn = Data.getSingleRecordFromServer(index).observeOn(AndroidSchedulers.mainThread());
        Objects.requireNonNull(observableEmitter);
        observeOn.subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                observableEmitter.onNext((Record) obj);
            }
        }, new EmitterErrorConsumer(observableEmitter));
    }

    public /* synthetic */ ObservableSource R(File file, Record record, Boolean bool) {
        try {
            if (!bool.booleanValue()) {
                return Observable.error(new Throwable(this.view.getContext().getString(R.string.p_record_photo_error_take)));
            }
            return Data.setPhotoForRecord(record, PhotoCompression.compressPhoto(this.view.getExternalPhotoDir(), new Compressor(this.view.getContext()), file.getName()));
        } catch (IOException e2) {
            e2.printStackTrace();
            return Observable.error(new Throwable(this.view.getContext().getString(R.string.p_record_photo_error_compress)));
        }
    }

    private Observable Y(Record record) {
        String lastUsedPhoto = Data.getLastUsedPhoto();
        if ("".equals(lastUsedPhoto)) {
            return Observable.error(new Throwable(this.view.getContext().getString(R.string.p_record_photo_error_no_last)));
        }
        return Data.setPhotoForRecord(record, lastUsedPhoto);
    }

    public /* synthetic */ ObservableSource y(int index, Boolean bool) {
        if (bool.booleanValue()) {
            return this.view.showConfirmDialog(R.string.p_record_delete_title_reconfirm, R.string.p_record_delete_content_reconfirm);
        }
        this.view.makeToast(R.string.p_record_delete_canceled, 0, new Object[0]);
        this.view.getRecordCardAdapter().notifyItemChanged(index);
        this.disposable.dispose();
        return Observable.just(Boolean.FALSE);
    }

    public /* synthetic */ ObservableSource z(int index, int index2, Boolean bool) {
        if (bool.booleanValue()) {
            return Data.deleteRecordById(index);
        }
        this.view.makeToast(R.string.p_record_delete_canceled, 0, new Object[0]);
        this.view.getRecordCardAdapter().notifyItemChanged(index2);
        this.disposable.dispose();
        return Observable.just(Boolean.FALSE);
    }

    public void handleIaaaLoginError(Throwable th) {
        if (!IaaaWrapper.RESULT_CANCEL.equals(th.getMessage())) {
            this.view.makeToast(R.string.p_record_error_other, 0, th.getLocalizedMessage());
        }
        this.view.dismissWaitDialog();
    }

    @Override
    public void showRecordDetail(final int index) {
        final Record singleRecord = Data.getSingleRecord(index);
        if (singleRecord == null) {
            this.view.makeToast(R.string.p_record_detail_error, 1, new Object[0]);
            return;
        }
        this.view.showWaitingDialog();
        this.view.setWaitingDialogMessage(R.string.p_record_waiting_message_after_sdk);
        Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                RecordListPresenter.L(singleRecord, index, observableEmitter);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.M((Record) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.N((Throwable) obj);
            }
        });
    }

    @Override
    public void syncData() {
        if (Data.getUser().isOffline().booleanValue()) {
            this.view.cancelRefresh();
        } else {
            Data.getRecordsFromServer().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    RecordListPresenter.this.O((ArrayList) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    RecordListPresenter.this.Q((Throwable) obj);
                }
            });
        }
    }

    @Override
    public void uploadVerifyRecord(final int index, final int index2) {
        final Record singleRecord = Data.getSingleRecord(index);
        this.view.showPhotoDialog(R.string.p_record_prepare_take_photo_title, R.string.p_record_prepare_take_photo_message).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource S;
                S = RecordListPresenter.this.S(singleRecord, (RecordListContract.View.PhotoStatus) obj);
                return S;
            }
        }).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource T;
                T = RecordListPresenter.this.T((Boolean) obj);
                return T;
            }
        }).delay(1500L, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource U;
                U = RecordListPresenter.this.U((Boolean) obj);
                return U;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.V((Pair) obj, index, index2);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                RecordListPresenter.this.handleIaaaLoginError((Throwable) obj);
            }
        });
    }
}
