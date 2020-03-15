package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.MemoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.event.KeyEvent;

public class Via6522Keyboard extends Via6522 {
	private char keyMatrix[];
	private char portADirection;
	private char portBDirection;
	private char portAState;
	private char portBState;

    private final static Logger logger = LoggerFactory.getLogger(Via6522Keyboard.class.getName());

    public Via6522Keyboard(int address) throws MemoryRangeException {
		super(address);
		keyMatrix = new char[8];
		for (int i=0; i<8; i++) {
			keyMatrix[i] = (char)0xFF; // default all Hi
		}
		portADirection = 0x00;
		portBDirection = 0x00;
		portAState = 0x00;
		portBState = 0x00;
	}

	public void setMatrix(int X, int Y, boolean pressed)
	{
		if(pressed)
		{
			keyMatrix[X] &= (char)((~(1 << Y)) & 0xFF); // set corresponding bit to 0
		}
		else
		{
			keyMatrix[X] |= (char)((1 << Y)&0xFF);  // set corresponding bit to 1
		}
        //logger.info("X: 0x"+Integer.toHexString(X)+" Y: 0x"+Integer.toHexString(Y)+" "+(pressed?" DOWN":"UP")+" keyMatrix[0x"+Integer.toHexString(X)+"] 0X"+Integer.toHexString(keyMatrix[X]));
	}

	public void setMatrix(int keycode, int extendedKeycode, int keyLocation, boolean down)
	{
		switch (keycode)
		{
            case 0x08:                       setMatrix(7,0,down); break; // Insert/Delete
            case 0x0A: 				         setMatrix(7,1,down); break; // RETURN
            case KeyEvent.VK_LEFT:           setMatrix(7,2,down); break; case KeyEvent.VK_RIGHT:        setMatrix(0,2,down); break;
            case KeyEvent.VK_UP:             setMatrix(7,3,down); break; case KeyEvent.VK_DOWN:         setMatrix(0,3,down); break;
            case KeyEvent.VK_F1:             setMatrix(7,4,down); break; case KeyEvent.VK_F2:           setMatrix(0,4,down); break;
            case KeyEvent.VK_F3:             setMatrix(7,5,down); break; case KeyEvent.VK_F4:           setMatrix(0,4,down); break;
            case KeyEvent.VK_F5:             setMatrix(7,6,down); break; case KeyEvent.VK_F6:           setMatrix(0,4,down); break;
            case KeyEvent.VK_F7:             setMatrix(7,7,down); break; case KeyEvent.VK_F8:           setMatrix(0,4,down); break;
            case 0x208:                      setMatrix(6,1,down); break; // £
            case KeyEvent.VK_ASTERISK:       setMatrix(6,1,down); break;
            case KeyEvent.VK_CLOSE_BRACKET:  setMatrix(6,2,down); break; case KeyEvent.VK_SEMICOLON:    setMatrix(1,2,down); break;
            case KeyEvent.VK_SLASH:          setMatrix(6,3,down); break;
            case 0x10: 
				if(keyLocation==3) { // Right shift
					setMatrix(6,4,down);
				} else {			 // Left shift
					setMatrix(1,3,down); 
				}
				break;
            case KeyEvent.VK_EQUALS:         setMatrix(6,5,down); break;
            case 0xDC:                       setMatrix(6,6,down); break; // |
            case KeyEvent.VK_HOME:           setMatrix(6,7,down); break;
            
            case KeyEvent.VK_PLUS:           setMatrix(5,0,down); break;
            case KeyEvent.VK_P:              setMatrix(5,1,down); break;
            case KeyEvent.VK_L:              setMatrix(5,2,down); break;
            case 0xBC:                       setMatrix(5,3,down); break; // COMMA
            case 0xBE:                       setMatrix(5,4,down); break; // PERIOD
            case KeyEvent.VK_OPEN_BRACKET:   setMatrix(5,5,down); break;
            case 0xDE:					     setMatrix(2,6,down); break; // @
            case KeyEvent.VK_MINUS:          setMatrix(5,7,down); break;

            case KeyEvent.VK_9:              setMatrix(4,0,down); break;
            case KeyEvent.VK_I:              setMatrix(4,1,down); break;
            case KeyEvent.VK_J:              setMatrix(4,2,down); break;
            case KeyEvent.VK_N:              setMatrix(4,3,down); break;
            case KeyEvent.VK_M:              setMatrix(4,4,down); break;
            case KeyEvent.VK_K:              setMatrix(4,5,down); break;
            case KeyEvent.VK_O:              setMatrix(4,6,down); break;
            case KeyEvent.VK_0:              setMatrix(4,7,down); break;

            case KeyEvent.VK_7:              setMatrix(3,0,down); break;
            case KeyEvent.VK_Y:              setMatrix(3,1,down); break;
            case KeyEvent.VK_G:              setMatrix(3,2,down); break;
            case KeyEvent.VK_V:              setMatrix(3,3,down); break;
            case KeyEvent.VK_B:              setMatrix(3,4,down); break;
            case KeyEvent.VK_H:              setMatrix(3,5,down); break;
            case KeyEvent.VK_U:              setMatrix(3,6,down); break;
            case KeyEvent.VK_8:              setMatrix(3,7,down); break;

            case KeyEvent.VK_5:              setMatrix(2,0,down); break;
            case KeyEvent.VK_R:              setMatrix(2,1,down); break;
            case KeyEvent.VK_D:              setMatrix(2,2,down); break;
            case KeyEvent.VK_X:              setMatrix(2,3,down); break;
            case KeyEvent.VK_C:              setMatrix(2,4,down); break;
            case KeyEvent.VK_F:              setMatrix(2,5,down); break;
            case KeyEvent.VK_T:              setMatrix(2,6,down); break;
            case KeyEvent.VK_6:              setMatrix(2,7,down); break;

            case KeyEvent.VK_3:              setMatrix(1,0,down); break;
            case KeyEvent.VK_W:              setMatrix(1,1,down); break;
            case KeyEvent.VK_A:              setMatrix(1,2,down); break;
            case 0xA0:                       setMatrix(1,3,down); break; // LSHIFT
            case KeyEvent.VK_Z:              setMatrix(1,4,down); break;
            case KeyEvent.VK_S:              setMatrix(1,5,down); break;
            case KeyEvent.VK_E:              setMatrix(1,6,down); break;
            case KeyEvent.VK_4:              setMatrix(1,7,down); break;

            case KeyEvent.VK_1:              setMatrix(0,0,down); break;
            case 0xC0:                       setMatrix(0,1,down); break;
            case 0x11:                       setMatrix(0,2,down); break; // Left control
            case KeyEvent.VK_ESCAPE:         setMatrix(0,3,down); break;
            case KeyEvent.VK_SPACE:          setMatrix(0,4,down); break;
            //case 0x5B:                       setMatrix(7,5,down); break; // Windows
            case KeyEvent.VK_Q:              setMatrix(0,6,down); break;
            case KeyEvent.VK_2:              setMatrix(0,7,down); break;

			default:
				logger.info("Unrecognised KeyCode: 0x"+Integer.toHexString(keycode)+" Ext: 0x"+Integer.toHexString(extendedKeycode)+" Loc: 0x"+Integer.toHexString(keyLocation)+" "+(down?"Down":"Up"));
		}
	}

	@Override
    public void write(int address, int data) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];
		char bdata = (char)(data&0xFF);
        switch (r) {
            case ORA:
				portAState &= (~portADirection & 0xFF);	// clear output bits
				portAState |= (bdata & portADirection);
				break;
            case ORB:
				portBState &= (~portBDirection & 0xFF);	// clear output bits
				portBState |= (bdata & portBDirection);
            case DDRA:
				// direction bitwise 1=output 0=input
				portADirection = (char)(bdata&0xFF);
				break;
            case DDRB:
				// direction bitwise 1=output 0=input
				portBDirection = (char)(bdata&0xFF);
				break;
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
            default:
        }
	}
    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];

        switch (r) {
            case ORA:
				return getKeyState(true);
            case ORB:
				return getKeyState(false);
            case DDRA:
				return (int) portADirection;
            case DDRB:
				return (int) portBDirection;
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
            default:
        }

        return 0;
	}

	private char getKeyState(boolean isPortA)
	{
		//logger.info("getKeyState: port"+(isPortA?"A":"B")+" Astate 0x"+Integer.toHexString(portAState)+" Bstate 0x"+Integer.toHexString(portBState));
		// this code only understands when one port is all input and the other port is all output
		/*
		if(isPortA && portADirection == 0) // check all bits are set as input
		{
			// port A is input port B is output
			assert(portBDirection==0xFF);
			// Check if any bit is low in the the output (i.e. being strobed) and return state from key matrix
			char keystate = (char)0xFF;
			for (int b=0;b<8;b++)
			{
				if(((portBState & (1<<b))) == 0)
				{
					keystate &= keyMatrix[b];
				}
			}
			logger.info("keyState 0x"+Integer.toHexString(keystate));
			return keystate;
		}
		else
		*/
		if(!isPortA && portBDirection == 0) // check all bits of B are set as input
		{
			// port B is input port A is output
			assert(portADirection==0xFF);
			// Check if any bit is low in the the output (i.e. being strobed) and return state from key matrix
			char keystate = (char)0xFF;
			for (int b=0;b<8;b++)
			{
				if(((portAState & (1<<b))) == 0)
				{
					//logger.info("bit "+b+" keyMatrix 0x"+Integer.toHexString(keyMatrix[b]));
					keystate &= keyMatrix[b];
				}
			}
			if(keystate!=0xFF) {
				//logger.info("keyState 0x"+Integer.toHexString(keystate));
			}
			return keystate;
		}
		return (char)0xFF;
	}
}	
