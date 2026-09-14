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
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        /* JADX WARN: Can't rename method to resolve collision */
        @Override
        public Point createFromParcel(Parcel parcel) {
            int readInt = parcel.readInt();
            Point point = new Point(parcel.readInt(), parcel.readInt(), parcel.readDouble(), parcel.readDouble(), parcel.readInt());
            point.setId(readInt);
            return point;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override
        public Point[] newArray(int index) {
            return new Point[index];
        }
    };
    public static final int STATUS_BEGIN = 1;
    public static final int STATUS_END = 2;
    public static final int STATUS_MARKER = 3;
    public static final int STATUS_RUNNING = 0;

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

    @Override
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

    public void setId(int index) {
        this.f6977id = index;
    }

    public void setLatitude(double value) {
        this.latitude = value;
    }

    public void setLongitude(double value) {
        this.longitude = value;
    }

    public void setRecordDbId(int index) {
        this.recordDbId = index;
    }

    public void setSequence(int index) {
        this.sequence = index;
    }

    public void setStatus(int index) {
        this.status = index;
    }

    public Point(LatLng latLng) {
        this.f6977id = 0;
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.status = 0;
    }

    public static ArrayList<Point> assignInfoToTrack(int index, ArrayList<Point> arrayList) {
        ArrayList<Point> arrayList2 = new ArrayList<>();
        for (int index2 = 0; index2 < arrayList.size(); index2++) {
            arrayList2.add(new Point(index2, index, arrayList.get(index2).getLongitude(), arrayList.get(index2).getLatitude(), arrayList.get(index2).getStatus()));
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

    @Override
    public void writeToParcel(Parcel parcel, int index) {
        parcel.writeInt(this.f6977id);
        parcel.writeInt(this.sequence);
        parcel.writeInt(this.recordDbId);
        parcel.writeDouble(this.latitude);
        parcel.writeDouble(this.longitude);
        parcel.writeInt(this.status);
    }

    public Point(int index, int index2, double value, double value2, int index3) {
        this.f6977id = 0;
        this.sequence = index;
        this.recordDbId = index2;
        this.latitude = value2;
        this.longitude = value;
        this.status = index3;
    }
}
