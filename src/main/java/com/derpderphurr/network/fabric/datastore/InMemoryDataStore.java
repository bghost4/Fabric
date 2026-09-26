package com.derpderphurr.network.fabric.datastore;

import com.derpderphurr.network.fabric.Device;
import com.derpderphurr.network.fabric.Interface;
import com.derpderphurr.network.fabric.InterfaceData;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Stream;

public class InMemoryDataStore implements DataStorage {

    private final LinkedList<InterfaceData> dataStore = new LinkedList<>();

    @Override
    public void put(InterfaceData d) {
        dataStore.add(d);
    }

    @Override
    public void putAll(Stream<InterfaceData> data) {
        dataStore.addAll(data.toList());
    }

    @Override
    public void putAll(Collection<InterfaceData> data) {
        dataStore.addAll(data);
    }

    @Override
    public void putAll(InterfaceData... data) {
        dataStore.addAll(Arrays.asList(data));
    }

    @Override
    public Stream<InterfaceData> get(Device d) {
        return dataStore.stream();
    }

    @Override
    public Stream<InterfaceData> get(Interface i) {
        return dataStore.stream().filter(d -> d.getIndex() == i.getIndex());
    }

    @Override
    public Stream<InterfaceData> get(Interface i, ZonedDateTime begin, ZonedDateTime end) {
        return dataStore.stream().filter(item -> {
            ZonedDateTime itemTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.getCaptureTimestamp()),begin.getZone());
            return (item.getDevice() == i.getDevice()) && (itemTime.isAfter(begin)) && (itemTime.isBefore(end));
        });
    }

    @Override
    public Stream<InterfaceData> get(Interface i, ZonedDateTime begin) {
        return dataStore.stream().filter(item -> {
            ZonedDateTime itemTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.getCaptureTimestamp()),begin.getZone());
            return (item.getDevice() == i.getDevice()) && itemTime.isAfter(begin);
        });
    }
}
