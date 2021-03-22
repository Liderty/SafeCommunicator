package com.marlib.safecommunicator;

public final class CalcUtils {

    private CalcUtils() {
        throw new UnsupportedOperationException();
    }

    public static int greatestCommonDivisor(int a, int b) {
        if (a != b) {
            return greatestCommonDivisor(a>b ? (a - b) : a, b>a ? (b - a) : b);
        }
        return a;
    }

    public static boolean isCoprime(int a, int b) {
        return (greatestCommonDivisor(a, b) == 1);
    }
}
