package com.marlib.safecommunicator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class ElGamal {
    private BigInteger p;
    private Integer alpha;
    private BigInteger g;
    private BigInteger b;

    public ElGamal(String p, String alpha, String g) {
        this.p = new BigInteger(p);
        this.alpha = new Integer(alpha);
        this.g = new BigInteger(g);

        calculateB();
    }

    private void calculateB () {
        this.b = g.pow(alpha).mod(p);
    }

    private BigInteger calculateC1 (Integer k) {
        return g.pow(k).mod(p);
    }

    private BigInteger calculateC2 (BigInteger sign, Integer k) {
        return (sign.multiply(b.pow(k))).mod(p);
    }

    public BigInteger getB() {
        return this.b;
    }

    public Integer getPrimeK() { //TODO: return prime
        return 127;
    }

    private int GCD(int a, int b)
    {
        if (a != b) {
            return GCD(a>b ? (a - b) : a, b>a ? (b - a) : b);
        }

        return a;
    }

    public String encrypt(String asciiMessage) {
        String[] splitedString = asciiMessage.split("\\s+");
        String encryptedMessage = "";

        for(String sign: splitedString) {
            encryptedMessage += encryptSign(sign) + " ";
        }

        return encryptedMessage;
    }

    private String encryptSign(String sign){
        BigInteger bigIntegerSign = new BigInteger(sign);

        Integer k = getPrimeK();
        BigInteger c1 = calculateC1(k);
        BigInteger c2 = calculateC2(bigIntegerSign, k);

        return c1.toString() + " " + c2.toString();
    }

    public String decrypt(String asciiMessage) {
        String[] splitedString = asciiMessage.split("\\s+");
        String decryptedMessage = "";

        for(int i=0; i<splitedString.length; i+=2) {
            decryptedMessage += decryptSign(new BigInteger(splitedString[i]), new BigInteger(splitedString[i+1])) + " ";
        }

        return decryptedMessage;
    }

    private String decryptSign(BigInteger c1, BigInteger c2){
        System.out.println("DECRYPTING: "+c1+" "+c2);
        BigDecimal insideInsider = new BigDecimal(c1.pow(alpha));
        System.out.println(insideInsider);

        BigDecimal insider = BigDecimal.valueOf(1).divide(insideInsider, RoundingMode.CEILING);
        System.out.println("INSIDE: "+insider);

        BigDecimal decryptedSign = new BigDecimal(c2).multiply(insider).remainder(new BigDecimal(p));
        System.out.println("DECRYPTED VALUE: "+decryptedSign);

        return decryptedSign.toString();
    }
}
