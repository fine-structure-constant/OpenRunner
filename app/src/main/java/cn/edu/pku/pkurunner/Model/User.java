package cn.edu.pku.pkurunner.Model;

import cn.edu.pku.pkurunner.BuildConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "user")
public class User {
    public static final int FEMALE = 0;
    public static final String FEMALE_STRING = "女";
    public static final int MALE = 1;
    public static final String MALE_STRING = "男";
    public static final int UNKNOWN_GENDER = -1;
    public static final String UNKNOWN_GENDER_STRING = "未知";

    @Column(name = "PESpecialty")
    private Boolean PESpecialty;

    @Column(name = "department")
    private String department;

    @Column(name = "gender")
    private int gender;

    @Column(isId = true, name = "id", property = "UNIQUE")
    private String f6980id;

    @Column(name = "name")
    private String name;

    @Column(name = "token")
    private String token;

    @Column(name = "offline")
    private Boolean offline = Boolean.FALSE;
    private ArrayList<Record> records = new ArrayList<>();
    private ArrayList<GymRecord> gymRecords = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    public @interface GENDER {
    }

    public static class Inner {
        private String access_token;
        private String department;

        private String id;
        private Boolean isPESpecialty;
        private String name;
        private String password;
        private String sex;
    }

    public User() {
    }

    private String getGenderByString() {
        int index = this.gender;
        return index != 0 ? index != 1 ? UNKNOWN_GENDER_STRING : MALE_STRING : FEMALE_STRING;
    }

    public String getDepartment() {
        return this.department;
    }

    public int getGender() {
        return this.gender;
    }

    public ArrayList<GymRecord> getGymRecords() {
        return this.gymRecords;
    }

    public String getId() {
        return this.f6980id;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<Record> getRecords() {
        return this.records;
    }

    public String getToken() {
        return this.token;
    }

    public Boolean isOffline() {
        return this.offline;
    }

    public Boolean isPESpecialty() {
        return this.PESpecialty;
    }

    public void setDepartment(String str) {
        this.department = str;
    }

    public void setGender(int index) {
        this.gender = index;
    }

    public void setGymRecordById(int index, GymRecord gymRecord) {
        for (int index2 = 0; index2 < this.gymRecords.size(); index2++) {
            if (this.gymRecords.get(index2).getId() == index) {
                this.gymRecords.set(index, gymRecord);
            }
        }
    }

    public void setGymRecords(ArrayList<GymRecord> arrayList) {
        this.gymRecords = arrayList;
    }

    public void setId(String str) {
        this.f6980id = str;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setPESpecialty(Boolean bool) {
        this.PESpecialty = bool;
    }

    public void setRecordById(int index, Record record) {
        for (int index2 = 0; index2 < this.records.size(); index2++) {
            if (this.records.get(index2).getId() == index) {
                this.records.set(index2, record);
            }
        }
    }

    public void setRecords(ArrayList<Record> arrayList) {
        this.records = arrayList;
    }

    public void setToken(String str) {
        this.token = str;
    }

    public static User createOfflineUser(int index, String str) {
        User user = new User();
        user.offline = Boolean.TRUE;
        user.department = "离线";
        user.gender = index;
        user.f6980id = UUID.randomUUID().toString();
        user.name = str;
        user.PESpecialty = Boolean.FALSE;
        user.token = UUID.randomUUID().toString();
        return user;
    }

    public int addGymRecord(GymRecord gymRecord) {
        this.gymRecords.add(gymRecord);
        return gymRecord.getId();
    }

    public int addRecord(Record record) {
        this.records.add(record);
        return record.getId();
    }

    public Boolean deleteGymRecordById(int index) {
        Iterator<GymRecord> it = this.gymRecords.iterator();
        while (it.hasNext()) {
            GymRecord next = it.next();
            if (next.getId() == index) {
                return next.isUploaded().booleanValue() ? Boolean.FALSE : Boolean.valueOf(this.gymRecords.remove(next));
            }
        }
        return Boolean.FALSE;
    }

    public Boolean deleteRecord(Record record) {
        return Boolean.valueOf(this.records.remove(record));
    }

    public GymRecord getGymRecordById(int index) {
        Iterator<GymRecord> it = this.gymRecords.iterator();
        while (it.hasNext()) {
            GymRecord next = it.next();
            if (next.getId() == index) {
                return next;
            }
        }
        return null;
    }

    public Record getRecordById(int index) {
        Iterator<Record> it = this.records.iterator();
        while (it.hasNext()) {
            Record next = it.next();
            if (next.getId() == index) {
                return next;
            }
        }
        return null;
    }

    public String toString() {
        return "User{id='" + this.f6980id + "', token='" + this.token + "', PESpecialty=" + this.PESpecialty + ", name='" + this.name + "', gender=" + getGenderByString() + ", department=" + this.department + ", records=" + this.records + ", gymRecords=" + this.gymRecords + '}';
    }

    public Boolean provideTrackForRecord(int index, ArrayList<Point> arrayList) {
        Record recordById = getRecordById(index);
        if (recordById != null) {
            recordById.setTrack(Point.assignInfoToTrack(index, arrayList));
            recordById.setDetailed(true);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public User(Inner inner) {
        this.token = inner.access_token;
        this.PESpecialty = inner.isPESpecialty;
        this.department = inner.department;
        this.name = inner.name;
        this.f6980id = inner.id;
        String str = inner.sex;
        str.hashCode();
        if (str.equals(FEMALE_STRING)) {
            this.gender = 0;
        } else if (str.equals(MALE_STRING)) {
            this.gender = 1;
        } else {
            this.gender = -1;
        }
    }

    public User(String str, String str2) {
        this.f6980id = str;
        this.token = str2;
    }
}
