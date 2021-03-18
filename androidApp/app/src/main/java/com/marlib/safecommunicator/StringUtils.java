package com.marlib.safecommunicator;

public class StringUtils {

    public String convertToAsciiString(String stringToConvert) {
        String outputAsciiString = "";

        for(char sign: stringToConvert.toCharArray()) {
            outputAsciiString += Integer.toString((int) sign);
            outputAsciiString += " ";
        }

        return  outputAsciiString;
    }

    public String convertAsciiStringToString(String asciiString) {
        String[] splitedString = asciiString.split("\\s+");
        String outputString = "";

        for(String sign: splitedString) {
            char charSign = (char) Integer.parseInt(sign);
            outputString += charSign;
        }

        return outputString;
    }
}
