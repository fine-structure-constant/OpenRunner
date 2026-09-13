package cn.edu.pku.pkurunner;

import android.content.Context;
import android.content.SharedPreferences;
import cn.edu.pku.pkurunner.Exception.DataException;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import cn.edu.pku.pkurunner.Model.GymRecord;
import cn.edu.pku.pkurunner.Model.PartialPoint;
import cn.edu.pku.pkurunner.Model.PartialRecord;
import cn.edu.pku.pkurunner.Model.Point;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Model.Task;
import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Photo.PhotoFile;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;

public class Data {

    /* renamed from: a, reason: collision with root package name */
    private static User f6851a;

    /* renamed from: b, reason: collision with root package name */
    private static UserStatus f6852b;

    /* renamed from: c, reason: collision with root package name */
    private static ArrayList f6853c = new ArrayList();

    /* renamed from: d, reason: collision with root package name */
    private static boolean f6854d = false;

    /* renamed from: e, reason: collision with root package name */
    private static final Function f6855e = new Function() { // from class: cn.edu.pku.pkurunner.z
        @Override // io.reactivex.functions.Function
        public final Object apply(Object obj) {
            return ((Context) obj).getFilesDir();
        }
    };

    /* renamed from: f, reason: collision with root package name */
    private static DbManager.DaoConfig f6856f;

    /* renamed from: g, reason: collision with root package name */
    private static DbManager f6857g;

    /* renamed from: h, reason: collision with root package name */
    private static SharedPreferences f6858h;

    /* renamed from: i, reason: collision with root package name */
    private static File f6859i;

    /* renamed from: j, reason: collision with root package name */
    private static SharedPreferences f6860j;

    /* renamed from: k, reason: collision with root package name */
    private static SharedPreferences f6861k;

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void M(ObservableEmitter observableEmitter, Boolean bool) {
        f6854d = true;
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource a0(Record record, int i2, Record record2) {
        record2.setDetailed(true);
        record2.setTrack(record.getTrack());
        record2.setId(i2);
        f6851a.setRecordById(i2, record2);
        try {
            f6857g.update(record2, new String[0]);
            return Observable.just(record2);
        } catch (DbException e2) {
            e2.printStackTrace();
            return Observable.error(new DataException(4, e2.getMessage()));
        }
    }

    public static ArrayList<Task> getTasks() {
        return f6853c;
    }

    public static User getUser() {
        return f6851a;
    }

    public static UserStatus getUserStatus() {
        return f6852b;
    }

    public static boolean isValid() {
        return f6854d;
    }

    public static void setUser(User user) {
        f6851a = user;
    }

    public static void setUserStatus(UserStatus userStatus) {
        f6852b = userStatus;
    }

    public static void setValid(boolean z2) {
        f6854d = z2;
    }

    private static void A() {
        try {
            f6857g.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        f6857g = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void B(String str, ObservableEmitter observableEmitter) {
        f6851a.setToken(str);
        try {
            f6857g.update(f6851a, new String[0]);
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(2, e2.getMessage()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void C(int i2, ObservableEmitter observableEmitter) {
        GymRecord gymRecordById = f6851a.getGymRecordById(i2);
        if (gymRecordById == null) {
            observableEmitter.onError(new DataException(32, "Cannot find GymRecord with id " + i2));
            return;
        }
        if (gymRecordById.isUploaded().booleanValue()) {
            observableEmitter.onNext(Boolean.FALSE);
        }
        try {
            f6857g.delete(gymRecordById);
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(8, "Delete gymRecord failed", e2));
        }
        if (f6851a.deleteGymRecordById(i2).booleanValue()) {
            observableEmitter.onNext(Boolean.TRUE);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void D(int i2, ObservableEmitter observableEmitter) {
        Record recordById = f6851a.getRecordById(i2);
        if (recordById == null) {
            observableEmitter.onError(new DataException(32, "Cannot find Record with id " + i2));
            return;
        }
        if (recordById.isUploaded()) {
            observableEmitter.onError(new DataException(64, "记录已上传！"));
        }
        try {
            f6857g.delete(recordById);
            f6857g.delete(Point.class, WhereBuilder.b("recordDbId", "=", Integer.valueOf(i2)));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(8, "Delete record failed", e2));
        }
        if (f6851a.deleteRecord(recordById).booleanValue()) {
            observableEmitter.onNext(Boolean.TRUE);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource E(ArrayList arrayList) {
        try {
            f6857g.delete(GymRecord.class, WhereBuilder.b("userId", "=", f6851a.getId()));
            f6857g.saveBindingId(arrayList);
            f6851a.setGymRecords(arrayList);
            return Observable.just(arrayList);
        } catch (DbException e2) {
            e2.printStackTrace();
            return Observable.error(e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource G(ArrayList arrayList) {
        ArrayList<Record> records = f6851a.getRecords();
        HashSet hashSet = new HashSet();
        Iterator<Record> it = records.iterator();
        while (it.hasNext()) {
            hashSet.add(Integer.valueOf(it.next().getRecordId()));
        }
        try {
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                Record record = (Record) it2.next();
                if (!hashSet.contains(Integer.valueOf(record.getRecordId()))) {
                    f6857g.saveBindingId(record);
                    if (record.isDetailed()) {
                        Iterator<Point> it3 = record.getTrack().iterator();
                        while (it3.hasNext()) {
                            Point next = it3.next();
                            next.setRecordDbId(record.getId());
                            f6857g.saveBindingId(next);
                        }
                    }
                    f6851a.addRecord(record);
                }
            }
            return Observable.just(arrayList);
        } catch (DbException e2) {
            e2.printStackTrace();
            LogUtil.e(e2.toString());
            return Observable.error(new DataException(2, "Cannot write to database", e2));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void K(DbManager dbManager, int i2, int i3) {
        switch (i2) {
            case 1:
            case 2:
            case 3:
                LogUtil.d("Added photoName and photoRemotePath.");
                try {
                    dbManager.addColumn(Record.class, "photoName");
                    dbManager.addColumn(Record.class, "photoRemotePath");
                } catch (DbException e2) {
                    e2.printStackTrace();
                }
            case 4:
                LogUtil.d("Added PartialRecord and PartialPoint.");
            case 5:
                LogUtil.d("Added offline property to user.");
                try {
                    dbManager.addColumn(User.class, "offline");
                } catch (DbException e3) {
                    e3.printStackTrace();
                }
            case 6:
                LogUtil.d("Added placeHint to record.");
                try {
                    dbManager.addColumn(Record.class, "placeHint");
                } catch (DbException e4) {
                    e4.printStackTrace();
                }
            case 7:
                LogUtil.d("Added extra to record.");
                try {
                    dbManager.addColumn(Record.class, "extra");
                    break;
                } catch (DbException e5) {
                    e5.printStackTrace();
                    break;
                }
        }
        LogUtil.e("Updated from " + i2 + " to " + i3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void N(final ObservableEmitter observableEmitter) {
        if (f6851a == null) {
            observableEmitter.onNext(Boolean.FALSE);
            return;
        }
        Observable b02 = b0();
        Consumer consumer = new Consumer() { // from class: cn.edu.pku.pkurunner.k
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                Data.M(observableEmitter, (Boolean) obj);
            }
        };
        Objects.requireNonNull(observableEmitter);
        b02.subscribe(consumer, new m(observableEmitter));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void R(final ObservableEmitter observableEmitter) {
        try {
            List findAll = f6857g.selector(Record.class).where("userId", "=", f6851a.getId()).findAll();
            if (findAll != null) {
                ArrayList<Record> arrayList = new ArrayList<>(findAll);
                f6851a.setRecords(arrayList);
                Iterator<Record> it = arrayList.iterator();
                while (it.hasNext()) {
                    Record next = it.next();
                    List findAll2 = f6857g.selector(Point.class).where("recordDbId", "=", Integer.valueOf(next.getId())).findAll();
                    if (findAll2 != null) {
                        Collections.sort(findAll2, new Comparator() { // from class: cn.edu.pku.pkurunner.q
                            @Override // java.util.Comparator
                            public final int compare(Object obj, Object obj2) {
                                int S;
                                S = Data.S((Point) obj, (Point) obj2);
                                return S;
                            }
                        });
                        next.setTrack(new ArrayList<>(findAll2));
                    }
                }
            } else {
                f6851a.setRecords(new ArrayList<>());
            }
            List findAll3 = f6857g.selector(GymRecord.class).where("userId", "=", f6851a.getId()).findAll();
            if (findAll3 != null) {
                f6851a.setGymRecords(new ArrayList<>(findAll3));
            } else {
                f6851a.setGymRecords(new ArrayList<>());
            }
            List findAll4 = f6857g.selector(PartialRecord.class).findAll();
            if (findAll4 == null || findAll4.size() == 0) {
                observableEmitter.onNext(Boolean.TRUE);
                return;
            }
            Collections.sort(findAll4, new Comparator() { // from class: cn.edu.pku.pkurunner.s
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    int O;
                    O = Data.O((PartialRecord) obj, (PartialRecord) obj2);
                    return O;
                }
            });
            int saveRecordToDatabase = saveRecordToDatabase(((PartialRecord) findAll4.get(findAll4.size() - 1)).toRecord(f6851a.getId()));
            ArrayList arrayList2 = new ArrayList();
            Iterator it2 = findAll4.iterator();
            while (it2.hasNext()) {
                List findAll5 = f6857g.selector(PartialPoint.class).where("recordDbId", "=", Integer.valueOf(((PartialRecord) it2.next()).getId())).findAll();
                Collections.sort(findAll5, new Comparator() { // from class: cn.edu.pku.pkurunner.t
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        int P;
                        P = Data.P((PartialPoint) obj, (PartialPoint) obj2);
                        return P;
                    }
                });
                Iterator it3 = findAll5.iterator();
                while (it3.hasNext()) {
                    arrayList2.add(((PartialPoint) it3.next()).toPoint());
                }
            }
            Observable<Boolean> provideTrackForRecord = provideTrackForRecord(saveRecordToDatabase, arrayList2);
            Consumer<? super Boolean> consumer = new Consumer() { // from class: cn.edu.pku.pkurunner.u
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    Data.Q(observableEmitter, (Boolean) obj);
                }
            };
            Objects.requireNonNull(observableEmitter);
            provideTrackForRecord.subscribe(consumer, new m(observableEmitter));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(1, e2.getMessage()));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource T(User user) {
        user.setRecords(f6851a.getRecords());
        user.setGymRecords(f6851a.getGymRecords());
        f6851a = user;
        saveCurrentUserIdToFile();
        try {
            f6857g.saveOrUpdate(f6851a);
            return Observable.just(Boolean.TRUE);
        } catch (DbException e2) {
            return Observable.error(e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void V(int i2, ArrayList arrayList, ObservableEmitter observableEmitter) {
        if (!f6851a.provideTrackForRecord(i2, arrayList).booleanValue()) {
            observableEmitter.onError(new DataException(32, "Cannot find Record with id " + i2));
        }
        try {
            Record recordById = f6851a.getRecordById(i2);
            f6857g.update(recordById, new String[0]);
            f6857g.saveBindingId(recordById.getTrack());
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(6, "Update record/Save track failed!" + e2.toString()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource W(UserStatus userStatus) {
        f6852b = userStatus;
        return Observable.just(userStatus);
    }

    public static int addGymRecord(GymRecord gymRecord) throws DataException {
        try {
            f6857g.saveBindingId(gymRecord);
            return f6851a.addGymRecord(gymRecord);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add record failed!", e2);
        }
    }

    private static Observable b0() {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.p
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.R(observableEmitter);
            }
        });
    }

    public static Observable<Boolean> changeUserToken(final String str) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.g
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.B(str, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void clearPartialData() throws DataException {
        try {
            f6857g.delete(PartialRecord.class);
            f6857g.delete(PartialPoint.class);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(8, e2.getMessage());
        }
    }

    private static void d0() {
        try {
            f6857g = org.xutils.x.getDb(f6856f);
        } catch (DbException e) {
            throw new DataException(1, e.getMessage(), e);
        }
    }

    public static Observable<Boolean> deleteGymRecordById(final int i2) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.h
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.C(i2, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> deleteRecordById(final int i2) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.v
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.D(i2, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    private static void e0(String str) {
        SharedPreferences.Editor edit = f6860j.edit();
        StringBuilder sb = new StringBuilder();
        sb.append("lastUsed");
        User user = f6851a;
        sb.append(user != null ? user.getId() : "");
        edit.putString(sb.toString(), str).commit();
    }

    public static String getCurrentUserIdFromFile() {
        return f6858h.getString("id", null);
    }

    public static List<User> getDatabaseUsers() throws DataException {
        try {
            return f6857g.findAll(User.class);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static ArrayList<GymRecord> getGymRecords() {
        return f6851a.getGymRecords();
    }

    public static String getLastUsedPhoto() {
        if (f6851a != null && f6860j.getString("lastUsed", null) != null) {
            f6860j.edit().putString("lastUsed" + f6851a.getId(), f6860j.getString("lastUsed", null)).remove("lastUsed").commit();
        }
        SharedPreferences sharedPreferences = f6860j;
        StringBuilder sb = new StringBuilder();
        sb.append("lastUsed");
        User user = f6851a;
        sb.append(user != null ? user.getId() : "");
        return sharedPreferences.getString(sb.toString(), "");
    }

    public static ArrayList<Record> getRecords() {
        return f6851a.getRecords();
    }

    public static Observable<ArrayList<Record>> getRecordsFromServer() {
        return Network.getRecords(f6851a.getId()).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.w
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource G;
                G = Data.G((ArrayList) obj);
                return G;
            }
        });
    }

    public static Record getSingleRecord(int i2) {
        return f6851a.getRecordById(i2);
    }

    public static Observable<Record> getSingleRecordFromServer(final int i2) {
        Record recordById = f6851a.getRecordById(i2);
        return recordById.isUploaded() ? Network.getSingleRecord(f6851a.getId(), recordById.getRecordId()).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.x
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource H;
                H = Data.H(i2, (Record) obj);
                return H;
            }
        }) : Observable.error(new DataException(16));
    }

    public static SpeedHelper.SPEED_UNIT getSpeedUnitPreference() {
        if (!f6861k.contains("unit")) {
            return SpeedHelper.SPEED_UNIT.MinutePerKilometer;
        }
        int i2 = f6861k.getInt("unit", 0);
        SpeedHelper.SPEED_UNIT[] values = SpeedHelper.SPEED_UNIT.values();
        return (i2 < 0 || i2 >= values.length) ? SpeedHelper.SPEED_UNIT.KilometerPerHour : values[i2];
    }

    public static Observable<Boolean> init(final Context context) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.l
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.L(context, observableEmitter);
            }
        });
    }

    public static Observable<Boolean> loadByUser() {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.a0
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.N(observableEmitter);
            }
        });
    }

    public static void loadSpecificUser(String str) throws DataException {
        try {
            f6851a = (User) f6857g.findById(User.class, str);
            saveCurrentUserIdToFile();
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static Observable<Boolean> login() {
        return Network.loginNew(f6851a.getToken()).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.i
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource T;
                T = Data.T((User) obj);
                return T;
            }
        });
    }

    public static Observable<Boolean> provideTrackForPartialRecord(final int i2, final List<Point> list) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.f
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.U(i2, list, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> provideTrackForRecord(final int i2, final ArrayList<Point> arrayList) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.a
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.V(i2, arrayList, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void saveCurrentUserIdToFile() {
        if (f6851a == null) {
            f6858h.edit().putString("id", null).apply();
        } else {
            f6858h.edit().putString("id", f6851a.getId()).apply();
        }
    }

    public static int savePartialRecordToDatabase(PartialRecord partialRecord) throws DataException {
        try {
            f6857g.saveBindingId(partialRecord);
            return partialRecord.getId();
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add partial record failed!", e2);
        }
    }

    public static int saveRecordToDatabase(Record record) throws DataException {
        try {
            f6857g.saveBindingId(record);
            return f6851a.addRecord(record);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add record failed!", e2);
        }
    }

    public static Observable<Boolean> saveUserToDatabase() {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.y
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.X(observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> setPhotoForRecord(final Record record, final String str) {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.c
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.Y(record, str, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void setSpeedUnitPreference(SpeedHelper.SPEED_UNIT speed_unit) {
        f6861k.edit().putInt("unit", speed_unit.ordinal()).commit();
    }

    public static Observable<Integer> uploadGymRecordGetOut(final int i2, String str) {
        return Network.uploadGymRecordGetOut(f6851a.getGymRecordById(i2).getRecordId(), str).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.c0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource Z;
                Z = Data.Z(i2, (GymRecord) obj);
                return Z;
            }
        });
    }

    public static Observable<Record> uploadRecordToServer(final int i2) {
        final Record recordById = f6851a.getRecordById(i2);
        String photoName = recordById.getPhotoName();
        return Network.uploadRecord(recordById, (photoName == null || "".equals(photoName)) ? null : new File(PhotoFile.getCompressedPhotoDir(f6859i), photoName)).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.e
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource a02;
                a02 = Data.a0(recordById, i2, (Record) obj);
                return a02;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource F(Record record, String str) {
        record.setPlaceHint(str);
        try {
            f6857g.update(record, new String[0]);
            return Observable.just(str);
        } catch (DbException e2) {
            return Observable.error(e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource H(int i2, Record record) {
        record.setId(i2);
        f6851a.setRecordById(i2, record);
        if (record.isDetailed()) {
            Iterator<Point> it = record.getTrack().iterator();
            while (it.hasNext()) {
                it.next().setRecordDbId(i2);
            }
        }
        try {
            f6857g.update(record, new String[0]);
            f6857g.delete(Point.class, WhereBuilder.b("recordDbId", "=", Integer.valueOf(i2)));
            if (record.getTrack() != null) {
                f6857g.saveBindingId(record.getTrack());
            }
            return Observable.just(record);
        } catch (DbException e2) {
            return Observable.error(new DataException(4, "Cannot update record and track", e2));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void J(DbManager dbManager) {
        dbManager.getDatabase().enableWriteAheadLogging();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void L(Context context, ObservableEmitter observableEmitter) {
        Context applicationContext = context.getApplicationContext();
        f6860j = applicationContext.getSharedPreferences("photo", 0);
        f6858h = applicationContext.getSharedPreferences("user", 0);
        f6861k = applicationContext.getSharedPreferences("speed-unit", 0);
        f6859i = context.getExternalFilesDir(PhotoFile.PicutreType);
        try {
            f6856f = new DbManager.DaoConfig().setDbName("data.db").setDbDir((File) f6855e.apply(context)).setDbVersion(8).setDbOpenListener(new DbManager.DbOpenListener() { // from class: cn.edu.pku.pkurunner.n
            @Override // org.xutils.DbManager.DbOpenListener
            public final void onDbOpened(DbManager dbManager) {
                Data.J(dbManager);
            }
            }).setDbUpgradeListener(new DbManager.DbUpgradeListener() { // from class: cn.edu.pku.pkurunner.o
            @Override // org.xutils.DbManager.DbUpgradeListener
            public final void onUpgrade(DbManager dbManager, int i2, int i3) {
                Data.K(dbManager, i2, i3);
            }
            });
        } catch (Exception e) {
            observableEmitter.onError(e);
            return;
        }
        d0();
        try {
            c0();
            setValid(false);
            observableEmitter.onNext(Boolean.TRUE);
        } catch (DataException e2) {
            observableEmitter.onError(e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int O(PartialRecord partialRecord, PartialRecord partialRecord2) {
        return partialRecord.getDate().compareTo(partialRecord2.getDate());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int P(PartialPoint partialPoint, PartialPoint partialPoint2) {
        return partialPoint.getSequence() - partialPoint2.getSequence();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void Q(ObservableEmitter observableEmitter, Boolean bool) {
        clearPartialData();
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int S(Point point, Point point2) {
        return point.getSequence() - point2.getSequence();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void U(int i2, List list, ObservableEmitter observableEmitter) {
        try {
            f6857g.saveBindingId(PartialPoint.assignInfoToTrack(i2, list));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(2, "Save partialTrack failed!" + e2.toString()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void X(ObservableEmitter observableEmitter) {
        saveCurrentUserIdToFile();
        try {
            f6857g.saveOrUpdate(f6851a);
        } catch (DbException e2) {
            observableEmitter.onError(e2);
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void Y(Record record, String str, ObservableEmitter observableEmitter) {
        record.setPhotoName(str);
        e0(str);
        try {
            f6857g.update(record, new String[0]);
        } catch (DbException e2) {
            observableEmitter.onError(e2);
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource Z(int i2, GymRecord gymRecord) {
        gymRecord.setId(i2);
        f6851a.setGymRecordById(i2, gymRecord);
        try {
            f6857g.update(gymRecord, new String[0]);
            return Observable.just(Integer.valueOf(gymRecord.getId()));
        } catch (DbException e2) {
            e2.printStackTrace();
            return Observable.error(new DataException(4, e2.getMessage()));
        }
    }

    private static void c0() {
        try {
            String currentUserIdFromFile = getCurrentUserIdFromFile();
            if (currentUserIdFromFile != null && !"".equals(currentUserIdFromFile)) {
                f6851a = (User) f6857g.findById(User.class, currentUserIdFromFile);
                return;
            }
            f6851a = null;
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static Observable<ArrayList<GymRecord>> getGymRecordsFromServer() {
        return Network.getGymRecords().observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.d
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource E;
                E = Data.E((ArrayList) obj);
                return E;
            }
        });
    }

    public static Observable<String> getRecordPlaceHintForOfflineUser(final Record record) {
        if (record.isPlaceHintAvailable()) {
            return Observable.just(record.getPlaceHint());
        }
        return Network.getReverseEncoding(record.getTrack().get(0)).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.b
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource F;
                F = Data.F(record, (String) obj);
                return F;
            }
        });
    }

    public static Observable<ArrayList<Task>> getTasksFromServer() {
        return Network.getTasks().observeOn(Schedulers.io()).doOnNext(new Consumer() { // from class: cn.edu.pku.pkurunner.j
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                Data.f6853c = (ArrayList) obj;
            }
        });
    }

    public static Observable<UserStatus> refreshUserStatus() {
        return Network.getUserStatus().observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.b0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource W;
                W = Data.W((UserStatus) obj);
                return W;
            }
        });
    }

    public static FileMetadata uploadDatabaseToDropbox(Context context, DbxClientV2 dbxClientV2) throws IOException, DbxException {
        File file = new File(context.getFilesDir(), "data.db");
        A();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            FileMetadata uploadAndFinish = dbxClientV2.files().uploadBuilder("/data.db").withMode(WriteMode.OVERWRITE).uploadAndFinish(fileInputStream);
            LogUtil.d(uploadAndFinish.toString());
            return uploadAndFinish;
        } finally {
            d0();
        }
    }
}
