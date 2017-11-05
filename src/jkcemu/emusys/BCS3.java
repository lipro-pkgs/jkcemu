/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BCS3
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class BCS3 extends EmuSys implements Z80CTCListener
{
  /*
   * Die beiden Tabellen mappen die BASIC-SE2.4- und BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer jede Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se24Tokens = {
	null,     null,   null,    null,		// 0xB0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,		// 0xC0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     "THEN", "AND",   null,		// 0xD0
	null,     null,   "OR",    "PEEK(",
	null,     "IN(",  "RND(",  null,
	null,     "BYTE", "END",   null,
	"REM",    null,   "GOSUB", null,		// 0xE0
	"CLEAR",  null,   "IF",    null,
	"INPUT",  null,   "PRINT", null,
	"RETURN", null,   "GOTO",  null,
	"RUN",    null,   "LIST",  null,		// 0xF0
	"LET",    null,   "LOAD",  null,
	"SAVE",   null,   "POKE",  null,
	"OUT",    null,   "NEW",   null };

  /*
   * Diese Tabelle mappt die BCS3-BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se31Tokens = {
	"LEN(",    null, "INT(",   null,		// 0xB0
	"USR(",    null, null,     null,
	"INKEY$",  null, "STEP",   null,
	"TO",      null, "CHR$",   null,
	"THEN",    null, "AND",    null,		// 0xC0
	"OR",      null, "PEEK(",  null,
	"IN(",     null, "RND",    null,
	null,      null, "END",    null,
	"REM",     null, "GOSUB",  null,		// 0xD0
	"CLS",     null, "IF",     null,
	"INPUT",   null, "PRINT",  null,
	"RESTORE", null, "RETURN", null,
	"GOTO",    null, "RUN",    null,		// 0xE0
	"LIST",    null, "LET",    null,
	"LOAD",    null, "SAVE",   null,
	"POKE",    null, "OUT",    null,
	"FOR",     null, "NEXT",   null,		// 0xF0
	"DIM",     null, "PLOT",   null,
	"UNPLOT",  null, "NEW",    null,
	"READ",    null, "DATA",   null };

  /*
   * Diese Tabelle mappt die BCS3-S/P-BASIC-3.3-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] sp33Tokens = {
	null,      null,     null,        null,		// 0xB0
	null,      null,     null,        null,
	null,      null,     null,        null,
	"USR(",    "RND(",   "PEEK(",     "LEN(",
	"INT(",    "INKEY$", "IN(",       "CHR$(",	// 0xC0
	"ASC(",    "OR",     "AND",       "THEN",
	"STEP",    "TO",     "END",       null,
	"REM",     null,     "GOSUB",     null,
	"CLS",     null,     "IF",        null,		// 0xD0
	"INPUT",   null,     "PRINT",     null,
	"RESTORE", null,     "RETURN",    null,
	"GOTO",    null,     "CALL",      null,
	"RUN",     null,     "LIST",      null,		// 0xE0
	"LET",     null,     "LOAD",      null,
	"SAVE",    null,     "POKE",      null,
	"OUT",     null,     "FOR",       null,
	"NEXT",    null,     "DIM",       null,		// 0xF0
	"PLOT",    null,     "UNPLOT",    null,
	"NEW",     null,     "RANDOMIZE", null,
	"READ",    null,     "DATA",      null };

  private static int[] endInstBytesSE24 = { 0x0F, 0x27, 0xDE, 0x1E };
  private static int[] endInstBytesSE31 = { 0x0F, 0x27, 0xCE, 0x1E };
  private static int[] endInstBytesSP33 = { 0x00, 0x27, 0xCA, 0x1E };

  private static byte[] fontBytesSE24  = null;
  private static byte[] fontBytesSE31  = null;
  private static byte[] fontBytesSP33  = null;
  private static byte[] osBytesSE24    = null;
  private static byte[] osBytesSE31_29 = null;
  private static byte[] osBytesSE31_40 = null;
  private static byte[] osBytesSP33_29 = null;
  private static byte[] mcEdtitorSE31  = null;

  private Z80CTC  ctc;
  private byte[]  fontBytes;
  private byte[]  rom0000;
  private byte[]  romF000;
  private byte[]  ram;
  private int     ramEndAddr;
  private int     screenOffset;
  private int     screenBaseHeight;
  private int     screenCharCols;
  private int     screenCharRows;
  private int     screenRowOffset;
  private int[]   kbMatrix;
  private boolean audioOutPhase;


  public BCS3( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.romF000 = null;
    this.rom0000 = readROMByProperty(
    			props,
			"jkcemu.bcs3.os.file",
			0x1000 );
    this.fontBytes = readFontByProperty(
			props,
			"jkcemu.bcs3.font.file",
			0x0800 );

    String version = EmuUtil.getProperty( props, "jkcemu.bcs3.os.version" );
    if( version.equals( "3.1" ) ) {
      if( this.fontBytes == null ) {
	if( fontBytesSE31 == null ) {
	  fontBytesSE31 = readResource( "/rom/bcs3/se31font.bin" );
	}
	this.fontBytes = fontBytesSE31;
      }
      this.screenOffset     = 0x0080;
      this.screenBaseHeight = 232;
      this.screenCharRows   = 4;	// Anfangswert
      if( is40CharsPerLineMode( props ) ) {
	this.screenCharCols  = 40;
	this.screenRowOffset = 41;
	if( this.rom0000 == null ) {
	  if( osBytesSE31_40 == null ) {
	    osBytesSE31_40 = readResource( "/rom/bcs3/se31_40.bin" );
	  }
	  this.rom0000 = osBytesSE31_40;
	}
      } else {
	this.screenCharCols  = 29;
	this.screenRowOffset = 30;
	if( this.rom0000 == null ) {
	  if( osBytesSE31_29 == null ) {
	    osBytesSE31_29 = readResource( "/rom/bcs3/se31_29.bin" );
	  }
	  this.rom0000 = osBytesSE31_29;
	}
	if( mcEdtitorSE31 == null ) {
	  mcEdtitorSE31 = readResource( "/rom/bcs3/se31mceditor.bin" );
	}
	this.romF000 = mcEdtitorSE31;
      }
    } else if( version.equals( "3.3" ) ) {
      if( this.fontBytes == null ) {
	if( fontBytesSP33 == null ) {
	  fontBytesSP33 = readResource( "/rom/bcs3/sp33font.bin" );
	}
	this.fontBytes = fontBytesSP33;
      }
      if( this.rom0000 == null ) {
	if( osBytesSP33_29 == null ) {
	  osBytesSP33_29 = readResource( "/rom/bcs3/sp33_29.bin" );
	}
	this.rom0000 = osBytesSP33_29;
      }
      this.screenOffset     = 0x00B4;
      this.screenBaseHeight = 232;
      this.screenCharRows   = 4;	// Anfangswert
      this.screenCharCols   = 29;
      this.screenRowOffset  = 30;
    } else {
      if( this.fontBytes == null ) {
	if( fontBytesSE24 == null ) {
	  fontBytesSE24 = readResource( "/rom/bcs3/se24font.bin" );
	}
	this.fontBytes = fontBytesSE24;
      }
      if( this.rom0000 == null ) {
	if( osBytesSE24 == null ) {
	  osBytesSE24 = readResource( "/rom/bcs3/se24.bin" );
	}
	this.rom0000 = osBytesSE24;
      }
      this.screenOffset     = 0x0050;
      this.screenBaseHeight = 184;
      this.screenCharCols   = 27;
      this.screenCharRows   = 12;
      this.screenRowOffset  = 28;
    }
    this.ram           = new byte[ 0x0400 ];
    this.ramEndAddr    = getRAMEndAddr( props );
    this.kbMatrix      = new int[ 10 ];
    this.audioOutPhase = false;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC" );
    cpu.setInterruptSources( this.ctc );

    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this.ctc );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static String getBasicProgram( byte[] data )
  {
    String rv = null;
    if( data != null ) {
      String[] tokens      = null;
      int      endLineNum  = -1;
      int      lastLineNum = -1;
      int      pos         = 0;
      while( pos < data.length - 3 ) {
	if( equalsRange( data, pos, endInstBytesSE24 ) ) {
	  tokens     = se24Tokens;
	  endLineNum = 9999;
	  break;
	}
	if( equalsRange( data, pos, endInstBytesSE31 ) ) {
	  tokens     = se31Tokens;
	  endLineNum = 9999;
	  break;
	}
	if( equalsRange( data, pos, endInstBytesSP33 ) ) {
	  tokens     = sp33Tokens;
	  endLineNum = 9984;
	  break;
	}
	int lineNum = EmuUtil.getWord( data, pos );
	if( lineNum <= lastLineNum ) {
	  break;
	}
	pos += 2;
	boolean found = false;
	while( !found && (pos < data.length) ) {
	  if( data[ pos++ ] == (byte) 0x1E )
	    found = true;
	}
	if( !found ) {
	  break;
	}
	lastLineNum = lineNum;
      }
      if( (tokens != null) && (endLineNum > 0) ) {
	pos = 0;
	int lineNum = EmuUtil.getWord( data, pos );
	if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	  StringBuilder buf = new StringBuilder( 0x2000 );
	  while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	    buf.append( lineNum );
	    pos += 2;
	    boolean sep = false;
	    boolean spc = true;
	    int     ch  = (int) data[ pos++ ] & 0xFF;
	    while( (ch != 0) && (ch != 0x1E) ) {
	      if( spc ) {
		buf.append( (char) '\u0020' );
		sep = false;
		spc = false;
	      }
	      if( ch >= 0xB0 ) {
		int idx = ch - 0xB0;
		if( (idx >= 0) && (idx < tokens.length) ) {
		  String s = tokens[ idx ];
		  if( s != null ) {
		    int len = s.length();
		    if( len > 0 ) {
		      if( isIdentifierChar( buf.charAt( buf.length() - 1 ) )
			  && isIdentifierChar( s.charAt( 0 ) ) )
		      {
			buf.append( (char) '\u0020' );
		      }
		      buf.append( s );
		      if( isIdentifierChar( s.charAt( len - 1 ) ) ) {
			sep = true;
		      } else {
			sep = false;
		      }
		    }
		    ch = 0;
		  }
		}
	      }
	      if( ch != 0 ) {
		if( sep
		    && (isIdentifierChar( ch )
				|| (ch == '\'')
				|| (ch == '\"')) )
		{
		  buf.append( (char) '\u0020' );
		}
		buf.append( (char) ch );
		sep = false;
	      }
	      ch = (int) data[ pos++ ] & 0xFF;
	    }
	    buf.append( (char) '\n' );
	    if( ch == 0 ) {
	      break;
	    }
	    if( lineNum == endLineNum ) {
	      break;
	    }
	    lineNum = EmuUtil.getWord( data, pos );
	  }
	  if( buf.length() > 0 ) {
	    rv = buf.toString();
	  }
	}
      }
    }
    return rv;
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return (EmuUtil.getProperty(
			props,
			"jkcemu.bcs3.os.version" ).equals( "3.1" )
		&& is40CharsPerLineMode( props )) ? 3500 : 2500;
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  this.audioOutPhase = !this.audioOutPhase;
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	  break;

	case 1:
	  this.audioOutPhase = false;
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	  this.emuThread.getZ80CPU().setWaitMode( false );
	  break;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( "BCS3" );
    if( rv ) {
      String version = EmuUtil.getProperty( props, "jkcemu.bcs3.os.version" );
      if( version.equals( "3.1" ) ) {
	if( is40CharsPerLineMode( props ) ) {
	  if( this.rom0000 != osBytesSE31_40 ) {
	    rv = false;
	  }
	} else {
	  if( this.rom0000 != osBytesSE31_29 ) {
	    rv = false;
	  }
	}
      } else if( version.equals( "3.3" ) ) {
	if( this.rom0000 != osBytesSP33_29 ) {
	  rv = false;
	}
      } else {
	if( this.rom0000 != osBytesSE24 ) {
	  rv = false;
	}
      }
    }
    if( rv ) {
      if( getRAMEndAddr( props ) != this.ramEndAddr ) {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = emuThread.getZ80CPU();
    cpu.removeTStatesListener( this.ctc );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return this.rom0000 == this.osBytesSE24 ? 0x3C50 : 0x3C80;
  }


  @Override
  public int getBorderColorIndex()
  {
    return WHITE;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = WHITE;
    if( this.fontBytes != null ) {
      int fIdx = -1;
      if( this.rom0000 == osBytesSE24 ) {
	int rPix = y % 16;
	if( rPix < 8 ) {
	  int row = y / 16;
	  int col = x / 8;
	  if( (row < this.screenCharRows) && (col < this.screenCharCols) ) {
	    int mIdx = this.screenOffset
				+ (row * this.screenRowOffset)
				+ col;
	    if( (mIdx >= 0) && (mIdx < this.ram.length) ) {
	      int ch = (int) (this.ram[ mIdx ] & 0xFF);
	      if( rPix == 7 ) {
		fIdx = (ch * 8);
	      } else {
		fIdx = (ch * 8) + rPix + 1;
	      }
	    }
	  }
	}
      } else {
	int row = y / 8;
	int col = x / 8;
	if( (row < this.screenCharRows) && (col < this.screenCharCols) ) {
	  int mIdx = this.screenOffset
				+ (row * this.screenRowOffset)
				+ col;
	  if( (mIdx >= 0) && (mIdx < this.ram.length) ) {
	    int rPix = y % 8;
	    int ch   = (int) (this.ram[ mIdx ] & 0xFF);
	    if( rPix == 7 ) {
	      fIdx = (ch * 8);
	    } else {
	      fIdx = (ch * 8) + rPix + 1;
	    }
	  }
	}
      }
      if( (fIdx >= 0) && (fIdx < this.fontBytes.length) ) {
	int m = 0x80;
	int n = x % 8;
	if( n > 0 ) {
	  m >>= n;
	}
	if( (this.fontBytes[ fIdx ] & m) != 0 ) {
	  rv = BLACK;
	}
      }
    }
    return rv;
  }


  @Override
  public int getCharColCount()
  {
    return this.screenCharCols;
  }


  @Override
  public int getCharHeight()
  {
    return 8;
  }


  @Override
  public int getCharRowCount()
  {
    return this.screenCharRows;
  }


  @Override
  public int getCharRowHeight()
  {
    return this.rom0000 == osBytesSE24 ? 16 : 8;
  }


  @Override
  public int getCharWidth()
  {
    return 8;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 80;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 200;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 60;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/bcs3.htm";
  }


  @Override
  public Integer getLoadAddr()
  {
    Integer rv = null;
    if( this.rom0000 == osBytesSE24 ) {
      rv = new Integer( 0x3DA1 );
    }
    else if( (this.rom0000 == osBytesSE31_29)
	     || (this.rom0000 == osBytesSE31_40)
	     || (this.rom0000 == osBytesSP33_29) )
    {
      rv = new Integer( getMemWord( 0x3C00 ) );
    }
    return rv;
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0x0F;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1000) && (addr < 0x1400)) {
	// Abfrage Tastatur und Kassettenrecordereingang
	rv    = 0x00;
	int a = ~addr;
	int m = 0x01;
	for( int i = 0; i < this.kbMatrix.length; i++ ) {
	  if( (a & m) != 0 ) {
	    rv |= this.kbMatrix[ i ];
	  }
	  m <<= 1;
	}
	if( this.emuThread.readAudioPhase() ) {
	  rv |= 0x80;
	} else {
	  rv &= 0x7F;
	}
      }
      else if( (addr >= 0x1800) && (addr < 0x1C00)) {
	/*
	 * Zugriff auf den Bildwiederholspeicher
	 * Beim BCS3 wird damit das Zeichen auf dem Bildschirm ausgegeben.
	 * Ist Bit 7 nicht gesetzt, liest die CPU NOP-Befehle (0x00).
	 */
	rv      = 0;
	int idx = addr - 0x1800;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  int ch = (int) (this.ram[ idx ] & 0xFF);
	  if( (ch & 0x80) != 0 ) {
	    rv = ch;
	  }
	}
      }
      else if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  rv = (int) (this.ram[ idx ] & 0xFF);
	}
      }
      else if( this.rom0000 != null ) {
	if( addr < this.rom0000.length ) {
	  rv = (int) this.rom0000[ addr ] & 0xFF;
	}
      }
    }
    else if( addr <= this.ramEndAddr ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    else if( (addr >= 0xF000) && (this.romF000 != null) ) {
      int idx = addr - 0xF000;
      if( idx < this.romF000.length ) {
	rv = (int) this.romF000[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = this.screenOffset + (chY * this.screenRowOffset) + chX;
    if( (idx >= 0) && (idx < this.ram.length) ) {
      int b = (int) this.ram[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return this.screenBaseHeight;
  }


  @Override
  public int getScreenWidth()
  {
    return this.screenCharCols * 8;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return false;
  }


  @Override
  public String getTitle()
  {
    return "BCS3";
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	this.kbMatrix[ 7 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_ENTER:
	this.kbMatrix[ 8 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_SPACE:
	this.kbMatrix[ 6 ] = 0x01;
	rv                 = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    Arrays.fill( this.kbMatrix, 0 );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv        = false;
    boolean shiftMode = false;
    int     idx       = -1;
    int     value     = 0;
    if( (ch >= 0x20) && (ch <= 0x29) ) {
      idx       = ch - 0x20;
      value     = 0x08;
      shiftMode = true;
    } else if( (ch >= 0x29) && (ch <= 0x2F) ) {
      idx       = ch - 0x26;
      value     = 0x04;
      shiftMode = true;
    } else if( (ch >= '0') && (ch <= '9') ) {
      idx   = ch - '0';
      value = 0x08;
    } else if( (ch >= 0x3A) && (ch <= 0x40) ) {
      idx       = ch - 0x3A;
      value     = 0x02;
      shiftMode = true;
    } else {
      if( (ch >= 'a') && (ch <= 'z') ) {
	ch = Character.toUpperCase( ch );
      }
      if( (ch >= 'A') && (ch <= 'J') ) {
	idx   = ch - 'A';
	value = 0x04;
      }
      else if( (ch >= 'K') && (ch <= 'T') ) {
	idx   = ch - 'K';
	value = 0x02;
      }
      else if( (ch >= 'U') && (ch <= 'Z') ) {
	idx   = ch - 'U';
	value = 0x01;
      }
    }
    if( (idx >= 0) && (idx < this.kbMatrix.length) ) {
      if( shiftMode && (idx == 9) ) {
	this.kbMatrix[ idx ] = value | 0x01;
      } else {
	if( shiftMode ) {
	  this.kbMatrix[ 9 ] = 0x01;
	}
	this.kbMatrix[ idx ] = value;
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    String   text       = null;
    String[] tokens     = null;
    int      addr       = -1;
    int      endLineNum = -1;
    if( this.rom0000 == osBytesSE24 ) {
      addr       = 0x3DA1;
      endLineNum = 9999;
      tokens     = se24Tokens;
    }
    else if( (this.rom0000 == osBytesSE31_29)
	     || (this.rom0000 == osBytesSE31_40) )
    {
      addr       = getMemWord( 0x3C00 );
      endLineNum = 9999;
      tokens     = se31Tokens;
    }
    else if( this.rom0000 == osBytesSP33_29 ) {
      addr       = getMemWord( 0x3C00 );
      endLineNum = 9984;
      tokens     = sp33Tokens;
    }
    if( (addr >= 0) && (endLineNum >= 0) && (tokens != null) ) {
      int lineNum = getMemWord( addr );
      if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	StringBuilder buf = new StringBuilder( 0x2000 );
	while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	  buf.append( lineNum );
	  addr += 2;
	  boolean sep = false;
	  boolean spc = true;
	  int     ch  = this.emuThread.getMemByte( addr++, false );
	  while( (ch != 0) && (ch != 0x1E) ) {
	    if( spc ) {
	      buf.append( (char) '\u0020' );
	      sep = false;
	      spc = false;
	    }
	    if( ch >= 0xB0 ) {
	      int idx = ch - 0xB0;
	      if( (idx >= 0) && (idx < tokens.length) ) {
		String s = tokens[ idx ];
		if( s != null ) {
		  int len = s.length();
		  if( len > 0 ) {
		    char preCh = buf.charAt( buf.length() - 1 );
		    if( (preCh != ':') && (preCh != '\u0020')
			&& isIdentifierChar( s.charAt( 0 ) ) )
		    {
		      buf.append( (char) '\u0020' );
		    }
		    buf.append( s );
		    if( isIdentifierChar( s.charAt( len - 1 ) ) ) {
		      sep = true;
		    } else {
		      sep = false;
		    }
		  }
		  ch = 0;
		}
	      }
	    }
	    if( ch != 0 ) {
	      if( sep
		  && (isIdentifierChar( ch )
				|| (ch == '\'')
				|| (ch == '\"')) )
	      {
		buf.append( (char) '\u0020' );
	      }
	      buf.append( (char) ch );
	      sep = false;
	    }
	    ch = this.emuThread.getMemByte( addr++, false );
	  }
	  buf.append( (char) '\n' );
	  if( ch == 0 ) {
	    break;
	  }
	  if( lineNum == endLineNum ) {
	    break;
	  }
	  lineNum = getMemWord( addr );
	}
	if( buf.length() > 0 ) {
	  text = buf.toString();
	}
      }
    }
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0x04) == 0 ) {		// A2=0
      rv = this.ctc.read( port & 0x03 );
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      if( (this.rom0000 == osBytesSE31_29)
	  || (this.rom0000 == osBytesSE31_40)
	  || (this.rom0000 == osBytesSP33_29) )
      {
	this.screenCharRows = 3;
      }
      initSRAM( this.ram, props );
      this.ctc.reset( true );
    } else {
      this.ctc.reset( false );
    }
    Arrays.fill( this.kbMatrix, 0 );
  }


  @Override
  public void saveBasicProgram()
  {
    int begAddr    = -1;
    int endLineNum = -1;
    if( this.rom0000 == osBytesSE24 ) {
      begAddr    = 0x3DA1;
      endLineNum = 9999;
    }
    else if( (this.rom0000 == osBytesSE31_29)
	     || (this.rom0000 == osBytesSE31_40) )
    {
      begAddr    = getMemWord( 0x3C00 );
      endLineNum = 9999;
    }
    else if( this.rom0000 == osBytesSP33_29 ) {
      begAddr    = getMemWord( 0x3C00 );
      endLineNum = 9984;
    }
    int endAddr = begAddr;
    if( (begAddr >= 0) && (endLineNum >= 0) ) {
      int lineNum = getMemWord( begAddr );
      if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	int addr = begAddr;
	while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	  addr += 2;
	  int ch = this.emuThread.getMemByte( addr++, false );
	  while( (ch != 0) && (ch != 0x1E) ) {
	    ch = this.emuThread.getMemByte( addr++, false );
	  }
	  if( ch == 0x1E ) {
	    endAddr = addr;
	  } else {
	    break;
	  }
	  lineNum = getMemWord( addr );
	}
      }
    }
    if( endAddr > begAddr + 1 ) {
      (new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr - 1,
		'B',
		false,          // kein KC-BASIC
		false,		// kein RBASIC
		"BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1400) && (addr < 0x1800)) {
	this.emuThread.getZ80CPU().setWaitMode( true );
      }
      else if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  this.ram[ idx ] = (byte) value;
	  if( ((this.rom0000 == osBytesSE31_29)
	       || (this.rom0000 == osBytesSE31_40)
	       || (this.rom0000 == osBytesSP33_29))
	      && (addr == 0x1C06) )
	  {
	    int nRows = (int) (this.ram[ 6 ] & 0xFF);
	    if( nRows != this.screenCharRows ) {
	      this.screenCharRows = nRows;
	      this.screenFrm.setScreenDirty( true );
	    }
	  }
	  if( (idx >= this.screenOffset)
	      && (idx < this.screenOffset
			+ (this.screenRowOffset * this.screenCharCols)) )
	  {
	    this.screenFrm.setScreenDirty( true );
	  }
	}
	rv = true;
      }
    }
    else if( addr <= this.ramEndAddr ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return (this.fontBytes != fontBytesSE24)
		&& (this.fontBytes != fontBytesSE31)
		&& (this.fontBytes != fontBytesSP33);
  }


  @Override
  public boolean supportsAudio()
  {
    return true;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsOpenBasic()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsSaveBasic()
  {
    return true;
  }


  @Override
  public void updSysCells(
                        int    begAddr,
                        int    len,
                        Object fileFmt,
                        int    fileType )
  {
    int   prgBegAddr   = -1;
    int[] endInstBytes = null;
    if( this.rom0000 == osBytesSE24 ) {
      prgBegAddr   = 0x3DA1;
      endInstBytes = endInstBytesSE24;
    }
    else if( (this.rom0000 == osBytesSE31_29)
	     || (this.rom0000 == osBytesSE31_40) )
    {
      prgBegAddr   = getMemWord( 0x3C00 );
      endInstBytes = endInstBytesSE31;
    }
    else if( this.rom0000 == osBytesSP33_29 ) {
      prgBegAddr   = getMemWord( 0x3C00 );
      endInstBytes = endInstBytesSP33;
    }
    if( (prgBegAddr >= 0)
	&& (begAddr == prgBegAddr)
	&& (endInstBytes != null) )
    {
      boolean state = false;
      if( fileFmt != null ) {
	if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	  if( fileType == 'B' ) {
	    state = true;
	  }
	}
	else if( fileFmt.equals( FileInfo.KCC )
		 || fileFmt.equals( FileInfo.INTELHEX )
		 || fileFmt.equals( FileInfo.BIN ) )
	{
	  state = true;
	}
      }
      if( state ) {
	for( int addr = begAddr; addr < 0x10000; addr++ ) {
	  int a = addr;
	  int b = 0;
	  while( b < endInstBytes.length ) {
	    if( ((int) endInstBytes[ b ] & 0xFF)
				!= this.emuThread.getMemByte( a, false ) )
	    {
	      b = -1;
	      break;
	    }
	    a++;
	    b++;
	  }
	  if( b == endInstBytes.length ) {
	    int prgLen = addr - begAddr + endInstBytes.length;
	    if( this.rom0000 == osBytesSE24 ) {
	      this.emuThread.setMemWord( 0x3C06, prgLen );
	    } else {
	      this.emuThread.setMemWord( 0x3C02, prgLen );
	    }
	    break;
	  }
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    if( (port & 0x04) == 0 )		// A2=0 
      this.ctc.write( port & 0x03, value );
  }


	/* --- private Methoden --- */

  private static boolean equalsRange( byte[] data, int pos, int[] pattern )
  {
    int idx = 0;
    while( (pos < data.length) && (idx < pattern.length) ) {
      if( ((int) data[ pos ] & 0xFF) != pattern[ idx ] ) {
	break;
      }
      pos++;
      idx++;
    }
    return idx == pattern.length;
  }


  private static int getRAMEndAddr( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		"jkcemu.bcs3.ram.kbyte" ).equals( "17" ) ? 0x7FFF : 0x3FFF;
  }


  private static boolean is40CharsPerLineMode( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.bcs3.chars_per_line" ).equals( "40" );
  }


  private static boolean isIdentifierChar( int ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| ((ch >= '0') && (ch <= '9'));
  }
}
