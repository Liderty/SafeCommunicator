package com.marlib.safecommunicator;

import org.junit.Test;

import static com.marlib.safecommunicator.CalcUtils.greatestCommonDivisor;
import static org.junit.Assert.*;

public class CalcUtilsUnitTest {

    @Test
    public void greatestCommonDivisor_zero() {
        assertEquals(5, greatestCommonDivisor(5, 0));
    }

    @Test
    public void greatestCommonDivisor_equals() {
        assertEquals(66, greatestCommonDivisor(66, 66));
    }

    @Test
    public void greatestCommonDivisor_primeNumbers() {
        assertEquals(1, greatestCommonDivisor(647, 971));
    }

    @Test
    public void greatestCommonDivisor_oneTwo() {
        assertEquals(1, greatestCommonDivisor(1, 2));
    }

    @Test
    public void greatestCommonDivisor_bigNumbers() {
        assertEquals(16, greatestCommonDivisor(26048, 300624));
    }
}
