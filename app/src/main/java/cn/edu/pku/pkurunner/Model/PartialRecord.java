package cn.edu.pku.pkurunner.Model;

import cn.edu.pku.pkurunner.Utils.SecUtil;
import cn.edu.pku.pkurunner.BuildConfig;
import java.util.ArrayList;
import java.util.Date;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "partial_record")
public class PartialRecord {

    @Column(name = "date")
    private Date date;

    @Column(name = "distance")
    private int distance;

    @Column(name = "duration")
    private int duration;
    private ArrayList<Point> geo;

    /* renamed from: id, reason: collision with root package name */
    @Column(isId = true, name = "id", property = "UNIQUE")
    private int f6976id;

    @Column(name = "step")
    private int step;

    public PartialRecord() {
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

    public ArrayList<Point> getGeo() {
        return this.geo;
    }

    public int getId() {
        return this.f6976id;
    }

    public int getStep() {
        return this.step;
    }

    public PartialRecord(int i2, int i3, Date date, int i4) {
        this.duration = i3;
        this.distance = i2;
        this.step = i4;
        this.date = date;
    }

    public Record toRecord(String str) {
        int i2 = this.distance;
        int i3 = this.duration;
        Date date = this.date;
        return new Record(str, i2, i3, date, this.step, SecUtil.generateCheckField(str, date));
    }
}
