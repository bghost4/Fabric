package com.derpderphurr.network.fabric.datastore;

import com.derpderphurr.network.fabric.Device;
import com.derpderphurr.network.fabric.Interface;
import com.derpderphurr.network.fabric.InterfaceData;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.stream.Stream;

public interface DataStorage {
    void put(InterfaceData d);
    void putAll(Stream<InterfaceData> data);
    void putAll(Collection<InterfaceData> data);
    void putAll(InterfaceData...data);
    Stream<InterfaceData> get(Device d);
    Stream<InterfaceData> get(Interface i);
    Stream<InterfaceData> get(Interface i, ZonedDateTime begin, ZonedDateTime end);
    Stream<InterfaceData> get(Interface i, ZonedDateTime noneBefore);
}
