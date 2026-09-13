package cn.edu.pku.pkurunner.Map;

public class WGS84_GCJ02 {
    private static boolean a(double d2, double d3) {
        return d2 < 72.004d || d2 > 137.8347d || d3 < 0.8293d || d3 > 55.8271d;
    }

    private static double b(double d2, double d3) {
        double d4 = d2 * 2.0d;
        double sqrt = (-100.0d) + d4 + (d3 * 3.0d) + (d3 * 0.2d * d3) + (0.1d * d2 * d3) + (Math.sqrt(Math.abs(d2)) * 0.2d) + ((((Math.sin((6.0d * d2) * 3.141592653589793d) * 20.0d) + (Math.sin(d4 * 3.141592653589793d) * 20.0d)) * 2.0d) / 3.0d);
        double d5 = d3 * 3.141592653589793d;
        return sqrt + ((((Math.sin(d5) * 20.0d) + (Math.sin((d3 / 3.0d) * 3.141592653589793d) * 40.0d)) * 2.0d) / 3.0d) + ((((Math.sin((d3 / 12.0d) * 3.141592653589793d) * 160.0d) + (Math.sin(d5 / 30.0d) * 320.0d)) * 2.0d) / 3.0d);
    }

    public static double[] convert(double d2, double d3) {
        if (a(d2, d3)) {
            return new double[]{d2, d3};
        }
        double d4 = d2 - 105.0d;
        double d5 = d3 - 35.0d;
        double c2 = c(d4, d5);
        double b2 = b(d4, d5);
        double d6 = (d3 / 180.0d) * 3.141592653589793d;
        double sin = Math.sin(d6);
        double d7 = 1.0d - ((0.006693421622965943d * sin) * sin);
        double sqrt = Math.sqrt(d7);
        return new double[]{d2 + ((c2 * 180.0d) / (((6378245.0d / sqrt) * Math.cos(d6)) * 3.141592653589793d)), d3 + ((b2 * 180.0d) / ((6335552.717000426d / (d7 * sqrt)) * 3.141592653589793d))};
    }

    private static double c(double d2, double d3) {
        double d4 = d2 * 0.1d;
        return d2 + 300.0d + (d3 * 2.0d) + (d4 * d2) + (d4 * d3) + (Math.sqrt(Math.abs(d2)) * 0.1d) + ((((Math.sin((6.0d * d2) * 3.141592653589793d) * 20.0d) + (Math.sin((d2 * 2.0d) * 3.141592653589793d) * 20.0d)) * 2.0d) / 3.0d) + ((((Math.sin(d2 * 3.141592653589793d) * 20.0d) + (Math.sin((d2 / 3.0d) * 3.141592653589793d) * 40.0d)) * 2.0d) / 3.0d) + ((((Math.sin((d2 / 12.0d) * 3.141592653589793d) * 150.0d) + (Math.sin((d2 / 30.0d) * 3.141592653589793d) * 300.0d)) * 2.0d) / 3.0d);
    }
}
