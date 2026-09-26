package com.derpderphurr.network.fabric.config;

import com.derpderphurr.network.fabric.Device;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {

    private final ArrayList<Device> devices = new ArrayList<>();
    private Path path;

    public Config() {

    }

    public List<Device> getDevices() {
        return devices;
    }

    public void addDevice(Device d) {
        devices.add(d);
    }

    public void save(Path p) {

    }

    public static Config load(Path p) {

    }

}
