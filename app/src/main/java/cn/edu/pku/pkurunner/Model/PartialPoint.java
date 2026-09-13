package cn.edu.pku.pkurunner.Model;

import androidx.core.app.NotificationCompat;
import cn.edu.pku.pkurunner.BuildConfig;
import java.util.ArrayList;
import java.util.List;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "partial_track")
public class PartialPoint {

    /* renamed from: id, reason: collision with root package name */
    @Column(isId = true, name = "id")
    private int f6975id;

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

    public PartialPoint(Point point) {
        this.f6975id = 0;
        this.sequence = 0;
        this.recordDbId = 0;
        this.latitude = point.getLatitude();
        this.longitude = point.getLongitude();
        this.status = point.getStatus();
    }

    public int getId() {
        return this.f6975id;
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
        this.f6975id = i2;
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

    public static ArrayList<PartialPoint> assignInfoToTrack(int i2, List<Point> list) {
        ArrayList<PartialPoint> arrayList = new ArrayList<>();
        for (int i3 = 0; i3 < list.size(); i3++) {
            PartialPoint partialPoint = new PartialPoint(list.get(i3));
            partialPoint.setSequence(i3);
            partialPoint.setRecordDbId(i2);
            arrayList.add(partialPoint);
        }
        return arrayList;
    }

    public Point toPoint() {
        return new Point(this.sequence, this.recordDbId, this.longitude, this.latitude, this.status);
    }

    public String toString() {
        return "PartialPoint{id=" + this.f6975id + ", sequence=" + this.sequence + ", recordDbId=" + this.recordDbId + ", latitude=" + this.latitude + ", longitude=" + this.longitude + ", status=" + this.status + '}';
    }

    public PartialPoint() {
    }
}
