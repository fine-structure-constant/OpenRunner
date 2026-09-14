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

    private static User currentUser;

    private static UserStatus userStatus;

    private static ArrayList partialRecords = new ArrayList();

    private static boolean initialized = false;

    private static final Function FILES_DIR_MAPPER = new Function() {
        @Override
        public final Object apply(Object obj) {
            return ((Context) obj).getFilesDir();
        }
    };

    private static DbManager.DaoConfig daoConfig;

    private static DbManager dbManager;

    private static SharedPreferences userPreferences;

    private static File filesDir;

    private static SharedPreferences photoPreferences;

    private static SharedPreferences speedUnitPreferences;

    public static /* synthetic */ void M(ObservableEmitter observableEmitter, Boolean bool) {
        initialized = true;
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ ObservableSource a0(Record record, int index, Record record2) {
        record2.setDetailed(true);
        record2.setTrack(record.getTrack());
        record2.setId(index);
        currentUser.setRecordById(index, record2);
        try {
            dbManager.update(record2, new String[0]);
            return Observable.just(record2);
        } catch (DbException e2) {
            e2.printStackTrace();
            return Observable.error(new DataException(4, e2.getMessage()));
        }
    }

    public static ArrayList<Task> getTasks() {
        return partialRecords;
    }

    public static User getUser() {
        return currentUser;
    }

    public static UserStatus getUserStatus() {
        return userStatus;
    }

    public static boolean isValid() {
        return initialized;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    public static void setUserStatus(UserStatus userStatus) {
        Data.userStatus = userStatus;
    }

    public static void setValid(boolean z2) {
        initialized = z2;
    }

    private static void A() {
        try {
            dbManager.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        dbManager = null;
    }

    public static /* synthetic */ void B(String str, ObservableEmitter observableEmitter) {
        currentUser.setToken(str);
        try {
            dbManager.update(currentUser, new String[0]);
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(2, e2.getMessage()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ void C(int index, ObservableEmitter observableEmitter) {
        GymRecord gymRecordById = currentUser.getGymRecordById(index);
        if (gymRecordById == null) {
            observableEmitter.onError(new DataException(32, "Cannot find GymRecord with id " + index));
            return;
        }
        if (gymRecordById.isUploaded().booleanValue()) {
            observableEmitter.onNext(Boolean.FALSE);
        }
        try {
            dbManager.delete(gymRecordById);
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(8, "Delete gymRecord failed", e2));
        }
        if (currentUser.deleteGymRecordById(index).booleanValue()) {
            observableEmitter.onNext(Boolean.TRUE);
        }
    }

    public static /* synthetic */ void D(int index, ObservableEmitter observableEmitter) {
        Record recordById = currentUser.getRecordById(index);
        if (recordById == null) {
            observableEmitter.onError(new DataException(32, "Cannot find Record with id " + index));
            return;
        }
        if (recordById.isUploaded()) {
            observableEmitter.onError(new DataException(64, "记录已上传！"));
        }
        try {
            dbManager.delete(recordById);
            dbManager.delete(Point.class, WhereBuilder.b("recordDbId", "=", Integer.valueOf(index)));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(8, "Delete record failed", e2));
        }
        if (currentUser.deleteRecord(recordById).booleanValue()) {
            observableEmitter.onNext(Boolean.TRUE);
        }
    }

    public static /* synthetic */ ObservableSource E(ArrayList arrayList) {
        try {
            dbManager.delete(GymRecord.class, WhereBuilder.b("userId", "=", currentUser.getId()));
            dbManager.saveBindingId(arrayList);
            currentUser.setGymRecords(arrayList);
            return Observable.just(arrayList);
        } catch (DbException e2) {
            e2.printStackTrace();
            return Observable.error(e2);
        }
    }

    public static /* synthetic */ ObservableSource G(ArrayList arrayList) {
        ArrayList<Record> records = currentUser.getRecords();
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
                    dbManager.saveBindingId(record);
                    if (record.isDetailed()) {
                        Iterator<Point> it3 = record.getTrack().iterator();
                        while (it3.hasNext()) {
                            Point next = it3.next();
                            next.setRecordDbId(record.getId());
                            dbManager.saveBindingId(next);
                        }
                    }
                    currentUser.addRecord(record);
                }
            }
            return Observable.just(arrayList);
        } catch (DbException e2) {
            e2.printStackTrace();
            LogUtil.e(e2.toString());
            return Observable.error(new DataException(2, "Cannot write to database", e2));
        }
    }

    public static /* synthetic */ void K(DbManager dbManager, int index, int index2) {
        switch (index) {
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
        LogUtil.e("Updated from " + index + " to " + index2);
    }

    public static /* synthetic */ void N(final ObservableEmitter observableEmitter) {
        if (currentUser == null) {
            observableEmitter.onNext(Boolean.FALSE);
            return;
        }
        Observable b02 = b0();
        Consumer consumer = new Consumer() {
            @Override
            public final void accept(Object obj) {
                Data.M(observableEmitter, (Boolean) obj);
            }
        };
        Objects.requireNonNull(observableEmitter);
        b02.subscribe(consumer, new EmitterErrorConsumer(observableEmitter));
    }

    public static /* synthetic */ void R(final ObservableEmitter observableEmitter) {
        try {
            List findAll = dbManager.selector(Record.class).where("userId", "=", currentUser.getId()).findAll();
            if (findAll != null) {
                ArrayList<Record> arrayList = new ArrayList<>(findAll);
                currentUser.setRecords(arrayList);
                Iterator<Record> it = arrayList.iterator();
                while (it.hasNext()) {
                    Record next = it.next();
                    List findAll2 = dbManager.selector(Point.class).where("recordDbId", "=", Integer.valueOf(next.getId())).findAll();
                    if (findAll2 != null) {
                        Collections.sort(findAll2, new Comparator() {
                            @Override
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
                currentUser.setRecords(new ArrayList<>());
            }
            List findAll3 = dbManager.selector(GymRecord.class).where("userId", "=", currentUser.getId()).findAll();
            if (findAll3 != null) {
                currentUser.setGymRecords(new ArrayList<>(findAll3));
            } else {
                currentUser.setGymRecords(new ArrayList<>());
            }
            List findAll4 = dbManager.selector(PartialRecord.class).findAll();
            if (findAll4 == null || findAll4.size() == 0) {
                observableEmitter.onNext(Boolean.TRUE);
                return;
            }
            Collections.sort(findAll4, new Comparator() {
                @Override
                public final int compare(Object obj, Object obj2) {
                    int O;
                    O = Data.O((PartialRecord) obj, (PartialRecord) obj2);
                    return O;
                }
            });
            int saveRecordToDatabase = saveRecordToDatabase(((PartialRecord) findAll4.get(findAll4.size() - 1)).toRecord(currentUser.getId()));
            ArrayList arrayList2 = new ArrayList();
            Iterator it2 = findAll4.iterator();
            while (it2.hasNext()) {
                List findAll5 = dbManager.selector(PartialPoint.class).where("recordDbId", "=", Integer.valueOf(((PartialRecord) it2.next()).getId())).findAll();
                Collections.sort(findAll5, new Comparator() {
                    @Override
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
            Consumer<? super Boolean> consumer = new Consumer() {
                @Override
                public final void accept(Object obj) {
                    Data.Q(observableEmitter, (Boolean) obj);
                }
            };
            Objects.requireNonNull(observableEmitter);
            provideTrackForRecord.subscribe(consumer, new EmitterErrorConsumer(observableEmitter));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(1, e2.getMessage()));
        }
    }

    public static /* synthetic */ ObservableSource T(User user) {
        user.setRecords(currentUser.getRecords());
        user.setGymRecords(currentUser.getGymRecords());
        currentUser = user;
        saveCurrentUserIdToFile();
        try {
            dbManager.saveOrUpdate(currentUser);
            return Observable.just(Boolean.TRUE);
        } catch (DbException e2) {
            return Observable.error(e2);
        }
    }

    public static /* synthetic */ void V(int index, ArrayList arrayList, ObservableEmitter observableEmitter) {
        if (!currentUser.provideTrackForRecord(index, arrayList).booleanValue()) {
            observableEmitter.onError(new DataException(32, "Cannot find Record with id " + index));
        }
        try {
            Record recordById = currentUser.getRecordById(index);
            dbManager.update(recordById, new String[0]);
            dbManager.saveBindingId(recordById.getTrack());
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(6, "Update record/Save track failed!" + e2.toString()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ ObservableSource W(UserStatus userStatus) {
        Data.userStatus = userStatus;
        return Observable.just(userStatus);
    }

    public static int addGymRecord(GymRecord gymRecord) throws DataException {
        try {
            dbManager.saveBindingId(gymRecord);
            return currentUser.addGymRecord(gymRecord);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add record failed!", e2);
        }
    }

    private static Observable b0() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.R(observableEmitter);
            }
        });
    }

    public static Observable<Boolean> changeUserToken(final String str) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.B(str, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void clearPartialData() throws DataException {
        try {
            dbManager.delete(PartialRecord.class);
            dbManager.delete(PartialPoint.class);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(8, e2.getMessage());
        }
    }

    private static void d0() {
        try {
            dbManager = org.xutils.x.getDb(daoConfig);
        } catch (DbException e) {
            throw new DataException(1, e.getMessage(), e);
        }
    }

    public static Observable<Boolean> deleteGymRecordById(final int index) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.C(index, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> deleteRecordById(final int index) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.D(index, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    private static void e0(String str) {
        SharedPreferences.Editor edit = photoPreferences.edit();
        StringBuilder sb = new StringBuilder();
        sb.append("lastUsed");
        User user = currentUser;
        sb.append(user != null ? user.getId() : "");
        edit.putString(sb.toString(), str).commit();
    }

    public static String getCurrentUserIdFromFile() {
        return userPreferences.getString("id", null);
    }

    public static List<User> getDatabaseUsers() throws DataException {
        try {
            return dbManager.findAll(User.class);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static ArrayList<GymRecord> getGymRecords() {
        return currentUser.getGymRecords();
    }

    public static String getLastUsedPhoto() {
        if (currentUser != null && photoPreferences.getString("lastUsed", null) != null) {
            photoPreferences.edit().putString("lastUsed" + currentUser.getId(), photoPreferences.getString("lastUsed", null)).remove("lastUsed").commit();
        }
        SharedPreferences sharedPreferences = photoPreferences;
        StringBuilder sb = new StringBuilder();
        sb.append("lastUsed");
        User user = currentUser;
        sb.append(user != null ? user.getId() : "");
        return sharedPreferences.getString(sb.toString(), "");
    }

    public static ArrayList<Record> getRecords() {
        return currentUser.getRecords();
    }

    public static Observable<ArrayList<Record>> getRecordsFromServer() {
        return Network.getRecords(currentUser.getId()).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource G;
                G = Data.G((ArrayList) obj);
                return G;
            }
        });
    }

    public static Record getSingleRecord(int index) {
        return currentUser.getRecordById(index);
    }

    public static Observable<Record> getSingleRecordFromServer(final int index) {
        Record recordById = currentUser.getRecordById(index);
        return recordById.isUploaded() ? Network.getSingleRecord(currentUser.getId(), recordById.getRecordId()).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource H;
                H = Data.H(index, (Record) obj);
                return H;
            }
        }) : Observable.error(new DataException(16));
    }

    public static SpeedHelper.SPEED_UNIT getSpeedUnitPreference() {
        if (!speedUnitPreferences.contains("unit")) {
            return SpeedHelper.SPEED_UNIT.MinutePerKilometer;
        }
        int index = speedUnitPreferences.getInt("unit", 0);
        SpeedHelper.SPEED_UNIT[] values = SpeedHelper.SPEED_UNIT.values();
        return (index < 0 || index >= values.length) ? SpeedHelper.SPEED_UNIT.KilometerPerHour : values[index];
    }

    public static Observable<Boolean> init(final Context context) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.L(context, observableEmitter);
            }
        });
    }

    public static Observable<Boolean> loadByUser() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.N(observableEmitter);
            }
        });
    }

    public static void loadSpecificUser(String str) throws DataException {
        try {
            currentUser = (User) dbManager.findById(User.class, str);
            saveCurrentUserIdToFile();
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static Observable<Boolean> login() {
        return Network.loginNew(currentUser.getToken()).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource T;
                T = Data.T((User) obj);
                return T;
            }
        });
    }

    public static Observable<Boolean> provideTrackForPartialRecord(final int index, final List<Point> list) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.U(index, list, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> provideTrackForRecord(final int index, final ArrayList<Point> arrayList) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.V(index, arrayList, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void saveCurrentUserIdToFile() {
        if (currentUser == null) {
            userPreferences.edit().putString("id", null).apply();
        } else {
            userPreferences.edit().putString("id", currentUser.getId()).apply();
        }
    }

    public static int savePartialRecordToDatabase(PartialRecord partialRecord) throws DataException {
        try {
            dbManager.saveBindingId(partialRecord);
            return partialRecord.getId();
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add partial record failed!", e2);
        }
    }

    public static int saveRecordToDatabase(Record record) throws DataException {
        try {
            dbManager.saveBindingId(record);
            return currentUser.addRecord(record);
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(2, "Add record failed!", e2);
        }
    }

    public static Observable<Boolean> saveUserToDatabase() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.X(observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> setPhotoForRecord(final Record record, final String str) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                Data.Y(record, str, observableEmitter);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void setSpeedUnitPreference(SpeedHelper.SPEED_UNIT speed_unit) {
        speedUnitPreferences.edit().putInt("unit", speed_unit.ordinal()).commit();
    }

    public static Observable<Integer> uploadGymRecordGetOut(final int index, String str) {
        return Network.uploadGymRecordGetOut(currentUser.getGymRecordById(index).getRecordId(), str).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource Z;
                Z = Data.Z(index, (GymRecord) obj);
                return Z;
            }
        });
    }

    public static Observable<Record> uploadRecordToServer(final int index) {
        final Record recordById = currentUser.getRecordById(index);
        String photoName = recordById.getPhotoName();
        return Network.uploadRecord(recordById, (photoName == null || "".equals(photoName)) ? null : new File(PhotoFile.getCompressedPhotoDir(filesDir), photoName)).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource a02;
                a02 = Data.a0(recordById, index, (Record) obj);
                return a02;
            }
        });
    }

    public static /* synthetic */ ObservableSource F(Record record, String str) {
        record.setPlaceHint(str);
        try {
            dbManager.update(record, new String[0]);
            return Observable.just(str);
        } catch (DbException e2) {
            return Observable.error(e2);
        }
    }

    public static /* synthetic */ ObservableSource H(int index, Record record) {
        record.setId(index);
        currentUser.setRecordById(index, record);
        if (record.isDetailed()) {
            Iterator<Point> it = record.getTrack().iterator();
            while (it.hasNext()) {
                it.next().setRecordDbId(index);
            }
        }
        try {
            dbManager.update(record, new String[0]);
            dbManager.delete(Point.class, WhereBuilder.b("recordDbId", "=", Integer.valueOf(index)));
            if (record.getTrack() != null) {
                dbManager.saveBindingId(record.getTrack());
            }
            return Observable.just(record);
        } catch (DbException e2) {
            return Observable.error(new DataException(4, "Cannot update record and track", e2));
        }
    }

    public static /* synthetic */ void J(DbManager dbManager) {
        dbManager.getDatabase().enableWriteAheadLogging();
    }

    public static /* synthetic */ void L(Context context, ObservableEmitter observableEmitter) {
        Context applicationContext = context.getApplicationContext();
        photoPreferences = applicationContext.getSharedPreferences("photo", 0);
        userPreferences = applicationContext.getSharedPreferences("user", 0);
        speedUnitPreferences = applicationContext.getSharedPreferences("speed-unit", 0);
        filesDir = context.getExternalFilesDir(PhotoFile.PicutreType);
        try {
            daoConfig = new DbManager.DaoConfig().setDbName("data.db").setDbDir((File) FILES_DIR_MAPPER.apply(context)).setDbVersion(8).setDbOpenListener(new DbManager.DbOpenListener() {
            @Override
            public final void onDbOpened(DbManager dbManager) {
                Data.J(dbManager);
            }
            }).setDbUpgradeListener(new DbManager.DbUpgradeListener() {
            @Override
            public final void onUpgrade(DbManager dbManager, int index, int index2) {
                Data.K(dbManager, index, index2);
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

    public static /* synthetic */ int O(PartialRecord partialRecord, PartialRecord partialRecord2) {
        return partialRecord.getDate().compareTo(partialRecord2.getDate());
    }

    public static /* synthetic */ int P(PartialPoint partialPoint, PartialPoint partialPoint2) {
        return partialPoint.getSequence() - partialPoint2.getSequence();
    }

    public static /* synthetic */ void Q(ObservableEmitter observableEmitter, Boolean bool) {
        clearPartialData();
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ int S(Point point, Point point2) {
        return point.getSequence() - point2.getSequence();
    }

    public static /* synthetic */ void U(int index, List list, ObservableEmitter observableEmitter) {
        try {
            dbManager.saveBindingId(PartialPoint.assignInfoToTrack(index, list));
        } catch (DbException e2) {
            e2.printStackTrace();
            observableEmitter.onError(new DataException(2, "Save partialTrack failed!" + e2.toString()));
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ void X(ObservableEmitter observableEmitter) {
        saveCurrentUserIdToFile();
        try {
            dbManager.saveOrUpdate(currentUser);
        } catch (DbException e2) {
            observableEmitter.onError(e2);
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ void Y(Record record, String str, ObservableEmitter observableEmitter) {
        record.setPhotoName(str);
        e0(str);
        try {
            dbManager.update(record, new String[0]);
        } catch (DbException e2) {
            observableEmitter.onError(e2);
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ ObservableSource Z(int index, GymRecord gymRecord) {
        gymRecord.setId(index);
        currentUser.setGymRecordById(index, gymRecord);
        try {
            dbManager.update(gymRecord, new String[0]);
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
                currentUser = (User) dbManager.findById(User.class, currentUserIdFromFile);
                return;
            }
            currentUser = null;
        } catch (DbException e2) {
            e2.printStackTrace();
            throw new DataException(1, e2.getMessage());
        }
    }

    public static Observable<ArrayList<GymRecord>> getGymRecordsFromServer() {
        return Network.getGymRecords().observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
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
        return Network.getReverseEncoding(record.getTrack().get(0)).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource F;
                F = Data.F(record, (String) obj);
                return F;
            }
        });
    }

    public static Observable<ArrayList<Task>> getTasksFromServer() {
        return Network.getTasks().observeOn(Schedulers.io()).doOnNext(new Consumer() {
            @Override
            public final void accept(Object obj) {
                Data.partialRecords = (ArrayList) obj;
            }
        });
    }

    public static Observable<UserStatus> refreshUserStatus() {
        return Network.getUserStatus().observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
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
