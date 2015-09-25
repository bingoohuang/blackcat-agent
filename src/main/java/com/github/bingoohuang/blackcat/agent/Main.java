package com.github.bingoohuang.blackcat.agent;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        Memory memory = hardware.getMemory();
        System.out.println("Total Memory: "
                        + _.humanReadableByteCount(memory.getTotal())
        );

        while (true) {
            System.out.println(
                    "Available Memory: "
                            + _.humanReadableByteCount(memory.getAvailable())
                            + " at " + _.now()
            );

            _.sleep(10, TimeUnit.SECONDS);
        }
    }

}
