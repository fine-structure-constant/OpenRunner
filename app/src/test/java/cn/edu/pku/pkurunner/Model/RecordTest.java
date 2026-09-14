package cn.edu.pku.pkurunner.Model;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class RecordTest {

    private static final double WUSI_LON = 116.3131d;
    private static final double WUSI_LAT = 39.9876d;
    private static final double WEIMING_LON = 116.3097d;
    private static final double WEIMING_LAT = 39.9948d;
    private static final double OUTSIDE_LON = 116.4000d;
    private static final double OUTSIDE_LAT = 39.9000d;

    private static ArrayList<Point> trackOf(double longitude, double latitude, int count) {
        ArrayList<Point> track = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            track.add(new Point(i, 0, longitude, latitude, Point.STATUS_RUNNING));
        }
        return track;
    }

    private static ArrayList<Point> squareOf(double[][] coords) {
        ArrayList<Point> track = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            track.add(new Point(i, 0, coords[i][0], coords[i][1], Point.STATUS_RUNNING));
        }
        return track;
    }

    @Test
    public void defaultConstructor_createsEmptyTrack() {
        Record record = new Record();

        assertThat(record.getTrack()).isNotNull();
        assertThat(record.getTrack()).isEmpty();
    }

    @Test
    public void getPlace_withEmptyOrNullTrack_returnsUnknown() {
        assertThat(new Record().getPlace()).isEqualTo(Record.RecordPlace.UNKNOWN);

        Record nullTrack = new Record();
        nullTrack.setTrack(null);
        assertThat(nullTrack.getPlace()).isEqualTo(Record.RecordPlace.UNKNOWN);
    }

    @Test
    public void getPlace_allPointsInsideWusi_returnsWusi() {
        Record record = new Record();
        record.setTrack(trackOf(WUSI_LON, WUSI_LAT, 40));

        assertThat(record.getPlace()).isEqualTo(Record.RecordPlace.WUSI);
    }

    @Test
    public void getPlace_allPointsInsideWeiming_returnsWeiming() {
        Record record = new Record();
        record.setTrack(trackOf(WEIMING_LON, WEIMING_LAT, 40));

        assertThat(record.getPlace()).isEqualTo(Record.RecordPlace.WEIMING);
    }

    @Test
    public void getPlace_allPointsOutsideBothAreas_returnsUnknown() {
        Record record = new Record();
        record.setTrack(trackOf(OUTSIDE_LON, OUTSIDE_LAT, 40));

        assertThat(record.getPlace()).isEqualTo(Record.RecordPlace.UNKNOWN);
    }

    @Test
    public void getPlaceString_mapsEveryPlace() {
        assertThat(Record.getPlaceString(Record.RecordPlace.WUSI)).isEqualTo("五四");
        assertThat(Record.getPlaceString(Record.RecordPlace.WEIMING)).isEqualTo("未名湖");
        assertThat(Record.getPlaceString(Record.RecordPlace.UNKNOWN)).isEqualTo(User.UNKNOWN_GENDER_STRING);
    }

    @Test
    public void getAccumulateBearing_withTooFewPoints_returnsZero() {
        Record nullTrack = new Record();
        nullTrack.setTrack(null);
        assertThat(nullTrack.getAccumulateBearing()).isEqualTo(0.0d);

        Record twoPoints = new Record();
        twoPoints.setTrack(trackOf(WUSI_LON, WUSI_LAT, 2));
        assertThat(twoPoints.getAccumulateBearing()).isEqualTo(0.0d);
    }

    @Test
    public void getAccumulateBearing_counterClockwiseSquare_returnsPositiveTurn() {
        Record record = new Record();
        record.setTrack(squareOf(new double[][]{{0.0d, 0.0d}, {1.0d, 0.0d}, {1.0d, 1.0d}, {0.0d, 1.0d}}));

        assertThat(record.getAccumulateBearing()).isWithin(1.0e-9).of(3.0d * Math.PI / 2.0d);
    }

    @Test
    public void getAccumulateBearing_clockwiseSquare_returnsNegativeTurn() {
        Record record = new Record();
        record.setTrack(squareOf(new double[][]{{0.0d, 0.0d}, {0.0d, 1.0d}, {1.0d, 1.0d}, {1.0d, 0.0d}}));

        assertThat(record.getAccumulateBearing()).isWithin(1.0e-9).of(-3.0d * Math.PI / 2.0d);
    }

    @Test
    public void isPlaceHintAvailable_isFalseForNullOrEmpty() {
        Record record = new Record();
        assertThat(record.isPlaceHintAvailable()).isFalse();

        record.setPlaceHint("");
        assertThat(record.isPlaceHintAvailable()).isFalse();

        record.setPlaceHint("五四体育场");
        assertThat(record.isPlaceHintAvailable()).isTrue();
    }

    @Test
    public void writeToParcel_thenCreateFromParcel_preservesEveryField() {
        Record record = new Record("user-1", 3200, 900, new Date(1700000000000L), 4200, "check");
        record.setId(7);
        record.setRecordId(88);
        record.setUploaded(true);
        record.setVerified(true);
        record.setDetailed(true);
        record.setPhotoName("photo.jpg");
        record.setPhotoRemotePath("/remote/photo.jpg");
        record.setTrack(trackOf(WUSI_LON, WUSI_LAT, 4));

        Parcel parcel = Parcel.obtain();
        record.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Record restored = Record.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restored.getId()).isEqualTo(7);
        assertThat(restored.getRecordId()).isEqualTo(88);
        assertThat(restored.getUserId()).isEqualTo("user-1");
        assertThat(restored.getDistance()).isEqualTo(3200);
        assertThat(restored.getDuration()).isEqualTo(900);
        assertThat(restored.getDate()).isEqualTo(new Date(1700000000000L));
        assertThat(restored.isUploaded()).isTrue();
        assertThat(restored.isVerified()).isTrue();
        assertThat(restored.isDetailed()).isTrue();
        assertThat(restored.getStep()).isEqualTo(4200);
        assertThat(restored.getPhotoName()).isEqualTo("photo.jpg");
        assertThat(restored.getPhotoRemotePath()).isEqualTo("/remote/photo.jpg");
        assertThat(restored.getTrack()).hasSize(4);
        assertThat(restored.getTrack().get(2).getSequence()).isEqualTo(2);
        assertThat(restored.getTrack().get(2).getStatus()).isEqualTo(Point.STATUS_RUNNING);
    }

    @Test
    public void writeToParcel_swapsLatitudeAndLongitudeOnRead() {
        Record record = new Record("user-1", 100, 20, new Date(0L), 30, "check");
        Point point = new Point(0, 0, 116.31d, 39.99d, Point.STATUS_RUNNING);
        ArrayList<Point> track = new ArrayList<>();
        track.add(point);
        track.add(new Point(1, 0, 116.32d, 39.98d, Point.STATUS_RUNNING));
        record.setTrack(track);

        Parcel parcel = Parcel.obtain();
        record.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Record restored = Record.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restored.getTrack().get(0).getLongitude()).isEqualTo(39.99d);
        assertThat(restored.getTrack().get(0).getLatitude()).isEqualTo(116.31d);
        assertThat(restored.getTrack().get(1).getLongitude()).isEqualTo(39.98d);
        assertThat(restored.getTrack().get(1).getLatitude()).isEqualTo(116.32d);
    }

    @Test
    public void writeToParcel_withNullDate_roundTripsAsNull() {
        Record record = new Record("user-1", 100, 20, null, 30, "check");
        record.setTrack(new ArrayList<Point>());

        Parcel parcel = Parcel.obtain();
        record.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Record restored = Record.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restored.getDate()).isNull();
    }

    @Test
    public void fromInner_withoutTrack_isNotDetailedAndKeepsNullTrack() {
        Record.Inner inner = new Gson().fromJson(
                "{\"recordId\":11,\"userId\":\"u-2\",\"distance\":1500,\"duration\":600.9,"
                        + "\"step\":2000,\"verified\":true,\"detail\":[],\"photoPath\":\"/p/1.jpg\"}",
                Record.Inner.class);

        Record record = new Record(inner);

        assertThat(record.getRecordId()).isEqualTo(11);
        assertThat(record.getUserId()).isEqualTo("u-2");
        assertThat(record.getDistance()).isEqualTo(1500);
        assertThat(record.getDuration()).isEqualTo(600);
        assertThat(record.getStep()).isEqualTo(2000);
        assertThat(record.isVerified()).isTrue();
        assertThat(record.isUploaded()).isTrue();
        assertThat(record.isDetailed()).isFalse();
        assertThat(record.getTrack()).isNull();
        assertThat(record.getPhotoRemotePath()).isEqualTo("/p/1.jpg");
        assertThat(record.getCheckField()).isEmpty();
    }

    @Test
    public void fromInner_withTrack_buildsPointsAndIsDetailed() {
        Record.Inner inner = new Gson().fromJson(
                "{\"recordId\":12,\"userId\":\"u-3\",\"distance\":2000,\"duration\":700,"
                        + "\"step\":3000,\"detail\":[[116.31,39.99,0],[116.32,39.98,1],[116.33,39.97]]}",
                Record.Inner.class);

        Record record = new Record(inner);

        assertThat(record.isDetailed()).isTrue();
        assertThat(record.getTrack()).hasSize(3);
        assertThat(record.getTrack().get(0).getSequence()).isEqualTo(0);
        assertThat(record.getTrack().get(1).getSequence()).isEqualTo(1);
        assertThat(record.getTrack().get(2).getSequence()).isEqualTo(2);
        assertThat(record.getTrack().get(0).getRecordDbId()).isEqualTo(-1);
        assertThat(record.getTrack().get(0).getLongitude()).isEqualTo(116.31d);
        assertThat(record.getTrack().get(0).getLatitude()).isEqualTo(39.99d);
        assertThat(record.getTrack().get(1).getStatus()).isEqualTo(1);
        assertThat(record.getTrack().get(2).getStatus()).isEqualTo(0);
    }

    @Test
    public void fromInner_withMorningBonus_setsInvalidReasonToHundred() {
        Record.Inner inner = new Gson().fromJson(
                "{\"recordId\":13,\"morningBonus\":true,\"invalidReason\":3}",
                Record.Inner.class);

        assertThat(new Record(inner).getInvalidReason()).isEqualTo(100);
    }

    @Test
    public void fromInner_withoutMorningBonus_keepsInvalidReason() {
        Record.Inner inner = new Gson().fromJson(
                "{\"recordId\":14,\"morningBonus\":false,\"invalidReason\":3}",
                Record.Inner.class);

        assertThat(new Record(inner).getInvalidReason()).isEqualTo(3);
    }
}
