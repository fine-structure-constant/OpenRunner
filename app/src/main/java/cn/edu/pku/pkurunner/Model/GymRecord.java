package cn.edu.pku.pkurunner.Model;

import cn.edu.pku.pkurunner.BuildConfig;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "record2")
public class GymRecord {
    private static final DateFormat dateFormatter;

    @Column(name = "duration")
    private int duration;

    @Column(isId = true, name = "id", property = "UNIQUE")
    private int f6974id;

    @Column(name = "place")
    private int place;

    @Column(name = "recordId")
    private int recordId;

    @Column(name = "startTime")
    private Date startTime;

    @Column(name = "uploaded")
    private Boolean uploaded;

    @Column(name = "userId")
    private String userId;

    @Column(name = "verified")
    private Boolean verified;

    public static class Inner {
        private double duration;
        private int place;
        private int recordId;
        private Date startTime;
        private String userId;
    }

    public GymRecord() {
    }

    public int getDuration() {
        return this.duration;
    }

    public int getId() {
        return this.f6974id;
    }

    public int getPlace() {
        return this.place;
    }

    public int getRecordId() {
        return this.recordId;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public String getUserId() {
        return this.userId;
    }

    public Boolean isUploaded() {
        return this.uploaded;
    }

    public Boolean isVerified() {
        return this.verified;
    }

    public void setDuration(int index) {
        this.duration = index;
    }

    public void setId(int index) {
        this.f6974id = index;
    }

    public void setPlace(int index) {
        this.place = index;
    }

    public void setRecordId(int index) {
        this.recordId = index;
    }

    public void setStartTime(Date date) {
        this.startTime = date;
    }

    public void setUploaded(Boolean bool) {
        this.uploaded = bool;
    }

    public void setUserId(String str) {
        this.userId = str;
    }

    public void setVerified(Boolean bool) {
        this.verified = bool;
    }

    public GymRecord(String str, int index) {
        this.place = index;
        this.f6974id = 0;
        this.recordId = -1;
        this.userId = str;
        this.duration = -1;
        this.startTime = new Date();
        Boolean bool = Boolean.FALSE;
        this.uploaded = bool;
        this.verified = bool;
    }

    public String toString() {
        return "GymRecord{id=" + this.f6974id + ", recordId=" + this.recordId + ", userId='" + this.userId + "', place='" + this.place + "', duration=" + this.duration + ", startTime=" + dateFormatter.format(this.startTime) + ", uploaded=" + this.uploaded + '}';
    }

    static {
        DateFormat dateTimeInstance = DateFormat.getDateTimeInstance();
        dateFormatter = dateTimeInstance;
        dateTimeInstance.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public GymRecord(Inner inner) {
        this.f6974id = -1;
        Boolean bool = Boolean.TRUE;
        this.verified = bool;
        this.recordId = inner.recordId;
        this.duration = (int) inner.duration;
        this.place = inner.place;
        this.startTime = inner.startTime;
        this.uploaded = bool;
        this.userId = inner.userId;
    }
}
