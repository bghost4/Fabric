package com.derpderphurr.network.fabric;

import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

public class TableReader {
    private OID tableSrc;
    private Map<String,OID> columns = new HashMap<>();
}
