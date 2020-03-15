/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
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

package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Color;

/**
 * Simulation of a TMS9918
 */
public class Vdp extends Device {
    private final static Logger logger = LoggerFactory.getLogger(Vdp.class.getName());

    private static final int VDP_REG_NUM = 8;

    // Display Mode
    public static final int DM_GRAPHICSI  = 0;
    public static final int DM_TEXT       = 1;
    public static final int DM_MULTICOLOR = 2;
    public static final int DM_GRAPHICSII = 4;

    // Registers
    private static int[] Registers;
    private static int StatusReg = 0;

    // Video RAM - not in the normal address space
    public int VramSizeInBytes = 4 * 1024;
    public int vram[];

    // VDP Status
    public int     DisplayMode;
    public boolean ExternalVDP;
    public boolean Memory16K;
    public boolean Blank;
    public boolean InterruptEnable;
    public boolean SpriteLarge;
    public boolean SpriteDouble;
    public int     BaseAddrNameTable;
    public int     BaseAddrColorTable;
    public int     BaseAddrPatternTable;
    public int     BaseAddrSpriteAttribute;
    public int     BaseAddrSpritePatternTable;
    public int     ColorTextFG;
    public int     ColorTextBG;

	public Color[] colors;

    // States following writes to the VDP
    enum VDP_WRITE_STATE {
	    START, HAVE_BYTE, HAVE_ADDRESS
    }
    private static final int VDP_CMD_WRITE_VRAM  = 0;
    private static final int VDP_CMD_WRITE_ADDR  = 1;
    private static final int VDP_CMD_WRITE_REG   = 1;
    private static final int VDP_CMD_READ_VRAM   = 2;
    private static final int VDP_CMD_READ_STATUS = 3;

    private String[] VdpCommandString = {"WriteVRAM","WriteADDR","ReadVRAM","ReadREG"};

    private VDP_WRITE_STATE VDPWriteState;
    private int CurrentRegister = 0;          // Current chosen register
    private int VDPWriteByte0 = 0;            // results of sequential writes to VDP reg 1
    private int CurrentVRAMAddress = 0;

    private String VdpStateToString(VDP_WRITE_STATE state)
    {
       switch(state) {
           case START: return "START";
           case HAVE_BYTE: return "HAVE_BYTE";
           case HAVE_ADDRESS: return "HAVE_ADDRESS";
       }
       return "?";
    }

    public void setupVram(boolean b16K)
    {
        if(b16K) {
            VramSizeInBytes = 16 * 1024;
        } else {
            VramSizeInBytes = 4 * 1024;
        }
        vram = new int[VramSizeInBytes];
    }
    public Vdp(int address, boolean b16K) throws MemoryRangeException {
        super(address, address + 3, "TMS9918A VDP");
        this.Registers = new int[VDP_REG_NUM];
        setupVram(b16K);
        this.VDPWriteState = VDP_WRITE_STATE.START;
        this.DisplayMode = DM_GRAPHICSI; // == 0

        this.ExternalVDP = false;
        this.Memory16K = b16K;
        this.Blank = false;
        this.InterruptEnable = false;
        this.SpriteLarge = false;
        this.SpriteDouble = false;
        this.BaseAddrNameTable = 0;
        this.BaseAddrColorTable = 0;
        this.BaseAddrPatternTable = 0;
        this.BaseAddrSpriteAttribute = 0;
        this.BaseAddrSpritePatternTable = 0;
        this.ColorTextFG = 4;
        this.ColorTextBG = 14;

		colors = new Color[16];
		/*
		colors[0] = new Color(0, 0, 0, 0); // transparent
		colors[1] = new Color(0, 0, 0, 255); // black
		colors[2] = new Color(0, 255, 0, 255); // medium green
		colors[3] = new Color(80, 255, 80, 255); // light green
		colors[4] = new Color(0, 0, 190, 255); // dark blue
		colors[5] = new Color(80, 80, 255, 255); // light blue
		colors[6] = new Color(190, 0, 0, 255); // dark red
		colors[7] = new Color(0, 255, 255, 255); // cyan 
		colors[8] = new Color(255, 0, 0, 255); // medium red 
		colors[9] = new Color(255, 80, 80, 255); // light red 
		colors[10] = new Color(180, 180, 0, 255); // dark yellow 
		colors[11] = new Color(255, 255, 80, 255); // light yellow 
		colors[12] = new Color(0, 190, 0, 255); // dark green
		colors[13] = new Color(255, 0, 255, 255); // magenta 
		colors[14] = new Color(180, 180, 180, 255); // grey
		colors[15] = new Color(255, 255, 255, 255); // white
		*/

	// colours from lospec.com
	
		colors[ 0] = new Color(0x00, 0x00, 0x00, 0x00); // transparent
		colors[ 1] = new Color(0x00, 0x00, 0x00, 0xff); // black
		colors[14] = new Color(0xca, 0xca, 0xca, 0xff); // grey
		colors[ 6] = new Color(0xb7, 0x5e, 0x51, 0xff); // dark red
		colors[ 8] = new Color(0xd9, 0x64, 0x59, 0xff); // medium red 
		colors[ 9] = new Color(0xfe, 0x87, 0x7c, 0xff); // light red 
		colors[10] = new Color(0xca, 0xc1, 0x5e, 0xff); // dark yellow 
		colors[11] = new Color(0xdd, 0xce, 0x85, 0xff); // light yellow 
		colors[12] = new Color(0x3c, 0xa0, 0x42, 0xff); // dark green
		colors[ 2] = new Color(0x40, 0xb6, 0x4a, 0xff); // medium green
		colors[ 3] = new Color(0x73, 0xce, 0x7c, 0xff); // light green
		colors[ 4] = new Color(0x59, 0x55, 0xdf, 0xff); // dark blue
		colors[ 5] = new Color(0x7e, 0x75, 0xf0, 0xff); // light blue
		colors[ 7] = new Color(0x64, 0xda, 0xee, 0xff); // cyan 
		colors[13] = new Color(0xb5, 0x65, 0xb3, 0xff); // magenta 
		colors[15] = new Color(0xff, 0xff, 0xff, 0xff); // white
    }
    
    @Override
    public String getName()
    {
        return "TMS9918";
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        VDP_WRITE_STATE oldState = VDPWriteState;
//        if(oldState == VDP_WRITE_STATE.START)
//            logger.info("---------------------------------");
//        logger.info("    write "+address+" "+VdpCommandString[address]+" -> 0x"+Integer.toHexString(data));
        switch(address)
        {
            case VDP_CMD_WRITE_VRAM:
                if(VDPWriteState==VDP_WRITE_STATE.HAVE_ADDRESS) {
                    writeVRAM(CurrentVRAMAddress, data);
                    CurrentVRAMAddress += 1;
                }
                break;
            case VDP_CMD_WRITE_ADDR:  // also VDP_CMD_WRITE_REG
                switch(VDPWriteState) {
                    case START:
                        VDPWriteByte0 = data;
                        VDPWriteState = VDP_WRITE_STATE.HAVE_BYTE;
                        break;
                    case HAVE_BYTE:
                        if(data<128) {
                            VDPWriteState = VDP_WRITE_STATE.HAVE_ADDRESS;
                            CurrentVRAMAddress = VDPWriteByte0 | ((data&0x3F)<<8);
                        } else {
                            CurrentRegister = data & 0x7;
                            writeRegister(VDPWriteByte0);
                            VDPWriteState = VDP_WRITE_STATE.START;
                        }
                        break;
                    case HAVE_ADDRESS:
                        VDPWriteByte0 = data;
                        VDPWriteState = VDP_WRITE_STATE.HAVE_BYTE;
                        break;
                }
                break;
            default:
                break;
        }
//        if(VDPWriteState==VDP_WRITE_STATE.START) {
//            logger.info("  state: "+VdpStateToString(oldState)+" -> "+VdpStateToString(VDPWriteState));
//        } else if(VDPWriteState==VDP_WRITE_STATE.HAVE_BYTE) {
//            logger.info("  state: "+VdpStateToString(oldState)+" -> "+VdpStateToString(VDPWriteState)+" 0x"+Integer.toHexString(VDPWriteByte0));
//        } else if(VDPWriteState==VDP_WRITE_STATE.HAVE_ADDRESS) {
//            logger.info("  state: "+VdpStateToString(oldState)+" -> "+VdpStateToString(VDPWriteState)+" 0x"+Integer.toHexString(CurrentVRAMAddress));
//        }
//        logger.info("");
    };

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        //logger.info("    read "+address+" "+VdpCommandString[address]);
        switch (address) {
            case VDP_CMD_READ_VRAM:
                switch(VDPWriteState) {
                    case HAVE_ADDRESS:
                        int data = readVRAM(CurrentVRAMAddress);
                        CurrentVRAMAddress += 1;
                        return data;
                    default:
                        /* Invalid */
                        logger.error("VDP read: invalid");
                        VDPWriteState = VDP_WRITE_STATE.START;
                        break;
                }
                break;
            case VDP_CMD_READ_STATUS:
                int data = StatusReg;
                // reset status register after a read
                StatusReg = 0;
                //logger.info("VDP read "+address+" "+data);
                VDPWriteState = VDP_WRITE_STATE.START;
                return data;
            default:
                logger.error("VDP read: unrecognised address "+address);
                return 0;
        }
        return 0;
    }

    private void writeRegister(int data)
    {
        logger.info("VDP Write Reg "+CurrentRegister+" -> 0x"+Integer.toHexString(data));
        Registers[CurrentRegister] = data;
        switch (CurrentRegister) {
            case 0:
                ExternalVDP     =  (data & 0x01) != 0;
                DisplayMode    &= 0x3;                    // clear mode bit 3 (just keep 1&2)
                DisplayMode    |= ((data & 0x02) << 1);   // set it with bit 2 of reg
                break;
            case 1:
                SpriteDouble    =  (data & 0x01) != 0;
                SpriteLarge     =  (data & 0x02) != 0;
                DisplayMode    &=  0x4;                   // clear mode bits 1&2
                DisplayMode    |=  ((data & 0x8)>>2);     // M2 in bit 4. Put in 2nd bit of mode  
                DisplayMode    |=  ((data & 0x10) >> 4);  // M1 in bit 5. Put in 1st bit of mode

                InterruptEnable =  (data & 0x20) != 0;
                Blank           =  (data & 0x40) != 0;
                boolean oldMemory16K = Memory16K;
                Memory16K       =  (data & 0x80) != 0;
                if(oldMemory16K != Memory16K) {
                    setupVram(Memory16K);
                }
                break;
            case 2:
                BaseAddrNameTable = (data & 0xF) << 10;
                break;
            case 3:
                if ( DisplayMode == DM_GRAPHICSI ) {
                    BaseAddrColorTable = (data & 0xFF) << 6;
                } else if ( DisplayMode == DM_GRAPHICSII ) {
                    BaseAddrColorTable = (data & 0x80) << 6;
                } else {
                    BaseAddrColorTable = 0;
                }
                break;
            case 4:
                if (DisplayMode == DM_GRAPHICSII) {
                    BaseAddrPatternTable = (data & 0x4) << 11;
                } else {
                    BaseAddrPatternTable = (data & 0x7) << 11;
                }
                break;
            case 5:
                BaseAddrSpriteAttribute = (data & 0x7F) << 7;
                break;
            case 6:
                BaseAddrSpritePatternTable = (data & 0x7) << 11;
                break;
            case 7:
                ColorTextFG = (data & 0xF0) >> 4;
                ColorTextBG = (data & 0xF);
                logger.info("Set colors FG "+ColorTextFG+" BG "+ColorTextBG);
                break;
            default:
                break;
        }
        VDPWriteState = VDP_WRITE_STATE.START;

        notifyListeners();
    }

    @Override
    public String toString() {
        return null;
    }

    private void writeVRAM(int address, int data) throws MemoryAccessException {
        if (address >= VramSizeInBytes) throw new MemoryAccessException("No VRAM at address " + address);
        vram[address] = data & 0xFF;
        //logger.info("Write VRAM 0x"+Integer.toHexString(address)+" -> 0x"+Integer.toHexString(data));
    }

    public int readVRAM(int address) throws MemoryAccessException {
        if (address >= VramSizeInBytes) throw new MemoryAccessException("No VRAM at address " + address);
        int data = vram[address];
        //logger.info("Read VRAM 0x"+Integer.toHexString(address)+" -> 0x"+Integer.toHexString(data));
        return data;
    }
	
	public Color getBackdropColor() {
		return colors[ColorTextBG];
	}
	public Color getForegroundColor() {
		return colors[ColorTextFG];
	}
    public Color convertColorGetFG(int colbyte)
    {
        return colors[(colbyte & 0xF0)>>4];
    }
    public Color convertColorGetBG(int colbyte)
    {
        return colors[(colbyte & 0xF)];
    }

	public int getVDPMode() {
		return DisplayMode;
	}
    
    public int getNameTableVAddr() {
        return BaseAddrNameTable;
    }
    public int getColorTableVAddr() {
        return BaseAddrColorTable;
    }
    public int getPatternTableVAddr() {
        return BaseAddrPatternTable;
    }
    public int getSpriteAttribTableVAddr() {
        return BaseAddrSpriteAttribute;
    }
    public int getSpritePatternTableVAddr() {
        return BaseAddrSpritePatternTable;
    }
    public int getRegister(int reg) {
        if (reg < 0 || reg > 7)
        {
            logger.error("Bad register number "+reg);
            return 0;
        }
        return Registers[reg];
    }
    public boolean getSpriteLarge() {
        return SpriteLarge;
    }
    public boolean getSpriteMagnify() {
        return SpriteDouble;
    }

    public void setFifthSprite(int sprite)
    {
        // only set fifth sprite if it is not already set
        if ((StatusReg & 0x40) == 0)
        {
            StatusReg |= 0x40;
            StatusReg |= (sprite & 0x1F);
        }
    }
    public void setCoincidenceFlag()
    {
        StatusReg |= 0x20;
    }

    public void setInterruptFlag()
    {
        StatusReg |= 0x80;
    }
    public boolean IsInterrupted()
    {
        return ( (StatusReg & 0x80) != 0);
    }
}
