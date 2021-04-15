package com.marlib.safecommunicator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;

public class RSA {
    private int keyLongDecrypt;
    long n;
    long d;
    long e;

    public RSA(String first_prime, String second_prime) {
        generateKeys(first_prime, second_prime);
    }

    private List<Long> divisorsNumber(long number) {
        List<Long> divisors = new ArrayList<Long>();
        divisors.add(number);
        for (int i = 2; i <= sqrt(number); i++) {
            if (number % i == 0) {
                divisors.add((long) i);
                divisors.add(number / i);
            }
        }
        return divisors;
    }

    private boolean listCompare(List<Long> list1, List<Long> list2) {
        for (int i = 0; i < list1.size(); i++) {
            int ind = list2.indexOf(list1.get(i));
            if (ind >= 0) {
                return true;
            }
        }
        return false;
    }

    public String getPublicKey() {
        return Long.toString(n) + ';' + e + ";" + keyLongDecrypt;
    }

    private String generateKeys(String first_prime, String second_prime) {
        long first_factor = Long.parseLong(first_prime);
        long second_factor = Long.parseLong(second_prime);
        n = first_factor * second_factor;
        long euler = (first_factor - 1) * (second_factor - 1);
        e = euler;

        List<Long> e_div = divisorsNumber(e);
        List<Long> euler_div = divisorsNumber(euler);

        while (listCompare(e_div, euler_div)) {
            e = (long) ((Math.random() * (euler - 3)) + 2);
            if (e % 2 == 0) {
                continue;
            }
            e_div = divisorsNumber(e);
        }

        long param1 = euler, param2 = euler;
        long param3 = e, param4 = 1;
        long newParam1, newParam2, temp;

        while (param3 != 1) {
            temp = param1 / param3;
            newParam1 = param1 - (temp * param3);
            newParam2 = temp * param4;
            newParam2 = (param2 - newParam2);

            param1 = param3;
            param2 = param4;
            param3 = CalcUtils.modulo(newParam1, euler);
            param4 = CalcUtils.modulo(newParam2, euler);
        }

        d = param4;
        BigInteger longD = new BigInteger(Long.toString(d));
        BigInteger longE = new BigInteger(Long.toString(e));
        BigInteger longEULER = new BigInteger(Long.toString(euler));
        BigInteger longlong = longD.multiply(longE);
        BigInteger longlong2 = longlong.mod(longEULER);

        if (longlong2.compareTo(BigInteger.ONE) == 0) {
            keyLongDecrypt = Long.toString(n).length();
            return Long.toString(n) + ';' + e + ';' + d + ";" + keyLongDecrypt;
        }
        return "ErrorRSA";
    }

    public String encryptMessageRSA(String message, String public_key1, String public_key2, int keyLongEncrypt) {
        long n = Long.parseLong(public_key1);
        long e = Long.parseLong(public_key2);
        BigInteger longE = new BigInteger(Long.toString(e));
        BigInteger longN = new BigInteger(Long.toString(n));
        long m = 0;
        long m_;
        int J = 0;

        while (J < message.length() % keyLongEncrypt) {
            J++;
        }
        StringBuilder encryptMsg = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            m_ = StringUtils.charToInt(message.charAt(i));

            if (m * 100 + m_ < n) {
                m = m * 100 + m_;
            } else {
                BigInteger longM = new BigInteger(Long.toString(m));
                BigInteger longind = longM.modPow(longE, longN);
                StringBuilder indToString = new StringBuilder(longind.toString());

                while (indToString.length() < keyLongEncrypt) {
                    indToString.insert(0, "0");
                }

                encryptMsg.append(indToString);
                m = m_;
            }
        }

        BigInteger longM = new BigInteger(Long.toString(m));
        BigInteger longind = longM.modPow(longE, longN);
        StringBuilder indToString = new StringBuilder(longind.toString());

        while (indToString.length() < keyLongEncrypt) {
            indToString.insert(0, "0");
        }
        encryptMsg.append(indToString);

        return encryptMsg.toString();
    }

    public String decryptMessageRSA(String message) {
        BigInteger longD = new BigInteger(Long.toString(d));
        BigInteger longN = new BigInteger(Long.toString(n));
        StringBuilder msg = new StringBuilder();
        StringBuilder msg2 = new StringBuilder();
        long blok;
        int i;

        for (i = 0; i < message.length() - (keyLongDecrypt - 1); i += keyLongDecrypt) {
            blok = Long.parseLong(message.substring(i, i + keyLongDecrypt));
            BigInteger longBLOK = new BigInteger(Long.toString(blok));
            BigInteger m = longBLOK.modPow(longD, longN);

            if (m.toString().length() % 2 == 1) {
                msg.append("0");
            }

            msg.append(m.toString());
        }

        if (i < message.length() - 1) {
            blok = Long.parseLong(message.substring(i));
            BigInteger longBLOK = new BigInteger(Long.toString(blok));
            BigInteger m = longBLOK.modPow(longD, longN);
            msg.append(m.toString());
        }

        for (i = 0; i < msg.length() - 1; i += 2) {
            char ch = StringUtils.intToChar(Integer.parseInt(msg.substring(i, i + 2)));
            msg2.append(ch);
        }

        return msg2.toString();
    }
}
