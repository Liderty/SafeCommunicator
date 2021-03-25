package com.marlib.safecommunicator;

import java.math.BigInteger;
import java.util.Random;

public class ElGamal {
    private final BigInteger p;
    private final Integer alpha;
    private final BigInteger g;
    private BigInteger b;

    public ElGamal(String p, String alpha, String g) {
        this.p = new BigInteger(p);
        this.alpha = Integer.valueOf(alpha);
        this.g = new BigInteger(g);
        setB();
    }

    private void setB() {
        this.b = g.pow(alpha).mod(p);
    }

    public int getCoprimeK(int public_p) {
        Random rand = new Random();
        int coprime = rand.nextInt(200);

        while (!CalcUtils.isCoprime(coprime, public_p - 1)) {
            coprime = rand.nextInt(200);
        }
        return coprime;
    }

    private BigInteger getC1(int k, BigInteger public_g, BigInteger public_p) {
        return public_g.pow(k).mod(public_p);
    }

    private BigInteger getC2(BigInteger sign, int k, BigInteger public_p, BigInteger public_b) {
        return (sign.multiply(public_b.pow(k))).mod(public_p);
    }

    public BigInteger getB() {
        return this.b;
    }

    public String encrypt(String asciiMessage, String strPublic_p, String strPublic_b, String strPublic_g) {
        String[] splitedString = asciiMessage.split("\\s+");
        StringBuilder encryptedMessage = new StringBuilder();

        BigInteger public_p = new BigInteger(strPublic_p);
        BigInteger public_b = new BigInteger(strPublic_b);
        BigInteger public_g = new BigInteger(strPublic_g);

        for (String sign : splitedString) {
            encryptedMessage.append(encryptSign(sign, public_p, public_b, public_g)).append(" ");
        }

        return encryptedMessage.toString();
    }

    private String encryptSign(String sign, BigInteger public_p, BigInteger public_b, BigInteger public_g) {
        BigInteger bigIntegerSign = new BigInteger(sign);
        int k = getCoprimeK(public_p.intValue());

        BigInteger c1 = getC1(k, public_g, public_p);
        BigInteger c2 = getC2(bigIntegerSign, k, public_p, public_b);

        return c1.toString() + " " + c2.toString();
    }

    public String decrypt(String asciiMessage) {
        String[] splitedString = asciiMessage.split("\\s+");
        StringBuilder decryptedMessage = new StringBuilder();

        for (int i = 0; i < splitedString.length; i += 2) {
            decryptedMessage.append(decryptSign(new BigInteger(splitedString[i]), new BigInteger(splitedString[i + 1]))).append(" ");
        }

        return decryptedMessage.toString();
    }

    private String decryptSign(BigInteger c1, BigInteger c2) {
        int c1Power = p.intValue() - 1 - alpha;
        BigInteger fi = c1.pow(c1Power);
        BigInteger decryptedSign = fi.multiply(c2).remainder(p);

        return decryptedSign.toString();
    }
}
