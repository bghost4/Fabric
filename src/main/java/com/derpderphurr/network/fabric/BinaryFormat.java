package com.derpderphurr.network.fabric;

import java.text.*;

public class BinaryFormat extends NumberFormat {
    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((long)number,toAppendTo,pos);
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return new StringBuffer(humanReadable(number));
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        //TODO left as an excersise to the reader
        return null;
    }

    public static String humanReadable(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }


}
