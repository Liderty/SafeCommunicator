package com.marlib.safecommunicator;

import java.math.BigInteger;

public class ElGamal {
    private BigInteger p;
    private BigInteger alpha;
    private BigInteger g;

    public ElGamal(String p, String alpha, String g) {
        this.p = new BigInteger(p);
        this.p = new BigInteger(alpha);
        this.p = new BigInteger(g);
    }

}
