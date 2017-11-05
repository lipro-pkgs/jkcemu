/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * Emulation der Z80 PIO
 *
 * In der Betriebsart BYTE_INOUT wird das Ready-Signal von Port B,
 * welches in dem Fall fuer die Eingabe in Port A zustaendig ist,
 * nicht emuliert.
 */

package z80emu;

import java.lang.*;
import java.util.*;


public class Z80PIO implements Z80InterruptSource
{
  public enum PortInfo { A, B };

  public enum Status {
		MODE_CHANGED,
		READY_FOR_INPUT,
		OUTPUT_AVAILABLE,
		OUTPUT_CHANGED };

  public static final int MODE_BYTE_OUT   = 0;
  public static final int MODE_BYTE_IN    = 1;
  public static final int MODE_BYTE_INOUT = 2;
  public static final int MODE_BIT_INOUT  = 3;

  private static final int CTRL_NONE           = 0;
  private static final int CTRL_IO_MASK        = 1;
  private static final int CTRL_INTERRUPT_MASK = 2;

  private Z80CPU      cpu;
  private Z80PIO.Port portA;
  private Z80PIO.Port portB;


  public Z80PIO( Z80CPU cpu )
  {
    this.cpu    = cpu;
    this.portA  = new Z80PIO.Port( PortInfo.A );
    this.portB  = new Z80PIO.Port( PortInfo.B );
  }


  /*
   * Methoden, die in einem anderem Thread
   * (z.B. IO-Emulations-Thread)
   * aufgerufen werden koennen
   */
  public synchronized void addPIOPortListener(
				Z80PIOPortListener listener,
				PortInfo           portInfo )
  {
    switch( portInfo ) {
      case A:
	addPIOPortListener( listener, this.portA );
	break;

      case B:
	addPIOPortListener( listener, this.portB );
	break;
    }
  }


  public synchronized void removePIOPortListener(
				Z80PIOPortListener listener,
				PortInfo           portInfo )
  {
    switch( portInfo ) {
      case A:
	removePIOPortListener( listener, this.portA );
	break;

      case B:
	removePIOPortListener( listener, this.portB );
	break;
    }
  }


  public synchronized void reset( boolean powerOn )
  {
    this.portA.reset( powerOn );
    this.portB.reset( powerOn );
  }


  /*
   * Die beiden Methoden lesen die Werte, die die PIO zur Ausgabe
   * an den Ports bereithaelt (Seite IO-System).
   * Kann der Wert nicht gelesen werden, z.B. wenn bei Handshake
   * kein neuer Wert in das Ausgaberegister geschrieben wurde,
   * wird -1 zurueckgegeben.
   * Je nach Betriebsart wird ein Interrupt ausgeloest.
   */
  public synchronized int fetchOutValuePortA( boolean strobe )
  {
    return fetchOutValue( this.portA, strobe );
  }

  public synchronized int fetchOutValuePortB( boolean strobe )
  {
    return fetchOutValue( this.portB, strobe );
  }


  public synchronized int getModePortA()
  {
    return this.portA.mode;
  }

  public synchronized int getModePortB()
  {
    return this.portB.mode;
  }


  public synchronized boolean isReadyPortA()
  {
    return this.portA.ready;
  }


  public synchronized boolean isReadyPortB()
  {
    return this.portB.ready;
  }


  /*
   * Die beiden Methoden setzen die Werte an den Ports,
   * die die PIO uebernehmen soll (Seite IO-System).
   * Je nach Betriebsart wird ein Interrupt ausgeloest.
   *
   * Die anderen beiden Methoden dienen dazu,
   * nur die Werte einzelner Bits zu setzen.
   * Die zu setzenden Bits werden mit dem Parameter "mask" maskiert.
   * Ein Handshake wird nicht simuliert.
   *
   * Rueckgabewert:
   *	true:	Daten durch PIO uebernommen
   *	false:	Daten nicht uebernommen
   *		(CPU hat den vorherigen Wert noch nicht gelesen.)
   */
  public synchronized boolean putInValuePortA( int value, boolean strobe )
  {
    return putInValue( this.portA, value, 0xFF, strobe );
  }

  public synchronized boolean putInValuePortA( int value, int mask )
  {
    return putInValue( this.portA, value, mask, false );
  }

  public synchronized boolean putInValuePortB( int value, boolean strobe )
  {
    return putInValue( this.portB, value, 0xFF, strobe );
  }

  public synchronized boolean putInValuePortB( int value, int mask )
  {
    return putInValue( this.portB, value, mask, false );
  }


  /*
   * Methoden, die im CPU-Emulations-Thread
   * aufgerufen werden koennen (CPU-Seite).
   */
  public synchronized int readPortA()
  {
    return readPort( this.portA );
  }


  public synchronized int readPortB()
  {
    return readPort( this.portB );
  }


  public synchronized int readControlA()
  {
    return readControl( this.portA );
  }


  public synchronized int readControlB()
  {
    return readControl( this.portB );
  }


  public synchronized void writePortA( int value )
  {
    writePort( this.portA, value );
  }


  public synchronized void writePortB( int value )
  {
    writePort( this.portB, value );
  }


  public synchronized void writeControlA( int value )
  {
    writeControl( this.portA, value );
  }


  public synchronized void writeControlB( int value )
  {
    writeControl( this.portB, value );
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  public synchronized int interruptAccepted()
  {
    int rv = 0;
    if( this.portA.interruptRequested ) {
      this.portA.interruptPending   = true;
      this.portA.interruptRequested = false;
      rv = this.portA.interruptVector;
    }
    else if( this.portB.interruptRequested ) {
      this.portB.interruptPending   = true;
      this.portB.interruptRequested = false;
      rv = this.portB.interruptVector;
    }
    return rv;
  }


  public synchronized void interruptFinished()
  {
    if( this.portA.interruptPending ) {
      this.portA.interruptPending      = false;
      this.portA.interruptRequested    = false;
      this.portA.interruptCondRealized = false;
    }
    else if( this.portB.interruptPending ) {
      this.portB.interruptPending      = false;
      this.portB.interruptRequested    = false;
      this.portB.interruptCondRealized = false;
    }
  }


  public boolean isInterruptPending()
  {
    return this.portA.interruptPending || this.portB.interruptPending;
  }


  public boolean isInterruptRequested()
  {
    boolean rv = (this.portA.interruptEnabled
				&& this.portA.interruptRequested);
    if( !rv && !this.portA.interruptPending ) {
      rv = (this.portB.interruptEnabled && this.portB.interruptRequested);
    }
    return rv;
  }


  public void reset()
  {
    reset( false );
  }


	/* --- private Methoden --- */

  private class Port
  {
    public PortInfo                       portInfo;
    public int                            inValue;
    public int                            outValue;
    public boolean                        ready;
    public int                            mode;
    public int                            nextCtrl;
    public int                            ioMask;
    public int                            interruptVector;
    public int                            interruptMask;
    public boolean                        interruptFireAtH;
    public boolean                        interruptBitsAnd;
    public boolean                        interruptEnabled;
    public volatile boolean               interruptPending;
    public volatile boolean               interruptRequested;
    public boolean                        interruptCondRealized;
    public Collection<Z80PIOPortListener> listeners;


    public Port( PortInfo portInfo )
    {
      this.portInfo  = portInfo;
      this.listeners = null;
      reset( true );
    }

    public void reset( boolean powerOn )
    {
      if( powerOn ) {
	this.interruptVector = 0;
      }
      this.inValue               = 0xFF;
      this.outValue              = 0xFF;
      this.mode                  = MODE_BYTE_IN;
      this.nextCtrl              = CTRL_NONE;
      this.ioMask                = 0;
      this.interruptMask         = 0;
      this.interruptFireAtH      = false;
      this.interruptBitsAnd      = false;
      this.interruptEnabled      = false;
      this.interruptPending      = false;
      this.interruptRequested    = false;
      this.interruptCondRealized = false;
      this.ready                 = false;
    }
  }


  private synchronized void addPIOPortListener(
				Z80PIOPortListener listener,
				Z80PIO.Port        port )
  {
    if( port.listeners == null ) {
      port.listeners = new ArrayList<Z80PIOPortListener>();
    }
    port.listeners.add( listener );
  }


  private synchronized void removePIOPortListener(
				Z80PIOPortListener listener,
				Z80PIO.Port        port )
  {
    if( port.listeners != null )
      port.listeners.remove( listener );
  }


  private int composeBitInValue( Z80PIO.Port port )
  {
    return (port.ioMask & port.inValue)
			| ((~port.ioMask) & port.outValue);
  }


  private void tryInterrupt( Z80PIO.Port port )
  {
    if( port.interruptEnabled && !port.interruptPending )
      port.interruptRequested = true;
  }


  private int fetchOutValue( Z80PIO.Port port, boolean strobe )
  {
    int rv = -1;
    if( strobe ) {
      rv = port.outValue;
      if( ((port.portInfo == PortInfo.A)
	   && ((port.mode == MODE_BYTE_OUT)
	       || (port.mode == MODE_BYTE_INOUT)))
	  || ((port.portInfo == PortInfo.B)
	      && (port.mode == MODE_BYTE_OUT)
	      && (this.portA.mode != MODE_BYTE_INOUT)) )
      {
	port.ready = false;
	tryInterrupt( port );
      }
    } else {
      rv = port.outValue;
    }
    return rv;
  }


  private boolean putInValue(
			Z80PIO.Port port,
			int         value,
			int         mask,
			boolean     strobe )
  {
    boolean rv = false;
    switch( port.mode ) {
      case MODE_BYTE_IN:
      case MODE_BYTE_INOUT:
	port.inValue = (value & mask) | (port.inValue & ~mask);
	if( strobe
	    && ((port.portInfo == PortInfo.A)
		|| ((port.portInfo == PortInfo.B)
		    && (port.mode == MODE_BYTE_IN)
		    && (this.portA.mode != MODE_BYTE_INOUT))) )
	{
	  port.ready = false;
	  tryInterrupt( port );
	}
	rv = true;
	break;

      case MODE_BIT_INOUT:
	port.inValue = (value & mask) | (port.inValue & ~mask);
	if( port.interruptEnabled ) {
	  boolean iCondRealized = false;
	  int     ioMask        = port.ioMask & ~port.interruptMask;
	  if( ioMask != 0 ) {
	    int v = composeBitInValue( port );
	    int m = port.interruptFireAtH ? v : ~v;
	    if( port.interruptBitsAnd ) {
	      if( (m & ioMask) == ioMask ) {
		iCondRealized = true;
	      }
	    } else {
	      if( (m & ioMask) != 0 ) {
		iCondRealized = true;
	      }
	    }
	  }

	  // Interrupt nur ausloesen, wenn die Interrupt-Bedingungen
	  // vorher nicht erfuellt waren
	  if( iCondRealized && !port.interruptCondRealized ) {
	    port.interruptCondRealized = true;
	    tryInterrupt( port );
	  }
	  port.interruptCondRealized = iCondRealized;
	}
	rv = true;
	break;
    }
    return rv;
  }


  private int readPort( Z80PIO.Port port )
  {
    int value = 0xFF;
    switch( port.mode ) {
      case MODE_BYTE_OUT:
	value = port.outValue;
	break;

      case MODE_BYTE_IN:
      case MODE_BYTE_INOUT:
	value = port.inValue;
	if( (port.portInfo == PortInfo.A)
	    || ((port.portInfo == PortInfo.B)
		&& (port.mode == MODE_BYTE_IN)
		&& (this.portA.mode != MODE_BYTE_INOUT)) )
	{
	  port.ready = true;
	}
	break;

      case MODE_BIT_INOUT:
	value = composeBitInValue( port );
	break;
    }
    if( (port.mode == MODE_BYTE_IN)
	|| (port.mode == MODE_BYTE_INOUT)
	|| (port.mode == MODE_BIT_INOUT) )
    {
      informListeners( port, Status.READY_FOR_INPUT );
    }
    return value;
  }


  private int readControl( Z80PIO.Port port )
  {
    return ((port.mode << 6) | 0x3F) & 0xFF;
  }


  private void writePort( Z80PIO.Port port, int value )
  {
    int oldValue = port.outValue;
    port.outValue = value;
    if( ((port.portInfo == PortInfo.A)
	 && ((port.mode == MODE_BYTE_OUT) || (port.mode == MODE_BYTE_INOUT)))
	|| ((port.portInfo == PortInfo.B)
	    && (port.mode == MODE_BYTE_OUT)
	    && (this.portA.mode != MODE_BYTE_INOUT)) )
    {
      port.ready = true;
    }
    if( (port.mode == MODE_BYTE_OUT) || (port.mode == MODE_BYTE_INOUT) ) {
      informListeners( port, Status.OUTPUT_AVAILABLE );
    }
    else if( port.mode == MODE_BIT_INOUT ) {
      if( oldValue != value )
	informListeners( port, Status.OUTPUT_CHANGED );
    }
  }


  private void writeControl( Z80PIO.Port port, int value )
  {
    switch( port.nextCtrl ) {
      case CTRL_IO_MASK:
	port.ioMask   = value;
	port.nextCtrl = CTRL_NONE;
	break;

      case CTRL_INTERRUPT_MASK:
	port.interruptMask = value;
	port.nextCtrl      = CTRL_NONE;
	break;

      default:
	if( (value & 0x0F) == 0x0F ) {		// Betriebsart
	  int oldMode = port.mode;
	  switch( (value >> 6) & 0x03 ) {
	    case 0:
	      port.mode = MODE_BYTE_OUT;
	      if( (port.portInfo == PortInfo.A)
		  || ((port.portInfo == PortInfo.B)
		      && (this.portA.mode != MODE_BYTE_INOUT)) )
	      {
		port.ready = false;
	      }
	      break;

	    case 2:
	      port.mode = MODE_BYTE_INOUT;
	      if( port.portInfo == PortInfo.A ) {
		port.ready = false;
	      }
	      break;

	    case 3:
	      port.mode  = MODE_BIT_INOUT;
	      port.ready = false;
	      break;

	    default:
	      port.mode = MODE_BYTE_IN;
	      if( (port.portInfo == PortInfo.A)
		  || ((port.portInfo == PortInfo.B)
		      && (this.portA.mode != MODE_BYTE_INOUT)) )
	      {
		port.ready = true;
	      }
	      break;
	  }
	  if( port.mode == MODE_BIT_INOUT ) {
	    port.nextCtrl = CTRL_IO_MASK;
	  }
	  if( oldMode != port.mode ) {
	    informListeners( port, Status.MODE_CHANGED );
	    if( oldMode == MODE_BYTE_OUT ) {
	      informListeners( port, Status.READY_FOR_INPUT );
	    }
	  }
	}
	else if( (value & 0x01) == 0 ) {	// Interrupt-Vektor
	  port.interruptVector = value;
	}
	else if( (value & 0x0F) == 0x03 ) {	// Interrupt-Freigabe
	  port.interruptEnabled = ((value & 0x80) != 0);
	}
	else if( (value & 0x0F) == 0x07 ) {	// Interrupt-Steuerwort
	  if( (value & 0x10) != 0 ) {
	    port.nextCtrl = CTRL_INTERRUPT_MASK;
	  }
	  port.interruptFireAtH      = ((value & 0x20) != 0);
	  port.interruptBitsAnd      = ((value & 0x40) != 0);
	  port.interruptEnabled      = ((value & 0x80) != 0);
	  port.interruptCondRealized = false;
	}
    }
  }


  private void informListeners( Z80PIO.Port port, Status status )
  {
    Collection<Z80PIOPortListener> listeners = port.listeners;
    if( listeners != null ) {
      for( Z80PIOPortListener listener : listeners )
	listener.z80PIOPortStatusChanged( this, port.portInfo, status );
    }
  }
}

