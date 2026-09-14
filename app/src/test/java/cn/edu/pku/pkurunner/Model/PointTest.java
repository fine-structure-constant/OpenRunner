package cn.edu.pku.pkurunner.Model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class PointTest {

    @Test
    public void constructor_mapsSequenceRecordDbIdAndStatus() {
        Point p = new Point(7, 42, 116.31d, 39.99d, Point.STATUS_BEGIN);
        assertThat(p.getSequence()).isEqualTo(7);
        assertThat(p.getRecordDbId()).isEqualTo(42);
        assertThat(p.getLongitude()).isEqualTo(116.31d);
        assertThat(p.getLatitude()).isEqualTo(39.99d);
        assertThat(p.getStatus()).isEqualTo(Point.STATUS_BEGIN);
    }

    @Test
    public void assignInfoToTrack_renumberSequenceAndRebindRecord() {
        ArrayList<Point> raw = new ArrayList<>();
        raw.add(new Point(0, 0, 116.0d, 39.0d, Point.STATUS_RUNNING));
        raw.add(new Point(0, 0, 117.0d, 40.0d, Point.STATUS_END));
        raw.add(new Point(0, 0, 118.0d, 41.0d, Point.STATUS_RUNNING));

        ArrayList<Point> assigned = Point.assignInfoToTrack(9, raw);

        assertThat(assigned).hasSize(3);
        for (int i = 0; i < assigned.size(); i++) {
            assertThat(assigned.get(i).getSequence()).isEqualTo(i);
            assertThat(assigned.get(i).getRecordDbId()).isEqualTo(9);
        }
        assertThat(assigned.get(0).getLongitude()).isEqualTo(116.0d);
        assertThat(assigned.get(0).getLatitude()).isEqualTo(39.0d);
        assertThat(assigned.get(1).getStatus()).isEqualTo(Point.STATUS_END);
    }

    @Test
    public void assignInfoToTrack_onEmptyList_returnsEmpty() {
        assertThat(Point.assignInfoToTrack(1, new ArrayList<Point>())).isEmpty();
    }

    @Test
    public void setters_roundTrip() {
        Point p = new Point();
        p.setId(5);
        p.setSequence(6);
        p.setRecordDbId(7);
        p.setLatitude(39.99d);
        p.setLongitude(116.31d);
        p.setStatus(Point.STATUS_MARKER);

        assertThat(p.getId()).isEqualTo(5);
        assertThat(p.getSequence()).isEqualTo(6);
        assertThat(p.getRecordDbId()).isEqualTo(7);
        assertThat(p.getLatitude()).isEqualTo(39.99d);
        assertThat(p.getLongitude()).isEqualTo(116.31d);
        assertThat(p.getStatus()).isEqualTo(Point.STATUS_MARKER);
    }

    @Test
    public void toString_containsAllFields() {
        Point p = new Point(1, 2, 116.0d, 39.0d, Point.STATUS_RUNNING);
        String s = p.toString();
        assertThat(s).contains("latitude=39.0");
        assertThat(s).contains("longitude=116.0");
        assertThat(s).contains("recordDbId=2");
    }
}
