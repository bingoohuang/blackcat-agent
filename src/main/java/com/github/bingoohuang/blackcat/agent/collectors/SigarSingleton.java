package com.github.bingoohuang.blackcat.agent.collectors;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarProxy;

public class SigarSingleton {
    public static final SigarProxy SIGAR = SigarFactory.newSigar();
}
