package com.marlib.safecommunicator;

public final class StringUtils {
    private static final String ASCII_CHARACTERS = "`;1234567890QWERTYUIOPASDFGHJKLZXCVBNM a-=qwertyuiop[]asdfghjkl'zxcvbnm,./{}:\nąęśćżźńół\"\\_+!@#?$%*()";

    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    public static String convertToAsciiString(String stringToConvert) {
        StringBuilder outputAsciiString = new StringBuilder();

        for (char sign : stringToConvert.toCharArray()) {
            outputAsciiString.append(Integer.toString((int) sign));
            outputAsciiString.append(" ");
        }

        return outputAsciiString.toString();
    }

    public static String convertAsciiStringToString(String asciiString) {
        String[] splitedString = asciiString.split("\\s+");
        StringBuilder outputString = new StringBuilder();

        for (String sign : splitedString) {
            char charSign = (char) Integer.parseInt(sign);
            outputString.append(charSign);
        }

        return outputString.toString();
    }

    public static int charToInt(char ch) {
        return ASCII_CHARACTERS.indexOf(ch);
    }

    public static char intToChar(int index) {
        return ASCII_CHARACTERS.charAt(index);
    }

}
