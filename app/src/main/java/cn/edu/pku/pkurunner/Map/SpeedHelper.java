package cn.edu.pku.pkurunner.Map;

public abstract class SpeedHelper {

    public enum SPEED_UNIT {
        MeterPerSecond,
        KilometerPerHour,
        MinutePerKilometer,
        MilePerHour,
        C
    }

    public static double meterToLightYear(double value) {
        return value / 9.4607304725808E15d;
    }

    public static double meterToPlanckLength(double value) {
        return value / 1.616229E-35d;
    }

    public static double secondToPlanckTime(double value) {
        return value / 5.39116E-44d;
    }

    static /* synthetic */ class SpeedUnitSwitchMap {

        static final /* synthetic */ int[] SPEED_UNIT_SWITCH_MAP;

        static {
            int[] iArr = new int[SPEED_UNIT.values().length];
            SPEED_UNIT_SWITCH_MAP = iArr;
            try {
                iArr[SPEED_UNIT.MeterPerSecond.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                SPEED_UNIT_SWITCH_MAP[SPEED_UNIT.KilometerPerHour.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                SPEED_UNIT_SWITCH_MAP[SPEED_UNIT.MinutePerKilometer.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                SPEED_UNIT_SWITCH_MAP[SPEED_UNIT.MilePerHour.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                SPEED_UNIT_SWITCH_MAP[SPEED_UNIT.C.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }

    public static double fromUnitOf(SPEED_UNIT speed_unit, double value) {
        double value2;
        int index = SpeedUnitSwitchMap.SPEED_UNIT_SWITCH_MAP[speed_unit.ordinal()];
        if (index == 1) {
            return value;
        }
        if (index == 2) {
            return value / 3.6d;
        }
        if (index == 3) {
            return 16.666666666666668d / value;
        }
        if (index == 4) {
            value *= 3.6d;
            value2 = 1.6d;
        } else {
            if (index != 5) {
                return 0.0d;
            }
            value2 = 2.99792458E8d;
        }
        return value * value2;
    }

    public static double toUnitOf(SPEED_UNIT speed_unit, double value) {
        double value2;
        int index = SpeedUnitSwitchMap.SPEED_UNIT_SWITCH_MAP[speed_unit.ordinal()];
        if (index == 1) {
            return value;
        }
        if (index == 2) {
            return value * 3.6d;
        }
        if (index == 3) {
            return 16.666666666666668d / value;
        }
        if (index == 4) {
            value /= 3.6d;
            value2 = 1.6d;
        } else {
            if (index != 5) {
                return 0.0d;
            }
            value2 = 2.99792458E8d;
        }
        return value / value2;
    }
}
