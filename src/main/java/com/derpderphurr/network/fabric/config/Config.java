package com.derpderphurr.network.fabric.config;

import com.derpderphurr.network.fabric.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Config {

    private final ArrayList<Device> devices = new ArrayList<>();
    private Path path;

    public Config() {

    }

    public List<Device> getDevices() {
        return devices;
    }

    public void addDevice(Device d) {
        System.out.println("Added Device?");
        devices.add(d);
    }

    // TODO: password is accepted for a future encrypted-at-rest format; the file is currently written as plain JSON.
    public void save(Path p,String password) throws IOException {
        List<DeviceDataTransferObject> dtos = devices.stream()
                .map(DeviceDataTransferObject::fromDevice)
                .collect(Collectors.toList());

        System.out.printf("Saving %d Devices",dtos.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(p.toFile(), dtos);
        this.path = p;
    }

    // TODO: password is accepted for a future encrypted-at-rest format; the file is currently read as plain JSON.
    public static Config load(Path p,String password) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<DeviceDataTransferObject> dtos = mapper.readValue(p.toFile(),
                mapper.getTypeFactory().constructCollectionType(List.class, DeviceDataTransferObject.class));

        Config config = new Config();
        config.path = p;
        for (DeviceDataTransferObject dto : dtos) {
            config.addDevice(dto.toDevice());
        }
        return config;
    }

}
