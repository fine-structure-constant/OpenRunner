package cn.edu.pku.pkurunner.Model;

import android.os.Parcel;
import android.os.Parcelable;
import cn.edu.pku.pkurunner.BuildConfig;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "record")
public class Record implements Serializable, Parcelable {

    @Column(name = "extra")
    private String checkField;

    @Column(name = "date")
    private Date date;

    @Column(name = "detailed")
    private boolean detailed;

    @Column(name = "distance")
    private int distance;

    @Column(name = "duration")
    private int duration;

    @Column(isId = true, name = "id", property = "UNIQUE")
    private int f6978id;

    @Column(name = "invalidReason")
    private int invalidReason;

    @Column(name = "photoName")
    private String photoName;

    @Column(name = "photoRemotePath")
    private String photoRemotePath;

    @Column(name = "placeHint")
    private String placeHint;

    @Column(name = "recordId")
    private int recordId;

    @Column(name = "step")
    private int step;
    private ArrayList<Point> track;

    @Column(name = "uploaded")
    private boolean uploaded;

    @Column(name = "userId")
    private String userId;

    @Column(name = "verified")
    private boolean verified;
    private static final DateFormat dateFormatter = DateFormat.getDateTimeInstance();
    public static final Parcelable.Creator<Record> CREATOR = new Parcelable.Creator<Record>() {
        /* JADX WARN: Can't rename method to resolve collision */
        @Override
        public Record createFromParcel(Parcel parcel) {
            return new Record(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override
        public Record[] newArray(int index) {
            return new Record[index];
        }
    };

    public static class Inner {
        private Date date;
        private boolean detailed;
        private int distance;
        private double duration;
        private int invalidReason;
        private boolean morningBonus;

        @SerializedName("photoPath")
        private String photoRemotePath;
        private int recordId;
        private int step;

        @SerializedName("detail")
        private double[][] track;
        private boolean uploaded;
        private String userId;
        private boolean verified;
    }

    public enum RecordPlace {
        WUSI,
        WEIMING,
        UNKNOWN
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getCheckField() {
        return this.checkField;
    }

    public Date getDate() {
        return this.date;
    }

    public int getDistance() {
        return this.distance;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getId() {
        return this.f6978id;
    }

    public int getInvalidReason() {
        return this.invalidReason;
    }

    public String getPhotoName() {
        return this.photoName;
    }

    public String getPhotoRemotePath() {
        return this.photoRemotePath;
    }

    public String getPlaceHint() {
        return this.placeHint;
    }

    public int getRecordId() {
        return this.recordId;
    }

    public int getStep() {
        return this.step;
    }

    public ArrayList<Point> getTrack() {
        return this.track;
    }

    public String getUserId() {
        return this.userId;
    }

    public boolean isDetailed() {
        return this.detailed;
    }

    public boolean isUploaded() {
        return this.uploaded;
    }

    public boolean isVerified() {
        return this.verified;
    }

    public void setCheckField(String str) {
        this.checkField = str;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDetailed(boolean z2) {
        this.detailed = z2;
    }

    public void setDistance(int index) {
        this.distance = index;
    }

    public void setDuration(int index) {
        this.duration = index;
    }

    public void setId(int index) {
        this.f6978id = index;
    }

    public void setInvalidReason(int index) {
        this.invalidReason = index;
    }

    public void setPhotoName(String str) {
        this.photoName = str;
    }

    public void setPhotoRemotePath(String str) {
        this.photoRemotePath = str;
    }

    public void setPlaceHint(String str) {
        this.placeHint = str;
    }

    public void setRecordId(int index) {
        this.recordId = index;
    }

    public void setStep(int index) {
        this.step = index;
    }

    public void setTrack(ArrayList<Point> arrayList) {
        this.track = arrayList;
    }

    public void setUploaded(boolean z2) {
        this.uploaded = z2;
    }

    public void setUserId(String str) {
        this.userId = str;
    }

    public void setVerified(boolean z2) {
        this.verified = z2;
    }

    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$cn$edu$pku$pkurunner$Model$Record$RecordPlace;

        static {
            int[] iArr = new int[RecordPlace.values().length];
            $SwitchMap$cn$edu$pku$pkurunner$Model$Record$RecordPlace = iArr;
            try {
                iArr[RecordPlace.WUSI.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$cn$edu$pku$pkurunner$Model$Record$RecordPlace[RecordPlace.WEIMING.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    public Record() {
        this.track = new ArrayList<>();
    }

    public static String getPlaceString(RecordPlace recordPlace) {
        int index = AnonymousClass2.$SwitchMap$cn$edu$pku$pkurunner$Model$Record$RecordPlace[recordPlace.ordinal()];
        return index != 1 ? index != 2 ? User.UNKNOWN_GENDER_STRING : "未名湖" : "五四";
    }

    public double getAccumulateBearing() {
        ArrayList<Point> arrayList = this.track;
        double accumulate = 0.0d;
        if (arrayList == null || arrayList.size() < 3) {
            return accumulate;
        }
        double longitudeSum = 0.0d;
        double latitudeSum = 0.0d;
        Iterator<Point> it = this.track.iterator();
        while (it.hasNext()) {
            Point next = it.next();
            if (next.getStatus() != Point.STATUS_MARKER) {
                longitudeSum += next.getLongitude();
                latitudeSum += next.getLatitude();
            }
        }
        double centerLongitude = longitudeSum / this.track.size();
        double centerLatitude = latitudeSum / this.track.size();
        for (int index = 1; index < this.track.size(); index++) {
            Point point = this.track.get(index);
            Point previous = this.track.get(index - 1);
            if (point.getStatus() == Point.STATUS_MARKER) {
                continue;
            }
            if (previous.getStatus() == Point.STATUS_MARKER && index > 1) {
                previous = this.track.get(index - 2);
            }
            double longitude = point.getLongitude() - centerLongitude;
            double latitude = point.getLatitude() - centerLatitude;
            double previousLongitude = previous.getLongitude() - centerLongitude;
            double previousLatitude = previous.getLatitude() - centerLatitude;
            accumulate += Math.atan2((latitude * previousLongitude) - (longitude * previousLatitude),
                    (longitude * previousLongitude) + (latitude * previousLatitude));
        }
        return accumulate;
    }

    public RecordPlace getPlace() {
        int index;
        ArrayList<Point> arrayList = this.track;
        if (arrayList == null || arrayList.isEmpty()) {
            return RecordPlace.UNKNOWN;
        }
        int[] iArr = new int[3];
        int index2 = 0;
        int index3 = 0;
        while (true) {
            if (index3 >= 30) {
                break;
            }
            int random = (int) (Math.random() * this.track.size());
            double longitude = this.track.get(random).getLongitude();
            double latitude = this.track.get(random).getLatitude();
            if (longitude > 116.3124d && longitude < 116.3139d && latitude > 39.9867d && latitude < 39.9885d) {
                iArr[0] = iArr[0] + 1;
            } else if (longitude <= 116.3074d || longitude >= 116.312d || latitude <= 39.9937d || latitude >= 39.996d) {
                iArr[2] = iArr[2] + 1;
            } else {
                iArr[1] = iArr[1] + 1;
            }
            index3++;
        }
        for (index = 1; index < 3; index++) {
            if (iArr[index] > iArr[index2]) {
                index2 = index;
            }
        }
        return RecordPlace.values()[index2];
    }

    public boolean isPlaceHintAvailable() {
        String str = this.placeHint;
        return (str == null || str.equals("")) ? false : true;
    }

    public String toString() {
        return "Record{id=" + this.f6978id + ", recordId=" + this.recordId + ", userId='" + this.userId + "', distance=" + this.distance + ", duration=" + this.duration + ", date=" + dateFormatter.format(this.date) + ", uploaded=" + this.uploaded + ", verified=" + this.verified + ", detailed=" + this.detailed + ", photoName='" + this.photoName + "', photoRemotePath='" + this.photoRemotePath + "', step=" + this.step + ", invalidReason=" + this.invalidReason + ", track=" + this.track + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int index) {
        parcel.writeInt(this.f6978id);
        parcel.writeInt(this.recordId);
        parcel.writeString(this.userId);
        parcel.writeInt(this.distance);
        parcel.writeInt(this.duration);
        Date date = this.date;
        parcel.writeLong(date != null ? date.getTime() : -1L);
        parcel.writeByte(this.uploaded ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.verified ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.detailed ? (byte) 1 : (byte) 0);
        parcel.writeInt(this.step);
        parcel.writeInt(this.invalidReason);
        parcel.writeTypedList(this.track);
        parcel.writeString(this.photoName);
        parcel.writeString(this.photoRemotePath);
    }

    public Record(Inner inner) {
        this.track = new ArrayList<>();
        this.f6978id = -1;
        this.userId = inner.userId;
        this.recordId = inner.recordId;
        this.distance = inner.distance;
        this.duration = (int) inner.duration;
        this.date = inner.date;
        this.uploaded = true;
        this.step = inner.step;
        this.verified = inner.verified;
        this.invalidReason = inner.morningBonus ? 100 : inner.invalidReason;
        this.photoRemotePath = inner.photoRemotePath;
        this.checkField = "";
        if (inner.track == null || inner.track.length == 0) {
            this.detailed = false;
            this.track = null;
            return;
        }
        this.detailed = true;
        ArrayList<Point> arrayList = new ArrayList<>();
        for (int index = 0; index < inner.track.length; index++) {
            arrayList.add(new Point(index, -1, inner.track[index][0], inner.track[index][1], inner.track[index].length < 3 ? 0 : (int) inner.track[index][2]));
        }
        this.track = arrayList;
    }

    public Record(String str, int index, int index2, Date date, int index3, String str2) {
        this.track = new ArrayList<>();
        this.distance = index;
        this.duration = index2;
        this.date = date;
        this.checkField = str2;
        this.f6978id = 0;
        this.recordId = -1;
        this.userId = str;
        this.verified = false;
        this.uploaded = false;
        this.detailed = false;
        this.step = index3;
        this.invalidReason = 0;
    }

    private Record(Parcel parcel) {
        this.track = new ArrayList<>();
        this.f6978id = parcel.readInt();
        this.recordId = parcel.readInt();
        this.userId = parcel.readString();
        this.distance = parcel.readInt();
        this.duration = parcel.readInt();
        long readLong = parcel.readLong();
        this.date = readLong == -1 ? null : new Date(readLong);
        this.uploaded = parcel.readByte() != 0;
        this.verified = parcel.readByte() != 0;
        this.detailed = parcel.readByte() != 0;
        this.step = parcel.readInt();
        this.invalidReason = parcel.readInt();
        this.track = parcel.createTypedArrayList(Point.CREATOR);
        this.photoName = parcel.readString();
        this.photoRemotePath = parcel.readString();
    }
}
