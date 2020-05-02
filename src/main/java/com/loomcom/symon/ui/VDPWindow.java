// vim: ts=4 et sw=4
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

package com.loomcom.symon.ui;

import com.loomcom.symon.devices.Vdp;
import com.loomcom.symon.devices.DeviceChangeListener;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Via6522Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.lang.System.*;


/**
 * VDPWindow represents a graphics framebuffer backed by a 6545 CRTC.
 * Each time the window's VideoPanel is repainted, the video memory is
 * scanned and converted to the appropriate bitmap representation.
 * <p>
 * It may be convenient to think of this as the View (in the MVC
 * pattern sense) to the vdp's Model and Controller. Whenever the VDP
 * updates state in a way that may require the view to update, it calls
 * the <tt>deviceStateChange</tt> callback on this Window.
 */
public class VDPWindow extends JFrame implements DeviceChangeListener {

    private static final Logger logger = Logger.getLogger(VDPWindow.class.getName());

    private final int scaleX, scaleY;
    private final boolean shouldScale;

    private BufferedImage image;
    private int[] charRom;

    private Dimension dimensions;
    private Vdp vdp;

	private int VDPBorderWidth;
	private int VDPBorderHeight;
	private int VDPCharWidth;
	private int VDPScreenWidth;
	private int VDPScreenHeight;
	private int rasterWidth;
	private int rasterHeight;

	private int[] finalPixels;
	private int[] patternPlane;
	private int[] spritePlane;

    private Color backdropColor;
    private int VDPMode;

    private int VAddr_NameTable;
    private int VAddr_PatternTable;
    private int VAddr_ColorTable;
    private int VAddr_SpriteAttribTable;
    private int VAddr_SpritePatternTable;

    private boolean bSpritesEnabled;
    private boolean bLargeSprites;
    private boolean bSpriteMagnify;

    Timer updateTimer;
    private long lastUpdate;
    private int screenVertical;

    public boolean bCPUIsRunning;
    public static Cpu cpu;

    /**
     * A panel representing the composite video output, with fast Graphics2D painting.
     */
    private class VideoPanel extends JPanel implements KeyListener {

        public Via6522Keyboard keyboardVia;

        public VideoPanel() {
            addKeyListener(this);
            setFocusable(true);
            requestFocusInWindow();
        }


        @Override
        public void paintComponent(Graphics g) {
            computeFinalPixels();
			image.getRaster().setDataElements(0,0,rasterWidth, rasterHeight, finalPixels); 
            Graphics2D g2d = (Graphics2D) g;
            if (shouldScale) {
                g2d.scale(scaleX, scaleY);
            }
            g2d.drawImage(image, 0, 0, null);
        }

        @Override
        public Dimension getMinimumSize() {
            return dimensions;
        }

        @Override
        public Dimension getPreferredSize() {
            return dimensions;
        }

		@Override
		/**
		 * Handle a Key Typed event.
		 *
		 * @param keyEvent The key event.
		 */
		public void keyTyped(KeyEvent keyEvent) {
			//char keyTyped = keyEvent.getKeyChar();
			//logger.info(""+keyTyped+"");

			keyEvent.consume();
		}

		@Override
		/**
		 * Handle a Key Press event.
		 *
		 * @param keyEvent The key event.
		 */
		public void keyPressed(KeyEvent keyEvent) {
			int key = keyEvent.getKeyCode();
			int ext = keyEvent.getExtendedKeyCode();
			int loc = keyEvent.getKeyLocation();
            keyboardVia.setMatrix(key, ext, loc, true);
			keyEvent.consume();
		}

		@Override
		/**
		 * Handle a key release event.
		 *
		 * @param keyEvent The key event.
		 */
		public void keyReleased(KeyEvent keyEvent) {
			int key = keyEvent.getKeyCode();
			int ext = keyEvent.getExtendedKeyCode();
			int loc = keyEvent.getKeyLocation();
            keyboardVia.setMatrix(key, ext, loc, false);
			keyEvent.consume();
		}

        public void setKeyboardVia(Via6522Keyboard keyboardVia) {
            this.keyboardVia = keyboardVia;
        }
    }

    private VideoPanel videoPanel;

    public Via6522Keyboard keyboardVia;

    public void setKeyboardVia(Via6522Keyboard keyboardVia)
    {
        this.keyboardVia = keyboardVia;
        if (videoPanel!=null) {
            videoPanel.setKeyboardVia(this.keyboardVia);
        }
    }


    public VDPWindow(Vdp vdp, int scaleX, int scaleY) throws IOException {
        vdp.registerListener(this);

        this.vdp = vdp;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.shouldScale = (scaleX > 1 || scaleY > 1);
		this.VDPBorderWidth = 8;
		this.VDPBorderHeight = 8;
		this.VDPScreenWidth = 256;
		this.VDPScreenHeight = 192;
        this.VDPCharWidth = 8;

        this.rasterWidth = VDPBorderWidth*2 + VDPScreenWidth;
        this.rasterHeight = VDPBorderHeight*2 + VDPScreenHeight;

		this.finalPixels = new int[rasterWidth*rasterHeight];
        this.backdropColor = vdp.getBackdropColor();
		this.patternPlane = new int[VDPScreenWidth*VDPScreenHeight];
		this.spritePlane = new int[VDPScreenWidth*VDPScreenHeight];
        
        setMode(vdp);

        computeFinalPixels();

        buildImage();

        createAndShowUi();

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate( new UpdaterTask(), 0, 2);
        lastUpdate = System.currentTimeMillis();
        screenVertical = 0;

        bCPUIsRunning = false;
        
        keyboardVia = null;
    }

    private void setMode(Vdp vdp)
    {
        VDPMode = vdp.getVDPMode();
        if (VDPMode==vdp.DM_TEXT)
        {
            VDPScreenWidth = 240;
            VDPCharWidth = 6;
        } else {
            VDPScreenWidth = 256;
            VDPCharWidth = 8;
        }
        bSpritesEnabled = (VDPMode != vdp.DM_TEXT);
        bLargeSprites = vdp.getSpriteLarge();
        bSpriteMagnify = vdp.getSpriteMagnify();
    }

    public class UpdaterTask extends TimerTask {
        public void run() {
            if (isVisible() && bCPUIsRunning)
            {
                if (screenVertical == 0)
                {
                    if (vdp.InterruptEnable) 
                    {
                        //logger.info("Interrupt "+(System.currentTimeMillis()-lastUpdate));

                        if (!vdp.IsInterrupted())
                        {
                            // set VDP interrupt flag
                            vdp.setInterruptFlag();
                            // interrupt CPU
                            cpu.assertIrq();
                        }
                    }

                    lastUpdate = System.currentTimeMillis();
                }
                if (screenVertical == 2)
                {
                    //logger.info("ScreenRefresh "+(System.currentTimeMillis()-lastUpdate));
                    //deviceStateChanged();
                    repaint();
                }

                screenVertical = (screenVertical + 1)%8;
            }
        }
    }

    private int getVPos(int sprite)
    {
        int vpos = 0;
        int SABoffset = VAddr_SpriteAttribTable + sprite*4;
        int vposadj = 0;
        
        try {
            vpos = vdp.readVRAM(SABoffset);
        } catch ( MemoryAccessException e)
        {
            logger.info("VRAM read error "+e);
            return 0;
        }
        vposadj = vpos;

        // get position - vpos can be negtative - so take 2's complement
        if (vpos > 0xD0) {
            vposadj = 0-(((~vpos)& 0xFF) +1);
        }
        // vpos = -1 means top of screen, so add 1
        vposadj += 1;

        if ((vposadj<-32) || (vposadj>256))
        {
           logger.info("ERROR: Sprite "+sprite+" VPOS In "+vpos+"(0x"+Integer.toHexString(vpos)+") Out "+vposadj);
           return 0;
        }
        return vpos;
    }

    private void SetMulticolorPixel(int patoffset, int colnibble)
    {
        //if (colnibble != 0)
        {
            Color col = vdp.convertColorGetBG(colnibble);
            int packCol = getPackedCol(col);

            for (int x=0;x<4;x++)
            {
                for (int y=0;y<4;y++)
                {
                    patternPlane[patoffset + x + y*VDPScreenWidth] = packCol;
                }
            }
        }
    }
    private void computeFinalPixels()
    {
        try {
        // VDPMode is mode as defined by bits M1, M2 and M3 of VDP regs 0 and 1
        if (VDPMode == vdp.DM_GRAPHICSI) // Graphics I
        {
            // name is index into NameTable ~= 32x24 text-screen
            for (int name = 0; name < (32*24); name++)
            {
                // Calculate offset into pixelPlane for top-left of char
                int x = (name % 32) * VDPCharWidth;
                int y = (int)(name / 32)*8;
                int offset = y * 256 + x;

                // Look up the pattern (ASCII num of char) from NameTable
                int pat = vdp.readVRAM(VAddr_NameTable + name);
                // Look up color for that char
                int col = vdp.readVRAM(VAddr_ColorTable + (pat>>3));
                // Byte contains BG and FG color
                Color colBG = vdp.convertColorGetBG(col);
                int packColBG = getPackedCol(colBG);
                Color colFG = vdp.convertColorGetFG(col);
                int packColFG = getPackedCol(colFG);

                // print pattern 8x8 pixels
                for (int row = 0; row < 8; row++)
                {
                    int ch = vdp.readVRAM(VAddr_PatternTable + pat*8 + row);
                    for (int p = 0; p < 8; p++) 
                    {
                        int b = (ch << p) & 0x80;
                        patternPlane[offset + row*32*8 + p] = (b==0)?packColBG:packColFG;
                    }
                }
            }
        } else if (VDPMode == vdp.DM_TEXT) // Text
        {
            // Text mode uses standard Backdrop and Foreground colours - no colour table
            Color colBG = vdp.getBackdropColor();
            int packColBG = getPackedCol(colBG);
            Color colFG = vdp.getForegroundColor();
            int packColFG = getPackedCol(colFG);
            //logger.info("Text mode FG 0x"+Integer.toHexString(packColFG)+" BG 0x"+Integer.toHexString(packColBG));

            // name is index into NameTable = 40x24 text-screen
            for (int name = 0; name < (40*24); name++)
            {
                // Calculate offset into pixelPlane for top-left of char
                int x = (name % 40) * VDPCharWidth;
                int y = (int)(name / 40) * 8;
                int offset = y * VDPScreenWidth + x;

                // Look up the pattern (ASCII num of char) from NameTable
                int pat = vdp.readVRAM(VAddr_NameTable + name);

                // print pattern 8x8 pixels
                for (int row = 0; row < 8; row++)
                {
                    int ch = vdp.readVRAM(VAddr_PatternTable + pat*8 + row);
                    for (int p = 0; p < 6; p++) 
                    {
                        int b = (ch << p) & 0x80;
                        patternPlane[offset + row*VDPScreenWidth + p] = (b==0)?packColBG:packColFG;
                    }
                }
            }
        }
        else if (VDPMode == vdp.DM_GRAPHICSII)
        {
            // name is index into NameTable  - 256 byte sections
            for (int name = 0; name < 256*3; name++)
            {
                // Mode 2 screen is split into 3 sections of 256, for name, pattern and color tables
                int section = name / 256;

                // Calculate offset into pixelPlane for top-left of char
                int x = (name % 32) * VDPCharWidth;
                int y = ((int)(name / 32))*8;
                int offset = y * VDPScreenWidth + x;

                // Look up the pattern (ASCII num of char) from NameTable
                int pat = vdp.readVRAM(VAddr_NameTable + name);
                // print pattern 8x8 pixels
                for (int row = 0; row < 8; row++)
                {
                    // Look up pattern & color
                    int ch = vdp.readVRAM(VAddr_PatternTable + section*0x800 + pat*8 + row);
                    int col = vdp.readVRAM(VAddr_ColorTable + section*0x800 + pat*8 + row);
                    // Byte contains BG and FG color
                    int packColBG = getPackedCol(vdp.convertColorGetBG(col));
                    int packColFG = getPackedCol(vdp.convertColorGetFG(col));

                    for (int p = 0; p < 8; p++) 
                    {
                        int b = (ch << p) & 0x80;
                        patternPlane[offset + row*VDPScreenWidth + p] = (b==0)?packColBG:packColFG;
                    }
                }
            }
        }
        else if (VDPMode == vdp.DM_MULTICOLOR)
        {
            // name is index into NameTable pointing to colors in Pattern table
            for (int name = 0; name < 256*3; name++)
            {
                // Calculate offset into pixelPlane for top-left of char
                int x = (name % 32) * VDPCharWidth;
                int y = ((int)(name / 32))*8;
                int offset = y * VDPScreenWidth + x;
                int ch;

                // get pattern number
                int pat = vdp.readVRAM(VAddr_NameTable + name);

                // line of screen decides on which bytes of pattern to use for colors
                int byteoffset = (((int)(name/32))%4)*2;

                // Look up 1st of pair of bytes from Pattern
                ch = vdp.readVRAM(VAddr_PatternTable + pat*8 + byteoffset);

                // top-left
                SetMulticolorPixel(offset, ((ch & 0xF0)>>4));
                // top-right
                SetMulticolorPixel(offset+4, (ch & 0x0F));

                // Look up 2nd byte of pair
                ch = vdp.readVRAM(VAddr_PatternTable + pat*8 + byteoffset + 1);
                
                // bottom-left
                SetMulticolorPixel(offset+4*VDPScreenWidth, ((ch & 0xF0)>>4));
                // bottom-right
                SetMulticolorPixel(offset+4*VDPScreenWidth+4, (ch & 0x0F));
            }
        }

        // sprites 8x8
        // Modes 0,2,4 (not text mode)
        if (bSpritesEnabled)
        {
			int blocks = bLargeSprites?4:1;
			int mag = bSpriteMagnify?2:1;
            int spriteSize = bLargeSprites?16:8;

			/* Debug - print sprite info
			logger.info("Mode: "+vdp.getVDPMode());
			// dump sprite block
			int os = VAddr_SpriteAttribTable;
			String str = "SAB:";
			for (int i =0; i < 128; i++)
			{
				int r = vdp.readVRAM(os+i);
				if (i%4 == 0) { str += String.format("%d[",i/4); }
				str += String.format("%02X", r);
				if (i%4 == 3) { str += "] "; }
			}
			logger.info(str);
			*/

			// sprites are processed from 0 -> 31 and if vpos==0xD00 is observed, sprite processing is stopped. 
			// But, sprites have to be draw back to front (31 -> 0) to make hidden object work.
			// So, save sprites to be drawn in order in a list.
			ArrayList<Integer> sprite_list = new ArrayList<>();
            // Only 4 sprites per line, Highest priority ones first
            int[] sprites_per_line = new int[256+32]; 
            int[] fifth_sprite = new int[256+32];

			// for each sprite in SAB (stop if any vertical pos == 0xD0)
			for (int sprite =0; sprite < 32; sprite++)
			{
				int SABoffset = VAddr_SpriteAttribTable + sprite*4;
				int vpos 		= vdp.readVRAM(SABoffset);
				
				// special value in vpos means stop processing sprites
				if (vpos == 0xD0) break;

                int vpos_adjusted = getVPos(sprite);
				int setFifthSprite = 0;

                for (int i=0;i<(spriteSize*mag+1);i++)
                {
                    sprites_per_line[vpos_adjusted+32+i]++;
                    if (sprites_per_line[vpos_adjusted+32+i] >4)
                    {
                        fifth_sprite[vpos_adjusted+32+i] = sprite;
						if (setFifthSprite == 0)
						{
							vdp.setFifthSprite(sprite);
							//logger.info("FIFTH Sprite: "+sprite+" VPos "+vpos_adjusted+"");
							setFifthSprite = sprite;
						}
                    }
                }
				sprite_list.add(new Integer(sprite));
			}
			
            // Iterate through the list in reverse
            ListIterator<Integer> li = sprite_list.listIterator(sprite_list.size());
			while (li.hasPrevious())
			{
                Integer I = li.previous();
                int sprite = I.intValue();

				int SABoffset = VAddr_SpriteAttribTable + sprite*4;
                int vpos        = getVPos(sprite);
				int hpos 		= vdp.readVRAM(SABoffset + 1);
				int pattern 	= vdp.readVRAM(SABoffset + 2);
				int col 		= vdp.readVRAM(SABoffset + 3) & 0x0F;
				Color color 	= vdp.convertColorGetBG(col); // use GetBG function because colour is in low nibble
				int packCol 	= getPackedCol(color);
				int earlyclock 	= vdp.readVRAM(SABoffset + 3) & 0x80;

				// EarlyClock bit shifts sprite to left by 32 pixels
                if (earlyclock>0) {
                    hpos -= 32;
                }
				
                //logger.info("Sprite: "+sprite+" Pos "+hpos+","+vpos+
                //        " pattern "+pattern+" color "+col+" 0x"+Integer.toHexString(packCol)+
                //        " Early "+((earlyclock>0)?"T":"F")+
                //        " PAddr 0x"+Integer.toHexString(VAddr_SpritePatternTable + pattern*8)+
                //        ""+(bLargeSprites?" Large":"")+(bSpriteMagnify?" Magnify":""));

				for (int block=0; block < blocks; block++)
				{
					int SPToffset	= VAddr_SpritePatternTable + (pattern/blocks)*8*blocks +block*8;

					int x = hpos;
					int y = vpos;

					/* [ Block0 ] [ Block2 ]
					 * [ Block1 ] [ Block3 ] */
					x += (block&2)*4*mag;
					y += (block&1)*8*mag;

					//logger.info("Sprite: "+sprite+" Pos "+x+","+y+" PAddr 0x"+Integer.toHexString(SPToffset));

					for (int row = 0; row < 8; row++)
					{
						if ((y+row*mag)>=0 && (y+row*mag)<192) // dont draw pixels outside main pattern plane
						{
							int ch = vdp.readVRAM(SPToffset + row);
							//logger.info("--- row "+row+": 0x"+Integer.toHexString(ch) + " fifth "+fifth_sprite[32+y+row*mag]);
							for (int p = 0; p < 8; p++)
							{
								if ( (x+p*mag)>=0 && (x+p*mag)<VDPScreenWidth) // dont draw pixels outside main pattern plane
								{
									int b = (ch << p) & 0x80;
									if (b>0) // only set where pixel==1, otherwise it is transparent
									{
										int offset = (y+row*mag)*VDPScreenWidth + (x + p*mag);
                                        if ((fifth_sprite[32+y+row*mag]==0) || sprite<fifth_sprite[32+y+row*mag])
                                        {
											//logger.info("     * p"+p+" SP offset "+offset+ " Cur " + Integer.toHexString(spritePlane[offset]) + " -> "+Integer.toHexString(packCol));
											if (spritePlane[offset] != -1)
											{
												vdp.setCoincidenceFlag();
												//logger.info("COLLISION: Sprite: "+sprite+" Pos "+x+","+y+"");
											}
                                            spritePlane[offset] = packCol;

                                            if (bSpriteMagnify)

                                            {
												if (spritePlane[offset+1] >= 0)
												{
													vdp.setCoincidenceFlag();
												}
                                                spritePlane[offset+1] = packCol;
                                            }
                                        }
                                        if ((fifth_sprite[32+y+row*mag+1]==0) || sprite<fifth_sprite[32+y+row*mag+1])
                                        {
                                            if (bSpriteMagnify)
                                            {
												if (spritePlane[offset+VDPScreenWidth] >= 0)
												{
													vdp.setCoincidenceFlag();
												}
                                                spritePlane[offset+VDPScreenWidth] = packCol;
												if (spritePlane[offset+VDPScreenWidth+1] >= 0)
												{
													vdp.setCoincidenceFlag();
												}
                                                spritePlane[offset+VDPScreenWidth+1] = packCol;
                                            }
										}
									}
								}
							}
						}
					}
				}
			}
        }

        int colpackedtst =  getPackedCol(new Color(0xff, 0x00, 0x00, 0xff));
        int packColBG =  getPackedCol(backdropColor);
		for (int i=0;i<rasterWidth;i++) {
			for (int j=0;j<rasterHeight;j++) {
                if (i >= VDPBorderWidth && i < (rasterWidth-VDPBorderWidth) &&
                    j >= VDPBorderWidth && j < (rasterHeight-VDPBorderWidth)) 
                {
                    int x=i-VDPBorderWidth;
                    int y=j-VDPBorderWidth;
                    try {
                        if (x<VDPScreenWidth)
                        {
                            //finalPixels[j*rasterWidth+i] = patternPlane[y*VDPScreenWidth+x];
                            finalPixels[j*rasterWidth+i] = (spritePlane[y*VDPScreenWidth+x] == -1) ?
																patternPlane[y*VDPScreenWidth+x]:
																spritePlane[y*VDPScreenWidth+x];
                        }
                        else
                        {
                            finalPixels[j*rasterWidth+i] = packColBG;
                        }

                    } catch (ArrayIndexOutOfBoundsException e){
                        logger.info("ArrayIndex: patternPlane["+x+","+y+"]");
                    }
                }
                else {
                    // outside border
                    finalPixels[j*rasterWidth+i] = packColBG;
                }
			}
		}
        } catch (MemoryAccessException e)
        {
            logger.info("VRAM error "+e);
        }

		/* Clear sprite plane to transparent */
		for (int i=0; i<VDPScreenWidth*VDPScreenHeight;i++) {
			spritePlane[i] = -1;
		}
           
    }

    private int getPackedCol(Color c)
    {
        return (255<<24) | (c.getRed()<<16) | (c.getGreen()<<8) | c.getBlue();
    } 

    private String VDPModeToStr(int mode)
    {
        switch(mode) {
            case 0 : return "Graphics I";
            case 1 : return "Text";
            case 2 : return "Multicolor";
            case 4 : return "Graphics II";
            default: return "Unknown("+mode+")";
        }
    }

    /**
     * Called by the VDP on state change.
     */
    public void deviceStateChanged() {

        boolean repackNeeded = false;

        Color col = vdp.getBackdropColor();
        if (!col.equals(backdropColor))
        {
            backdropColor = col;
            logger.info("Backdrop color R "+col.getRed()+" G "+col.getGreen()+" B "+col.getBlue());
            repackNeeded = true;
        }
        int mode = vdp.getVDPMode();
        boolean magnify = vdp.getSpriteMagnify();
        boolean large  = vdp.getSpriteLarge();
        if ((mode != VDPMode) ||
            (magnify != bSpriteMagnify) ||
            (large != bLargeSprites))
        {
            setMode(vdp);
            if (bSpritesEnabled)
            {
                logger.info("Mode changed to "+VDPModeToStr(VDPMode)+" Sprites "+
                        (bLargeSprites?"16x16":"8x8")+" "+ 
                        (bSpritesEnabled?"2x2 mag":"no mag"));
            }
            else
            {
                logger.info("Mode changed to "+VDPModeToStr(VDPMode)+" (no sprites) ");
            }
            Arrays.fill(patternPlane, 0);
            repackNeeded = true;
        }
        
        int nt = VAddr_NameTable;
        int pt = VAddr_PatternTable;
        int ct = VAddr_ColorTable;
        int sa = VAddr_SpriteAttribTable;
        int sp = VAddr_SpritePatternTable;
        VAddr_NameTable = vdp.getNameTableVAddr();
        VAddr_PatternTable = vdp.getPatternTableVAddr();
        VAddr_ColorTable = vdp.getColorTableVAddr();
        VAddr_SpriteAttribTable = vdp.getSpriteAttribTableVAddr();
        VAddr_SpritePatternTable = vdp.getSpritePatternTableVAddr();
        if ( nt != VAddr_NameTable ||
            pt != VAddr_PatternTable ||
            ct != VAddr_ColorTable ||
            sa != VAddr_SpriteAttribTable ||
            sp != VAddr_SpritePatternTable)
        {
            logger.info("TableAddresses: Name: 0x"+Integer.toHexString(VAddr_NameTable)+
                        " Pattern: 0x"+Integer.toHexString(VAddr_PatternTable)+
                        " Color: 0x"+Integer.toHexString(VAddr_ColorTable)+
                        " Sprite Attr: 0x"+Integer.toHexString(VAddr_SpriteAttribTable)+
                        " Sprite Patt: 0x"+Integer.toHexString(VAddr_SpritePatternTable));
            //repackNeeded = true;
        }
        /*
        if (repackNeeded) {
            buildImage();
            invalidate();
            pack();
        }
        */
    }

    private void createAndShowUi() {
        setTitle("Composite Video");

        int borderWidth = 20;
        int borderHeight = 20;

        JPanel containerPane = new JPanel();
        containerPane.setBorder(BorderFactory.createEmptyBorder(borderHeight, borderWidth, borderHeight, borderWidth));
        containerPane.setLayout(new BorderLayout());
        containerPane.setBackground(Color.black);
       
        videoPanel = new VideoPanel(); 
        if (keyboardVia!=null){
            videoPanel.setKeyboardVia(keyboardVia);
        }
        containerPane.add(videoPanel, BorderLayout.CENTER);

        getContentPane().add(containerPane, BorderLayout.CENTER);
        setResizable(false);
        pack();
    }

    private void buildImage() {
        this.dimensions = new Dimension(rasterWidth * scaleX, rasterHeight * scaleY);
        this.image = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_INT_ARGB);
    }

}
