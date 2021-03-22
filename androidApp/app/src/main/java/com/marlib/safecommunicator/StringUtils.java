package com.marlib.safecommunicator;

public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    public static String convertToAsciiString(String stringToConvert) {
        String outputAsciiString = "";

        for(char sign: stringToConvert.toCharArray()) {
            outputAsciiString += Integer.toString((int) sign);
            outputAsciiString += " ";
        }

        return  outputAsciiString;
    }

    public static String convertAsciiStringToString(String asciiString) {
        String[] splitedString = asciiString.split("\\s+");
        String outputString = "";

        for(String sign: splitedString) {
            char charSign = (char) Integer.parseInt(sign);
            outputString += charSign;
        }

        return outputString;
    }
}
