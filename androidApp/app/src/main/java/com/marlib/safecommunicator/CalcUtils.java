package com.marlib.safecommunicator;

import static java.lang.Math.max;

public final class CalcUtils {

    private CalcUtils() {
        throw new UnsupportedOperationException();
    }

    public static int greatestCommonDivisor(int a, int b) {
        if(a==0 || b==0) {
            return max(a, b);
        }

        if (a != b) {
            return greatestCommonDivisor(a > b ? (a - b) : a, b > a ? (b - a) : b);
        }
        return a;
    }

    public static boolean isCoprime(int a, int b) {
        return (greatestCommonDivisor(a, b) == 1);
    }

    public static long modulo(long number, long mod) {
        while (number < 0) {
            number += mod;
        }
        return number % mod;
    }
}
