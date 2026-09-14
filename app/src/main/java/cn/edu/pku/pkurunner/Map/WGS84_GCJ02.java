package cn.edu.pku.pkurunner.Map;

public class WGS84_GCJ02 {
    private static boolean a(double value, double value2) {
        return value < 72.004d || value > 137.8347d || value2 < 0.8293d || value2 > 55.8271d;
    }

    private static double b(double value, double value2) {
        double value3 = value * 2.0d;
        double sqrt = (-100.0d) + value3 + (value2 * 3.0d) + (value2 * 0.2d * value2) + (0.1d * value * value2) + (Math.sqrt(Math.abs(value)) * 0.2d) + ((((Math.sin((6.0d * value) * 3.141592653589793d) * 20.0d) + (Math.sin(value3 * 3.141592653589793d) * 20.0d)) * 2.0d) / 3.0d);
        double d5 = value2 * 3.141592653589793d;
        return sqrt + ((((Math.sin(d5) * 20.0d) + (Math.sin((value2 / 3.0d) * 3.141592653589793d) * 40.0d)) * 2.0d) / 3.0d) + ((((Math.sin((value2 / 12.0d) * 3.141592653589793d) * 160.0d) + (Math.sin(d5 / 30.0d) * 320.0d)) * 2.0d) / 3.0d);
    }

    public static double[] convert(double value, double value2) {
        if (a(value, value2)) {
            return new double[]{value, value2};
        }
        double value3 = value - 105.0d;
        double d5 = value2 - 35.0d;
        double character = c(value3, d5);
        double flag = b(value3, d5);
        double d6 = (value2 / 180.0d) * 3.141592653589793d;
        double sin = Math.sin(d6);
        double d7 = 1.0d - ((0.006693421622965943d * sin) * sin);
        double sqrt = Math.sqrt(d7);
        return new double[]{value + ((character * 180.0d) / (((6378245.0d / sqrt) * Math.cos(d6)) * 3.141592653589793d)), value2 + ((flag * 180.0d) / ((6335552.717000426d / (d7 * sqrt)) * 3.141592653589793d))};
    }

    private static double c(double value, double value2) {
        double value3 = value * 0.1d;
        return value + 300.0d + (value2 * 2.0d) + (value3 * value) + (value3 * value2) + (Math.sqrt(Math.abs(value)) * 0.1d) + ((((Math.sin((6.0d * value) * 3.141592653589793d) * 20.0d) + (Math.sin((value * 2.0d) * 3.141592653589793d) * 20.0d)) * 2.0d) / 3.0d) + ((((Math.sin(value * 3.141592653589793d) * 20.0d) + (Math.sin((value / 3.0d) * 3.141592653589793d) * 40.0d)) * 2.0d) / 3.0d) + ((((Math.sin((value / 12.0d) * 3.141592653589793d) * 150.0d) + (Math.sin((value / 30.0d) * 3.141592653589793d) * 300.0d)) * 2.0d) / 3.0d);
    }
}
