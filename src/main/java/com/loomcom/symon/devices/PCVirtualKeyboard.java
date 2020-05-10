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
        /*
        logger.info("Key: "+ch+"(0x"+Integer.toHexString(ch)+") 0x"+Integer.toHexString(key) +
                    " Ext: 0x"+Integer.toHexString(ext) +
                    " Loc: 0x"+Integer.toHexString(loc) +
                    " Mods: 0x"+Integer.toHexString(mods) +
                    (pressed ? " Pressed":" Released"));
                    */
        char trch = translate(ch, key); 
        int scancode = getscancode(key);
        try {
            if (pressed) {
                this.write(0, trch);
                this.write(1, 0);
            } else {
                this.write(0, 0xFF);
                this.write(1, trch);
            }
            this.write(2,scancode);
        } catch ( MemoryAccessException e) {
            logger.info("Virtual Keyboard Write error "+e);
            return;
        }
    }

    private char translate(char ch, int key) {
        switch (ch)
        {
            case 0x0A: return 0x0D;
        }
        switch (key)
        {
            case KeyEvent.VK_LEFT:      return 0xB0; 
            case KeyEvent.VK_DOWN:      return 0xB1;
            case KeyEvent.VK_RIGHT:     return 0xB2;
            case KeyEvent.VK_UP:        return 0xB3;
            case KeyEvent.VK_DELETE:    return 0xB4;
        }
        return ch;
    }

    private int getscancode(int key) {
        switch (key)
        {
            case KeyEvent.VK_A:         return 0x1C;
            case KeyEvent.VK_B :    return  0x32;
            case KeyEvent.VK_C :    return  0x21;
            case KeyEvent.VK_D :    return  0x23;
            case KeyEvent.VK_E :    return  0x24;
            case KeyEvent.VK_F :    return  0x2B;
            case KeyEvent.VK_G :    return  0x34;
            case KeyEvent.VK_H :    return  0x33;
            case KeyEvent.VK_I :    return  0x43;
            case KeyEvent.VK_J :    return  0x3B;
            case KeyEvent.VK_K :    return  0x42;
            case KeyEvent.VK_L :    return  0x4B;
            case KeyEvent.VK_M :    return  0x3A;
            case KeyEvent.VK_N :    return  0x31;
            case KeyEvent.VK_O :    return  0x44;
            case KeyEvent.VK_P :    return  0x4D;
            case KeyEvent.VK_Q :    return  0x15;
            case KeyEvent.VK_R :    return  0x2D;
            case KeyEvent.VK_S :    return  0x1B;
            case KeyEvent.VK_T :    return  0x2C;
            case KeyEvent.VK_U :    return  0x3C;
            case KeyEvent.VK_V :    return  0x2A;
            case KeyEvent.VK_W :    return  0x1D;
            case KeyEvent.VK_X :    return  0x22;
            case KeyEvent.VK_Y :    return  0x35;
            case KeyEvent.VK_Z :    return  0x1A;
            case KeyEvent.VK_SPACE :    return  0x29;
            case KeyEvent.VK_F1 :    return  0x05 ;
            case KeyEvent.VK_F2 :    return  0x06;
            case KeyEvent.VK_F3 :    return  0x04;
            case KeyEvent.VK_F4 :    return  0x0C;
            case KeyEvent.VK_F5 :    return  0x03;
            case KeyEvent.VK_F6 :    return  0x0B;
            case KeyEvent.VK_F7 :    return  0x02;
            case KeyEvent.VK_F8 :    return  0x0A;
            case KeyEvent.VK_F9 :    return  0x01;
            case KeyEvent.VK_F10 :    return  0x09;
            case KeyEvent.VK_F11 :    return  0x78;
            case KeyEvent.VK_F12 :    return  0x07;
            case KeyEvent.VK_TAB :    return  0x0D;
            case KeyEvent.VK_BACK_QUOTE :    return  0x0E;
            case KeyEvent.VK_1 :    return  0x16;
            case KeyEvent.VK_2 :    return  0x1E;
            case KeyEvent.VK_3 :    return  0x26;
            case KeyEvent.VK_4 :    return  0x25;
            case KeyEvent.VK_5 :    return  0x2E;
            case KeyEvent.VK_6 :    return  0x36;
            case KeyEvent.VK_7 :    return  0x3D;
            case KeyEvent.VK_8 :    return  0x3E;
            case KeyEvent.VK_9 :    return  0x46;
            case KeyEvent.VK_0 :    return  0x45;
            case KeyEvent.VK_COMMA :    return  0x41;
            case KeyEvent.VK_PERIOD :    return  0x49;
            case KeyEvent.VK_SLASH :    return  0x4A;
            case KeyEvent.VK_SEMICOLON :    return  0x4C;
            case KeyEvent.VK_MINUS :    return  0x4E;
            case KeyEvent.VK_QUOTE :    return  0x52;
            case KeyEvent.VK_OPEN_BRACKET :    return  0x54;
            case KeyEvent.VK_EQUALS :    return  0x55;
            case KeyEvent.VK_CAPS_LOCK :    return  0x58;
//            case KeyEvent.VK_RSHIFT :    return  0x59;
            case KeyEvent.VK_ENTER :    return  0x5A;
            case KeyEvent.VK_CLOSE_BRACKET :    return  0x5B;
            case KeyEvent.VK_NUMBER_SIGN :    return  0x5D;
            case KeyEvent.VK_BACK_SLASH :    return  0x61;
            case KeyEvent.VK_BACK_SPACE :    return  0x66;
            case KeyEvent.VK_ESCAPE :    return  0x76;
/*
            case KeyEvent.VK_KP0 :    return  0x70;
            case KeyEvent.VK_KP1 :    return  0x69;
            case KeyEvent.VK_KP2 :    return  0x72;
            case KeyEvent.VK_KP3 :    return  0x7A;
            case KeyEvent.VK_KP4 :    return  0x6B;
            case KeyEvent.VK_KP5 :    return  0x73;
            case KeyEvent.VK_KP6 :    return  0x74;
            case KeyEvent.VK_KP7 :    return  0x6C;
            case KeyEvent.VK_KP8 :    return  0x75;
            case KeyEvent.VK_KP9 :    return  0x7D;
            case KeyEvent.VK_KPSLASH :    return  0x6A;
            case KeyEvent.VK_KPDOT :    return  0x71;
            case KeyEvent.VK_KPPLUS :    return  0x79;
            case KeyEvent.VK_KPMINUS :    return  0x7B;
            case KeyEvent.VK_KPSTAR :    return  0x7C;
            case KeyEvent.VK_NUMLOCK :    return  0x77 ; num lock;
            case KeyEvent.VK_SCROLLOCK :    return  0x7E ; scroll lock;
            case KeyEvent.VK_PRINTSCR :    return  0x0F ; relocated Print Screen key;
            case KeyEvent.VK_PAUSE :    return  0x10 ; relocated Pause/Break key;
            case KeyEvent.VK_LFTALT :    return  0x11 ; left alt (right alt too);
            case KeyEvent.VK_LFTSHIFT :    return  0x12 ; left shift;
            case KeyEvent.VK_ALTRELEASE :    return  0x13 ; relocated Alt release code;
            case KeyEvent.VK_LEFTCTRL :    return  0x14 ; left ctrl (right ctrl too);
            case KeyEvent.VK_LFTWINDOWS :    return  0x1F ; Windows 98 menu key (left side);
            case KeyEvent.VK_CTRLBREAK :    return  0x20 ; relocated ctrl-break key;
            case KeyEvent.VK_RGTWINDOWS :    return  0x27 ; Windows 98 menu key (right side);
            case KeyEvent.VK_RGTWINDOWSOPT :    return  0x2F ; Windows 98 option key (right click, right side);
*/
            // arrow keys - ascii return from KBINPUT;
            case KeyEvent.VK_LEFT:    return  0xB0;
            case KeyEvent.VK_DOWN:    return  0xB1;
            case KeyEvent.VK_RIGHT:    return  0xB2;
            case KeyEvent.VK_UP:    return  0xB3;
            case KeyEvent.VK_DELETE :    return  0xB4;
        }
        return key;
    }
}    
