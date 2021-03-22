package com.marlib.safecommunicator;

import java.math.BigInteger;
import java.util.Random;

public class ElGamal {
    private BigInteger p;
    private Integer alpha;
    private BigInteger g;
    private BigInteger b;
    private Integer k;

    public ElGamal(String p, String alpha, String g) {
        this.p = new BigInteger(p);
        this.alpha = new Integer(alpha);
        this.g = new BigInteger(g);
        setB();
        setPrimeK();
    }

    private void setB () {
        this.b = g.pow(alpha).mod(p);
    }

    public void setPrimeK() {
        Random rand = new Random();

        int coprime = rand.nextInt();
        while (!CalcUtils.isCoprime(coprime, p.intValue()-1)) {
            coprime = rand.nextInt();
        }
        this.k = coprime;
    }

    private BigInteger getC1 (Integer k) {
        return g.pow(k).mod(p);
    }

    private BigInteger getC2 (BigInteger sign, Integer k) {
        return (sign.multiply(b.pow(k))).mod(p);
    }

    public BigInteger getB() {
        return this.b;
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

        BigInteger c1 = getC1(k);
        BigInteger c2 = getC2(bigIntegerSign, k);

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
        int c1Power = p.intValue() - 1 - alpha.intValue();
        BigInteger fi = c1.pow(c1Power);
        BigInteger decryptedSign = fi.multiply(c2).remainder(p);

        return decryptedSign.toString();
    }
}
