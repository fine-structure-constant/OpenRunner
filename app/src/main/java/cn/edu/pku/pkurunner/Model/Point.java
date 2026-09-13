package cn.edu.pku.pkurunner.Model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.app.NotificationCompat;
import com.amap.api.maps2d.model.LatLng;
import cn.edu.pku.pkurunner.BuildConfig;
import java.io.Serializable;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "track")
public class Point implements Serializable, Parcelable {
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() { // from class: cn.edu.pku.pkurunner.Model.Point.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Point createFromParcel(Parcel parcel) {
            int readInt = parcel.readInt();
            Point point = new Point(parcel.readInt(), parcel.readInt(), parcel.readDouble(), parcel.readDouble(), parcel.readInt());
            point.setId(readInt);
            return point;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Point[] newArray(int i2) {
            return new Point[i2];
        }
    };
    public static final int STATUS_BEGIN = 1;
    public static final int STATUS_END = 2;
    public static final int STATUS_MARKER = 3;
    public static final int STATUS_RUNNING = 0;

    /* renamed from: id, reason: collision with root package name */
    @Column(isId = true, name = "id")
    private int f6977id;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "recordDbId")
    private int recordDbId;

    @Column(name = "sequence")
    private int sequence;

    @Column(name = NotificationCompat.CATEGORY_STATUS)
    private int status;

    public Point() {
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getId() {
        return this.f6977id;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public int getRecordDbId() {
        return this.recordDbId;
    }

    public int getSequence() {
        return this.sequence;
    }

    public int getStatus() {
        return this.status;
    }

    public void setId(int i2) {
        this.f6977id = i2;
    }

    public void setLatitude(double d2) {
        this.latitude = d2;
    }

    public void setLongitude(double d2) {
        this.longitude = d2;
    }

    public void setRecordDbId(int i2) {
        this.recordDbId = i2;
    }

    public void setSequence(int i2) {
        this.sequence = i2;
    }

    public void setStatus(int i2) {
        this.status = i2;
    }

    public Point(LatLng latLng) {
        this.f6977id = 0;
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.status = 0;
    }

    public static ArrayList<Point> assignInfoToTrack(int i2, ArrayList<Point> arrayList) {
        ArrayList<Point> arrayList2 = new ArrayList<>();
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            arrayList2.add(new Point(i3, i2, arrayList.get(i3).getLongitude(), arrayList.get(i3).getLatitude(), arrayList.get(i3).getStatus()));
        }
        return arrayList2;
    }

    public JSONArray toJSONArray() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        try {
            jSONArray.put(0, this.longitude);
            jSONArray.put(1, this.latitude);
            jSONArray.put(2, this.status);
            return jSONArray;
        } catch (JSONException e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    public LatLng toLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }

    public String toString() {
        return "Point{id=" + this.f6977id + ", sequence=" + this.sequence + ", recordDbId=" + this.recordDbId + ", latitude=" + this.latitude + ", longitude=" + this.longitude + ", status=" + this.status + '}';
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i2) {
        parcel.writeInt(this.f6977id);
        parcel.writeInt(this.sequence);
        parcel.writeInt(this.recordDbId);
        parcel.writeDouble(this.latitude);
        parcel.writeDouble(this.longitude);
        parcel.writeInt(this.status);
    }

    public Point(int i2, int i3, double d2, double d3, int i4) {
        this.f6977id = 0;
        this.sequence = i2;
        this.recordDbId = i3;
        this.latitude = d3;
        this.longitude = d2;
        this.status = i4;
    }
}
