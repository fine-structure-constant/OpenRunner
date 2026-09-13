package cn.edu.pku.pkurunner.Map;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SpeedHelperTest {

    private static final double EPS = 1e-9;

    @Test
    public void meterPerSecond_isIdentity() {
        assertThat(SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.MeterPerSecond, 5.0)).isWithin(EPS).of(5.0);
        assertThat(SpeedHelper.toUnitOf(SpeedHelper.SPEED_UNIT.MeterPerSecond, 5.0)).isWithin(EPS).of(5.0);
    }

    @Test
    public void kilometerPerHour_roundTrip() {
        double v = 7.2;
        double ms = SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.KilometerPerHour, v);
        assertThat(ms).isWithin(EPS).of(v / 3.6);
        double back = SpeedHelper.toUnitOf(SpeedHelper.SPEED_UNIT.KilometerPerHour, ms);
        assertThat(back).isWithin(1e-6).of(v);
    }

    @Test
    public void minutePerKilometer_roundTrip() {
        double pace = 5.0;
        double ms = SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.MinutePerKilometer, pace);
        assertThat(ms).isWithin(1e-6).of(16.666666666666668 / pace);
        double back = SpeedHelper.toUnitOf(SpeedHelper.SPEED_UNIT.MinutePerKilometer, ms);
        assertThat(back).isWithin(1e-6).of(pace);
    }

    @Test
    public void milePerHour_roundTrip() {
        double mph = 6.2137119223733395;
        double ms = SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.MilePerHour, mph);
        assertThat(ms).isWithin(1e-6).of(mph * 3.6 * 1.6);
        double back = SpeedHelper.toUnitOf(SpeedHelper.SPEED_UNIT.MilePerHour, ms);
        assertThat(back).isWithin(1e-6).of(mph);
    }

    @Test
    public void lightSpeed_roundTrip() {
        double distance = 1.0;
        double time = SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.C, distance);
        assertThat(time).isWithin(1e-3).of(distance * 2.99792458e8);
        double back = SpeedHelper.toUnitOf(SpeedHelper.SPEED_UNIT.C, time);
        assertThat(back).isWithin(1e-6).of(distance);
    }

    @Test
    public void minutePerKilometer_dividesByInput_zero_safe() {
        Double zero = SpeedHelper.fromUnitOf(SpeedHelper.SPEED_UNIT.MinutePerKilometer, 0.0);
        assertThat(zero.isInfinite()).isTrue();
    }

    @Test
    public void meterToLightYear_basic() {
        double ly = SpeedHelper.meterToLightYear(9.4607304725808e15);
        assertThat(ly).isWithin(1e-6).of(1.0);
    }

    @Test
    public void meterToPlanckLength_basic() {
        double pl = SpeedHelper.meterToPlanckLength(1.616229e-35);
        assertThat(pl).isWithin(1e-15).of(1.0);
    }

    @Test
    public void secondToPlanckTime_basic() {
        double pt = SpeedHelper.secondToPlanckTime(5.39116e-44);
        assertThat(pt).isWithin(1e-24).of(1.0);
    }
}
