// vim: ts=4 sw=4 et
/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.machines;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.MemoryRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HomebrewMachine implements Machine {
    
    private final static Logger logger = LoggerFactory.getLogger(HomebrewMachine.class.getName());
    
    // Constants used by the simulated system. These define the memory map.
    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    // 32K of RAM from $0000 - $7EFF
    private static final int MEMORY_BASE = 0x0000;
    private static final int MEMORY_SIZE = 0x7F00;

    // ACIA at $7F00-$7F03
    private static final int ACIA_BASE = 0x7F00;
    
    // VIA1 (Keyboard) at $7F40-$7F4F
    private static final int PIA1_BASE = 0x7F40;
    // VIA2 at $7F80-$7F9F
    private static final int PIA2_BASE = 0x7F80;

    // VDP at $7F60-$7F7F
    private static final int VDP_BASE = 0x7F60;

    // PC virtual Keyboard
    private static final int PCVKBD_BASE = 0x7FA0;

    // 32KB ROM at $8000-$FFFF
    private static final int ROM_BASE = 0x8000;
    private static final int ROM_SIZE = 0x8000;


    // The simulated peripherals
    private final Bus    bus;
    private final Cpu    cpu;
    private final Acia   acia;
    private final Via6522Keyboard pia1;
    //private final Pia    pia1;
    private final Pia    pia2;
    private final Memory ram;
    private       Memory rom;
    private final Vdp    vdp;
    private final PCVirtualKeyboard pcvkbd;

    public HomebrewMachine(String romFile) throws Exception {
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.cpu = new Cpu();
        this.ram = new Memory(MEMORY_BASE, MEMORY_BASE + MEMORY_SIZE - 1, false);
        this.acia = new Acia6551(ACIA_BASE);
        this.pia1 = new Via6522Keyboard(PIA1_BASE);
        logger.info("VIA1 Keyboard at {}",this.pia1.startAddress());
        this.pia2 = new Via6522(PIA2_BASE);
        logger.info("VIA2 at {}",this.pia2.startAddress());
        this.vdp = new Vdp(VDP_BASE, true);

        this.pcvkbd = new PCVirtualKeyboard(PCVKBD_BASE);

        bus.addCpu(cpu);
        bus.addDevice(ram);
        bus.addDevice(acia);
        bus.addDevice(pia1);
        bus.addDevice(pia2);
        bus.addDevice(vdp);
        bus.addDevice(pcvkbd);
        
        File romImage;
        if (romFile != null) {
            romImage = new File(romFile);
        }
        else {
            romImage = new File("homebrew.bin");
        }
        if (romImage.canRead()) {
            logger.info("Loading ROM image from file {}", romImage);
            this.rom = Memory.makeROM(ROM_BASE, ROM_BASE + ROM_SIZE - 1, romImage);
        } else {
            logger.info("Default ROM file {} not found, loading empty R/W memory image.", romImage);
            this.rom = Memory.makeRAM(ROM_BASE, ROM_BASE + ROM_SIZE - 1);
        }

        bus.addDevice(rom);
        
    }

    @Override
    public Bus getBus() {
        return bus;
    }

    @Override
    public Cpu getCpu() {
        return cpu;
    }

    @Override
    public Memory getRam() {
        return ram;
    }

    @Override
    public Acia getAcia() {
        return acia;
    }

    @Override
    public Pia getPia() {
        return pia2;
    }

    @Override
    public Crtc getCrtc() {
        return null;
    }

    @Override
    public Memory getRom() {
        return rom;
    }
    
    @Override
    public Vdp getVdp() {
        return vdp;
    }
    
    public void setRom(Memory rom) throws MemoryRangeException {
        if(this.rom != null) {
            bus.removeDevice(this.rom);
        }
        this.rom = rom;
        bus.addDevice(this.rom);
    }

    @Override
    public int getRomBase() {
        return ROM_BASE;
    }

    @Override
    public int getRomSize() {
        return ROM_SIZE;
    }

    @Override
    public int getMemorySize() {
        return MEMORY_SIZE;
    }

    @Override
    public String getName() {
        return "Homebrew";
    }

    @Override
    public Via6522Keyboard getKeyboardVia()
    {
        //return null;
        return pia1;
    }

    @Override
    public PCVirtualKeyboard getPCVirtualKeyboard()
    {
        return pcvkbd;
    }
}
