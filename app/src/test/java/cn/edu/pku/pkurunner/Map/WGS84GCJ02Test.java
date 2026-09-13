package cn.edu.pku.pkurunner.Map;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class WGS84GCJ02Test {

    private static final double EPS = 1e-6;

    @Test
    public void convert_outsideChinaNorth_returnsInput() {
        double[] p = WGS84_GCJ02.convert(40.0, -10.0);
        assertThat(p[0]).isWithin(EPS).of(40.0);
        assertThat(p[1]).isWithin(EPS).of(-10.0);

        double[] p2 = WGS84_GCJ02.convert(0.0, 200.0);
        assertThat(p2[0]).isWithin(EPS).of(0.0);
        assertThat(p2[1]).isWithin(EPS).of(200.0);

        double[] p3 = WGS84_GCJ02.convert(80.0, 60.0);
        assertThat(p3[0]).isWithin(EPS).of(80.0);
        assertThat(p3[1]).isWithin(EPS).of(60.0);
    }

    @Test
    public void convert_insideChina_offsetsApplied() {
        double[] p = WGS84_GCJ02.convert(116.404, 39.915);
        assertThat(p[0]).isNotEqualTo(116.404);
        assertThat(p[1]).isNotEqualTo(39.915);
        assertThat(p[0]).isGreaterThan(116.0);
        assertThat(p[1]).isGreaterThan(35.0);
    }

    @Test
    public void convert_isStable_onRepeat() {
        double[] a = WGS84_GCJ02.convert(116.404, 39.915);
        double[] b = WGS84_GCJ02.convert(116.404, 39.915);
        assertThat(a[0]).isWithin(EPS).of(b[0]);
        assertThat(a[1]).isWithin(EPS).of(b[1]);
    }

    @Test
    public void convert_strictlyOutsideLat_returnsInput() {
        double[] p = WGS84_GCJ02.convert(71.9, 30.0);
        assertThat(p[0]).isWithin(EPS).of(71.9);
        double[] p2 = WGS84_GCJ02.convert(137.9, 30.0);
        assertThat(p2[0]).isWithin(EPS).of(137.9);
    }

    @Test
    public void convert_strictlyOutsideLon_returnsInput() {
        double[] p = WGS84_GCJ02.convert(100.0, 0.8);
        assertThat(p[1]).isWithin(EPS).of(0.8);
        double[] p2 = WGS84_GCJ02.convert(100.0, 55.9);
        assertThat(p2[1]).isWithin(EPS).of(55.9);
    }
}
