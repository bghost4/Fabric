package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.BinaryFormat;
import javafx.util.StringConverter;

public class BinaryStringConverter extends StringConverter<Number> {
    @Override
    public String toString(Number object) {
        return BinaryFormat.humanReadable(object.longValue());
    }

    @Override
    public Number fromString(String string) {
        return Double.NaN;
    }
}
