/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer BASIC-Compiler-Optionen
 */

package jkcemu.programming.basic;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.*;


public class BasicOptionsDlg extends AbstractOptionsDlg
{
  private static final int MIN_STACK_SIZE = 64;

  private static final String textBegAddr   = "Anfangsadresse:";
  private static final String textEndOfMem  = "Adresse Speicherende:";
  private static final String textArraySize =
				"Gr\u00F6\u00DFe des @-Variablen-Arrays:";

  private JTabbedPane     tabbedPane;
  private JRadioButton    btnStackSystem;
  private JRadioButton    btnStackSeparate;
  private JRadioButton    btnCheckAll;
  private JRadioButton    btnCheckNone;
  private JRadioButton    btnCheckCustom;
  private JRadioButton    btnBreakAnywhere;
  private JRadioButton    btnBreakInput;
  private JRadioButton    btnBreakNever;
  private JCheckBox       btnForceCPM;
  private JCheckBox       btnCheckStack;
  private JCheckBox       btnCheckArray;
  private JCheckBox       btnStrictAC1Basic;
  private JCheckBox       btnStrictZ1013Basic;
  private JCheckBox       btnPrintCalls;
  private JCheckBox       btnAllowLongVarNames;
  private JCheckBox       btnFormatSource;
  private JCheckBox       btnShowAsm;
  private JCheckBox       btnStructuredForNext;
  private JCheckBox       btnPreferRelJumps;
  private JTextField      fldAppName;
  private JTextField      fldArraySize;
  private JTextField      fldBegAddr;
  private JTextField      fldStackSize;
  private JLabel          labelAppName;
  private JLabel          labelBegAddr;
  private JLabel          labelBegAddrUnit;
  private JLabel          labelStackSize;
  private JLabel          labelStackUnit;
  private LimitedDocument docAppName;
  private HexDocument     docBegAddr;
  private IntegerDocument docArraySize;
  private HexDocument     docEndOfMem;
  private IntegerDocument docStackSize;


  public BasicOptionsDlg(
		Frame      owner,
		EmuThread  emuThread,
		PrgOptions options )
  {
    super( owner, emuThread, "BASIC-Compiler-Optionen" );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );


    // Bereich Allgemein
    JPanel panelGeneral = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnForceCPM = new JCheckBox(
		"Programmcode f\u00FCr ein CP/M-kompatibles"
			+ " Betriebssystem erzeugen",
		false );
    this.btnForceCPM.addActionListener( this );
    panelGeneral.add( this.btnForceCPM, gbcGeneral );

    this.labelAppName     = new JLabel( "Name des Programms:" );
    gbcGeneral.insets.top = 20;
    gbcGeneral.gridwidth  = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.labelAppName, gbcGeneral );

    this.docAppName      = new LimitedDocument( 8 );
    this.fldAppName      = new JTextField( this.docAppName, "", 8 );
    gbcGeneral.gridwidth = 2;
    gbcGeneral.gridx++;
    panelGeneral.add( this.fldAppName, gbcGeneral );

    this.labelBegAddr    = new JLabel( textBegAddr );
    gbcGeneral.gridwidth = 1;
    gbcGeneral.gridx     = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( this.labelBegAddr, gbcGeneral );

    this.fldBegAddr = new JTextField( 5 );
    this.fldBegAddr.addActionListener( this );
    this.docBegAddr = new HexDocument( this.fldBegAddr, 4, textBegAddr );
    gbcGeneral.gridx++;
    panelGeneral.add( this.fldBegAddr, gbcGeneral );

    this.labelBegAddrUnit = new JLabel( "hex" );
    gbcGeneral.gridx++;
    panelGeneral.add( this.labelBegAddrUnit, gbcGeneral );

    ButtonGroup grpStack = new ButtonGroup();

    this.btnStackSystem = new JRadioButton( "System-Stack verwenden", true );
    this.btnStackSystem.addActionListener( this );
    grpStack.add( this.btnStackSystem );
    gbcGeneral.insets.left = 20;
    gbcGeneral.gridwidth   = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx++;
    panelGeneral.add( this.btnStackSystem, gbcGeneral );

    gbcGeneral.insets.top  = 2;
    gbcGeneral.insets.left = 5;
    gbcGeneral.gridwidth   = 1;
    gbcGeneral.gridx       = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( textEndOfMem ), gbcGeneral );

    JTextField fld = new JTextField( 5 );
    fld.addActionListener( this );
    this.docEndOfMem = new HexDocument( fld, 4, textEndOfMem );
    gbcGeneral.gridx++;
    panelGeneral.add( fld, gbcGeneral );
    gbcGeneral.gridx++;
    panelGeneral.add( new JLabel( "hex" ), gbcGeneral );

    this.btnStackSeparate = new JRadioButton(
					"Eigener Stack-Bereich:",
					false );
    this.btnStackSeparate.addActionListener( this );
    grpStack.add( this.btnStackSeparate );
    gbcGeneral.insets.left = 20;
    gbcGeneral.gridwidth   = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx++;
    panelGeneral.add( this.btnStackSeparate, gbcGeneral );

    gbcGeneral.insets.left   = 5;
    gbcGeneral.insets.bottom = 5;
    gbcGeneral.gridwidth     = 1;
    gbcGeneral.gridx         = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( textArraySize ), gbcGeneral );

    fld = new JTextField( 5 );
    fld.addActionListener( this );
    this.docArraySize    = new IntegerDocument( fld, new Integer( 0 ), null );
    gbcGeneral.gridx++;
    panelGeneral.add( fld, gbcGeneral );
    gbcGeneral.gridx++;
    panelGeneral.add( new JLabel( "Variablen" ), gbcGeneral );

    this.labelStackSize = new JLabel( "Gr\u00F6\u00DFe:" );
    gbcGeneral.insets.left = 50;
    gbcGeneral.gridx++;
    panelGeneral.add( this.labelStackSize, gbcGeneral );
    
    this.fldStackSize = new JTextField( 5 );
    this.fldStackSize.addActionListener( this );
    this.docStackSize = new IntegerDocument(
					this.fldStackSize,
					new Integer( MIN_STACK_SIZE ),
					null );
    gbcGeneral.insets.left = 5;
    gbcGeneral.gridx++;
    panelGeneral.add( this.fldStackSize, gbcGeneral );
    this.labelStackUnit = new JLabel( "Bytes" );
    gbcGeneral.gridx++;
    panelGeneral.add( this.labelStackUnit, gbcGeneral );


    // Bereich Laufzeiteigenschaften
    JPanel panelCheck = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Laufzeiteigenschaften", panelCheck );

    GridBagConstraints gbcCheck = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpCheck = new ButtonGroup();

    this.btnCheckAll = new JRadioButton( "Max. Sicherheit", true );
    this.btnCheckAll.addActionListener( this );
    grpCheck.add( this.btnCheckAll );
    panelCheck.add( this.btnCheckAll, gbcCheck );

    this.btnCheckNone = new JRadioButton( "Max. Geschwindigkeit", false );
    this.btnCheckNone.addActionListener( this );
    grpCheck.add( this.btnCheckNone );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckNone, gbcCheck );

    this.btnCheckCustom = new JRadioButton( "Benutzerdefiniert", false );
    this.btnCheckCustom.addActionListener( this );
    grpCheck.add( this.btnCheckCustom );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckCustom, gbcCheck );

    ButtonGroup grpBreak = new ButtonGroup();

    this.btnBreakAnywhere = new JRadioButton(
		"CTRL-C bricht Programm ab",
		false );
    grpBreak.add( this.btnBreakAnywhere );
    gbcCheck.insets.top    = 5;
    gbcCheck.insets.bottom = 0;
    gbcCheck.insets.left   = 20;
    gbcCheck.gridy         = 0;
    gbcCheck.gridx++;
    panelCheck.add( this.btnBreakAnywhere, gbcCheck );

    this.btnBreakInput = new JRadioButton(
		"CTRL-C bricht Programm nur bei Eingaben ab",
		true );
    grpBreak.add( this.btnBreakInput );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnBreakInput, gbcCheck );

    this.btnBreakNever = new JRadioButton(
		"CTRL-C bricht Programm nicht ab",
		false );
    grpBreak.add( this.btnBreakNever );
    gbcCheck.gridy++;
    panelCheck.add( this.btnBreakNever, gbcCheck );

    this.btnCheckArray = new JCheckBox(
		"Grenzen des @-Variablen-Arrays pr\u00FCfen",
		true );
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckArray, gbcCheck );

    this.btnCheckStack = new JCheckBox(
		"Stack bez\u00FCglich GOSUB/RETURN und FOR/NEXT pr\u00FCfen",
		true );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckStack, gbcCheck );


    // Bereich Erzeugter Programmcode
    this.tabbedPane.addTab(
			"Erzeugter Programmcode",
			createCodeDestOptions() );


    // Bereich Sonstiges
    JPanel panelEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", panelEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Unterbereich Fehlervermeidung
    panelEtc.add( new JLabel( "Fehlervermeidung:" ), gbcEtc );

    this.btnPrintCalls = new JCheckBox(
				"Auf CALL-Anweisungen hinweisen",
				true );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnPrintCalls, gbcEtc );


    // Unterbereich Syntax
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Syntax:" ), gbcEtc );

    this.btnAllowLongVarNames = new JCheckBox(
		"Variablennamen mit mehr als einem Zeichen L\u00E4nge"
			+ " erlauben" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnAllowLongVarNames, gbcEtc );

    this.btnStrictAC1Basic = new JCheckBox(
	"Abweichungen von der originalen AC1-Mini-BASIC-Syntax melden" );
    gbcEtc.gridy++;
    panelEtc.add( this.btnStrictAC1Basic, gbcEtc );

    this.btnStrictZ1013Basic = new JCheckBox(
	"Abweichungen von der originalen Z1013-Tiny-BASIC-Syntax melden" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnStrictZ1013Basic, gbcEtc );


    // Unterbereich Quelltext
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Quelltext:" ), gbcEtc );

    this.btnFormatSource = new JCheckBox( "BASIC-Quelltext formatieren" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnFormatSource, gbcEtc );

    this.btnShowAsm = new JCheckBox(
			"Erzeugten Assembler-Quelltext anzeigen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnShowAsm, gbcEtc );


    // Unterbereich Optimierung
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Optimierung:" ), gbcEtc );

    this.btnStructuredForNext = new JCheckBox(
		"FOR/NEXT als strukturierte Schleife \u00FCbersetzten"
			+ " - siehe Hilfe!!!" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnStructuredForNext, gbcEtc );

    this.btnPreferRelJumps = new JCheckBox(
				"Relative Spr\u00FCnge bevorzugen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnPreferRelJumps, gbcEtc );


    // Bereich Knoepfe
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( createButtons( "Compilieren" ), gbc );


    // Vorbelegungen
    BasicOptions basicOptions = null;
    if( options != null ) {
      if( options instanceof BasicOptions ) {
	basicOptions = (BasicOptions) options;
      }
    }
    int stackSize = BasicOptions.DEFAULT_STACK_SIZE;
    if( basicOptions != null ) {
      stackSize = basicOptions.getStackSize();
      this.btnForceCPM.setSelected( basicOptions.getForceCPM() );
      this.fldAppName.setText( basicOptions.getAppName() );
      this.docBegAddr.setValue( basicOptions.getBegAddr(), 4 );
      this.docArraySize.setValue( basicOptions.getArraySize() );
      this.docEndOfMem.setValue( basicOptions.getEndOfMemory(), 4 );
      switch( basicOptions.getBreakPossibility() ) {
	case BREAK_INPUT:
	  this.btnBreakInput.setSelected( true );
	  break;
	case BREAK_NEVER:
	  this.btnBreakNever.setSelected( true );
	  break;
	default:
	  this.btnBreakAnywhere.setSelected( true );
	  break;
      }
      this.btnCheckArray.setSelected( basicOptions.getCheckArray() );
      this.btnCheckStack.setSelected( basicOptions.getCheckStack() );
      this.btnPrintCalls.setSelected( basicOptions.getPrintCalls() );
      this.btnAllowLongVarNames.setSelected(
				basicOptions.getAllowLongVarNames() );
      this.btnStrictAC1Basic.setSelected(
				basicOptions.getStrictAC1MiniBASIC() );
      this.btnStrictZ1013Basic.setSelected(
				basicOptions.getStrictZ1013TinyBASIC() );
      this.btnFormatSource.setSelected( basicOptions.getFormatSource() );
      this.btnShowAsm.setSelected( basicOptions.getShowAsm() );
      this.btnStructuredForNext.setSelected(
				basicOptions.getStructuredForNext() );
      this.btnPreferRelJumps.setSelected(
				basicOptions.getPreferRelativeJumps() );

      // Bei Aenderung der Plattform die Anfangsadresse neu setzen
      BasicCompiler.Platform lastPlatform = basicOptions.getPlatform();
      if( lastPlatform != null ) {
	EmuSys emuSys = emuThread.getEmuSys();
	if( !lastPlatform.equals( BasicCompiler.getPlatform( emuSys ) ) ) {
	  this.docBegAddr.setValue(
			BasicOptions.getDefaultBegAddr( emuSys ), 4 );
	}
      }
    } else {
      this.btnForceCPM.setSelected( false );
      this.fldAppName.setText( "MYAPP" );
      this.docBegAddr.setValue(
		BasicOptions.getDefaultBegAddr( emuThread.getEmuSys() ), 4 );
      this.docArraySize.setValue( BasicOptions.DEFAULT_ARRAY_SIZE );
      this.docEndOfMem.setValue( BasicOptions.DEFAULT_END_OF_MEM, 4 );
      this.btnBreakInput.setSelected( true );
      this.btnCheckArray.setSelected( true );
      this.btnCheckStack.setSelected( true );
      this.btnPrintCalls.setSelected( true );
      this.btnAllowLongVarNames.setSelected( false );
      this.btnStrictAC1Basic.setSelected( false );
      this.btnStrictZ1013Basic.setSelected( false );
      this.btnFormatSource.setSelected( false );
      this.btnShowAsm.setSelected( false );
      this.btnStructuredForNext.setSelected( false );
      this.btnPreferRelJumps.setSelected( true );
    }
    if( this.btnBreakInput.isSelected()
	&& this.btnCheckArray.isSelected()
	&& this.btnCheckStack.isSelected() )
    {
      this.btnCheckAll.setSelected( true );
    }
    else if( this.btnBreakNever.isSelected()
	&& !this.btnCheckArray.isSelected()
	&& !this.btnCheckStack.isSelected() )
    {
      this.btnCheckNone.setSelected( true );
    } else {
      this.btnCheckCustom.setSelected( true );
    }
    if( stackSize > 0 ) {
      this.btnStackSeparate.setSelected( true );
      this.docStackSize.setValue( stackSize );
    } else {
      this.btnStackSystem.setSelected( true );
    }
    this.docAppName.setSwapCase( true );
    updCodeDestFields( options );
    updCheckFieldsEnabled();
    updStackFieldsEnabled();
    settingsChanged();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  protected void doApply()
  {
    String labelText = null;
    try {
      labelText      = textBegAddr;
      int begAddr    = this.docBegAddr.intValue();
      int actualAddr = begAddr;
      if( this.btnForceCPM.isSelected() ) {
	actualAddr = 0x0100;
      }

      labelText     = textArraySize;
      int arraySize = this.docArraySize.intValue();

      labelText    = textEndOfMem;
      int endOfMem = this.docEndOfMem.intValue();
      if( endOfMem <= actualAddr ) {
	throw new NumberFormatException( "Wert zu klein" );
      }

      labelText     = "Stack-Gr\u00F6\u00DFe:";
      int stackSize = 0;
      if( this.btnStackSeparate.isSelected() ) {
	stackSize = this.docStackSize.intValue();
      }
      labelText = null;

      BasicOptions.BreakPossibility breakPossibility =
			BasicOptions.BreakPossibility.BREAK_ALWAYS;
      if( this.btnBreakInput.isSelected() ) {
	breakPossibility = BasicOptions.BreakPossibility.BREAK_INPUT;
      }
      else if( this.btnBreakNever.isSelected() ) {
	breakPossibility = BasicOptions.BreakPossibility.BREAK_NEVER;
      }

      BasicOptions options = new BasicOptions( this.emuThread );
      options.setForceCPM( this.btnForceCPM.isSelected() );
      options.setAppName( this.fldAppName.getText() );
      options.setBegAddr( begAddr );
      options.setArraySize( arraySize );
      options.setEndOfMemory( endOfMem );
      options.setStackSize( stackSize );
      options.setBreakPossibility( breakPossibility );
      options.setCheckArray( this.btnCheckArray.isSelected() );
      options.setCheckStack( this.btnCheckStack.isSelected() );
      options.setPrintCalls( this.btnPrintCalls.isSelected() );
      options.setAllowLongVarNames( this.btnAllowLongVarNames.isSelected() );
      options.setStrictAC1MiniBASIC( this.btnStrictAC1Basic.isSelected() );
      options.setStrictZ1013TinyBASIC(
				this.btnStrictZ1013Basic.isSelected() );
      options.setFormatSource( this.btnFormatSource.isSelected() );
      options.setShowAsm( this.btnShowAsm.isSelected() );
      options.setStructuredForNext( this.btnStructuredForNext.isSelected() );
      options.setPreferRelativeJumps( this.btnPreferRelJumps.isSelected() );
      try {
	applyCodeDestOptionsTo( options );
	this.appliedOptions = options;
	doClose();
      }
      catch( UserInputException ex ) {
	showErrorDlg( this, "Erzeugter Programmcode:\n" + ex.getMessage() );
      }
    }
    catch( NumberFormatException ex ) {
      String msg = ex.getMessage();
      if( labelText != null ) {
	msg = labelText + " " + msg;
      }
      showErrorDlg( this, msg );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = super.doAction( e );
    if( !rv && (e != null) ) {
      Object src = e.getSource();
      if( src == this.btnForceCPM ) {
	rv = true;
	settingsChanged();
      }
      else if( (src == this.btnStackSystem)
	       || (src == this.btnStackSeparate) )
      {
	rv = true;
	updStackFieldsEnabled();
      }
      else if( src == this.btnCheckAll ) {
	rv = true;
	this.btnBreakInput.setSelected( true );
	this.btnCheckArray.setSelected( true );
	this.btnCheckStack.setSelected( true );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckNone ) {
	rv = true;
	this.btnBreakNever.setSelected( true );
	this.btnCheckArray.setSelected( false );
	this.btnCheckStack.setSelected( false );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckCustom ) {
	rv = true;
	updCheckFieldsEnabled();
      } else {
	rv = true;
	if( src instanceof JTextField )
	  ((JTextField) src).transferFocus();
      }
    }
    return rv;
  }


  @Override
  public void settingsChanged()
  {
    super.settingsChanged();
    boolean stateCPM  = this.btnForceCPM.isSelected();
    boolean stateName = false;
    if( !stateCPM ) {
      BasicCompiler.Platform platform = BasicCompiler.getPlatform(
					this.emuThread.getEmuSys() );

      stateName = (platform == BasicCompiler.Platform.HUEBLERMC)
			|| (platform == BasicCompiler.Platform.KC85)
			|| (platform == BasicCompiler.Platform.Z9001);
    }
    this.labelAppName.setEnabled( stateName );
    this.fldAppName.setEditable( stateName );
    this.fldAppName.setEnabled( stateName );

    boolean stateAddr = !stateCPM;
    this.labelBegAddr.setEnabled( stateAddr );
    this.fldBegAddr.setEditable( stateAddr );
    this.fldBegAddr.setEnabled( stateAddr );
    this.labelBegAddrUnit.setEnabled( stateAddr );
  }


	/* --- private Methoden --- */

  private void updCheckFieldsEnabled()
  {
    boolean state = this.btnCheckCustom.isSelected();
    this.btnBreakAnywhere.setEnabled( state );
    this.btnBreakInput.setEnabled( state );
    this.btnBreakNever.setEnabled( state );
    this.btnCheckArray.setEnabled( state );
    this.btnCheckStack.setEnabled( state );
  }


  private void updStackFieldsEnabled()
  {
    boolean state = this.btnStackSeparate.isSelected();
    this.fldStackSize.setEnabled( state );
    this.labelStackSize.setEnabled( state );
    this.labelStackUnit.setEnabled( state );
  }
}

