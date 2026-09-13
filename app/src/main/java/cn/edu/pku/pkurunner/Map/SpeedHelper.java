package cn.edu.pku.pkurunner.Map;

public abstract class SpeedHelper {

    public enum SPEED_UNIT {
        MeterPerSecond,
        KilometerPerHour,
        MinutePerKilometer,
        MilePerHour,
        C
    }

    public static double meterToLightYear(double d2) {
        return d2 / 9.4607304725808E15d;
    }

    public static double meterToPlanckLength(double d2) {
        return d2 / 1.616229E-35d;
    }

    public static double secondToPlanckTime(double d2) {
        return d2 / 5.39116E-44d;
    }

    static /* synthetic */ class a {

        /* renamed from: a, reason: collision with root package name */
        static final /* synthetic */ int[] f6970a;

        static {
            int[] iArr = new int[SPEED_UNIT.values().length];
            f6970a = iArr;
            try {
                iArr[SPEED_UNIT.MeterPerSecond.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f6970a[SPEED_UNIT.KilometerPerHour.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f6970a[SPEED_UNIT.MinutePerKilometer.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f6970a[SPEED_UNIT.MilePerHour.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                f6970a[SPEED_UNIT.C.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }

    public static double fromUnitOf(SPEED_UNIT speed_unit, double d2) {
        double d3;
        int i2 = a.f6970a[speed_unit.ordinal()];
        if (i2 == 1) {
            return d2;
        }
        if (i2 == 2) {
            return d2 / 3.6d;
        }
        if (i2 == 3) {
            return 16.666666666666668d / d2;
        }
        if (i2 == 4) {
            d2 *= 3.6d;
            d3 = 1.6d;
        } else {
            if (i2 != 5) {
                return 0.0d;
            }
            d3 = 2.99792458E8d;
        }
        return d2 * d3;
    }

    public static double toUnitOf(SPEED_UNIT speed_unit, double d2) {
        double d3;
        int i2 = a.f6970a[speed_unit.ordinal()];
        if (i2 == 1) {
            return d2;
        }
        if (i2 == 2) {
            return d2 * 3.6d;
        }
        if (i2 == 3) {
            return 16.666666666666668d / d2;
        }
        if (i2 == 4) {
            d2 /= 3.6d;
            d3 = 1.6d;
        } else {
            if (i2 != 5) {
                return 0.0d;
            }
            d3 = 2.99792458E8d;
        }
        return d2 / d3;
    }
}
