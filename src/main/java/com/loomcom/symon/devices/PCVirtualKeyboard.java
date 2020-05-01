package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.MemoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.event.KeyEvent;

public class PCVirtualKeyboard extends Device {
    public static final int PCVK_SIZE = 4;

    private final String name;
    private final static Logger logger = LoggerFactory.getLogger("PCVK");

    public PCVirtualKeyboard(int address) throws MemoryRangeException {
        super(address, address + PCVK_SIZE - 1, "PC Virtual Keyboard");
		logger.info("PC VK Initialise");
		this.name = "PCVirtualKeyboard";
	}

    @Override
    public String toString() {
        return name;
    }

	@Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
		logger.info("Read address 0x"+Integer.toHexString(address));
        return 0;
	}
    @Override
    public void write(int address, int data) throws MemoryAccessException {
	}
}	
