// vim: ts=4 sw=4 et
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

    private int[] devmem;

    public PCVirtualKeyboard(int address) throws MemoryRangeException {
        super(address, address + PCVK_SIZE - 1, "PC Virtual Keyboard");
        logger.info("PC VK Initialise");
        this.name = "PCVirtualKeyboard";
        this.devmem = new int[PCVK_SIZE];
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        //logger.info("Read address 0x"+Integer.toHexString(address));
        return devmem[address];
    }
    @Override
    public void write(int address, int data) throws MemoryAccessException {
        //logger.info("Write address 0x"+Integer.toHexString(address)+" data 0x"+Integer.toHexString(data));
        devmem[address] = data;
    }

    public void newkey(char ch, int key, int ext, int loc, int mods, boolean pressed){
        logger.info("Key: "+ch+" 0x"+Integer.toHexString(key) +
                    " Ext: 0x"+Integer.toHexString(ext) +
                    " Loc: 0x"+Integer.toHexString(loc) +
                    " Mods: 0x"+Integer.toHexString(mods) +
                    (pressed ? " Pressed":"Released"));
        char trch = translate(ch); 
        try {
            if (pressed)
            {
                this.write(0, trch);
            }
            else
            {
                this.write(0, 0xFF);
                this.write(1, trch);
            }
        } catch ( MemoryAccessException e) {
            logger.info("Virtual Keyboard Write error "+e);
            return;
        }
    }

    private char translate(char ch) {
        switch (ch)
        {
            case 0x0A: return 0x0D;
            default: return ch;
        }
    }
}    
