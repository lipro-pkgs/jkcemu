/*
 * (c) 2009-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Verzeichnisses
 */

package jkcemu.base;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;


public class DirSelectDlg
		extends BasicDlg
		implements TreeSelectionListener, TreeWillExpandListener
{
  private File                   selectedFile;
  private FileTreeNodeComparator comparator;
  private FileTreeNode           rootNode;
  private DefaultTreeModel       treeModel;
  private JTree                  tree;
  private JButton                btnApprove;
  private JButton                btnCancel;
  private JButton                btnNew;


  public static File selectDirectory( Window owner, File preselection )
  {
    DirSelectDlg dlg = new DirSelectDlg( owner, preselection );
    dlg.setVisible( true );
    return dlg.selectedFile;
  }


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      /*
       * Pruefen, ob der ausgewaehlte Eintrag ein Verzeichnis ist.
       * Das ist notwendig,
       * da ein symbolischer Link ausgewaehlt sein koennte,
       * der auf etwas anderes als ein Verzeichnis zeigt.
       */
      boolean state = false;
      File    file  = getSelectedFile();
      if( file != null ) {
	state = file.isDirectory();
      }
      this.btnApprove.setEnabled( state );
      this.btnNew.setEnabled( state );
    }
  }


	/* --- TreeWillExpandListener --- */

  @Override
  public void treeWillCollapse( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    // leer
  }


  @Override
  public void treeWillExpand( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    TreePath treePath = e.getPath();
    if( treePath != null ) {
      setWaitCursor( true );
      Object o = treePath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileTreeNode ) {
	  refreshNode( (FileTreeNode) o );
	}
      }
      setWaitCursor( false );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.tree) || (src == this.btnApprove) ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.btnNew ) {
	  rv = true;
	  doNew();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  this.selectedFile = null;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Mothoden --- */

  private DirSelectDlg( Window owner, File preselection )
  {
    super( owner, "Verzeichnisauswahl" );
    this.selectedFile = null;
    this.comparator   = FileTreeNodeComparator.getIgnoreCaseInstance();


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


    // Dateibaum
    DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
    selModel.setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

    this.rootNode = new FileTreeNode( null, null, true );
    refreshNode( this.rootNode );

    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = new JTree( this.treeModel );
    this.tree.setSelectionModel( selModel );
    this.tree.setEditable( false );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    add( new JScrollPane( this.tree ), gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApprove = new JButton( "Ausw\u00E4hlen" );
    this.btnApprove.setEnabled( false );
    panelBtn.add( this.btnApprove );

    this.btnNew = new JButton( "Neu..." );
    this.btnNew.setEnabled( false );
    panelBtn.add( this.btnNew );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Listener
    this.tree.addTreeSelectionListener( this );
    this.tree.addTreeWillExpandListener( this );
    this.tree.addKeyListener( this );

    this.btnApprove.addActionListener( this );
    this.btnApprove.addKeyListener( this );

    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );

    this.btnNew.addActionListener( this );
    this.btnNew.addKeyListener( this );


    // Fenstergroesse
    setSize( 300, 300 );
    setResizable( true );
    setParentCentered();


    // vorausgewaehltes Verzeichnis einstellen
    if( preselection != null ) {
      final java.util.List<String> nameItems = new ArrayList<>();
      while( preselection != null ) {
	String s = preselection.getName();
	if( s != null ) {
	  if( s.isEmpty() ) {
	    s = null;
	  }
	}
	if( s == null ) {
	  s = preselection.getPath();
	}
	if( s == null ) {
	  break;
	}
	nameItems.add( s );
	preselection = preselection.getParentFile();
      }
      EventQueue.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    selectPath( nameItems );
			  }
			} );
    }
  }


  private void doApprove()
  {
    File file = getSelectedFile();
    if( file != null ) {
      if( file.isDirectory() ) {
	this.selectedFile = file;
	doClose();
      }
    }
  }


  private void doNew()
  {
    TreePath parentPath = this.tree.getSelectionPath();
    if( parentPath != null ) {
      Object o = parentPath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileTreeNode ) {
	  FileTreeNode parent = (FileTreeNode) o;
	  if( parent != null ) {
	    refreshNode( parent );
	    File parentFile = parent.getFile();
	    if( parentFile != null ) {
	      if( parentFile.isDirectory() ) {
		File newDir = EmuUtil.createDir( this, parentFile );
		if( newDir != null ) {
		  try {
		    FileTreeNode newNode = new FileTreeNode(
							parent,
							newDir.toPath(),
							false );
		    parent.add( newNode );
		    this.treeModel.nodeStructureChanged( parent );
		    final JTree    tree    = this.tree;
		    final TreePath newPath = parentPath.pathByAddingChild(
								  newNode );
		    EventQueue.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    tree.setSelectionPath( newPath );
				  }
				} );
		  }
		  catch( InvalidPathException ex ) {}
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private File getSelectedFile()
  {
    File     rv = null;
    TreePath tp = this.tree.getSelectionPath();
    if( tp != null ) {
      Object o = tp.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileTreeNode ) {
	  rv = ((FileTreeNode) o).getFile();
	}
      }
    }
    return rv;
  }


  private void refreshNode( FileTreeNode node )
  {
    if( !node.hasChildrenLoaded() ) {
      boolean        fsRoot  = false;
      Iterable<Path> entries = null;
      try {
	Path path = node.getPath();
	if( path != null ) {
	  if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS )
	      && !Files.isSymbolicLink( path ) )
	  {
	    entries = Files.newDirectoryStream( path );
	  }
	} else {
	  entries = FileSystems.getDefault().getRootDirectories();
	  fsRoot  = true;
	}
	for( Path tmpPath : entries ) {
	  try {
	    if( Files.isDirectory( tmpPath )
		&& (fsRoot || !Files.isHidden( tmpPath )) )
	    {
	      node.add( new FileTreeNode( node, tmpPath, fsRoot ) );
	    }
	  }
	  catch( IOException ex ) {}
	}
      }
      catch( Exception ex ) {}
      finally {
	if( entries != null ) {
	  if( entries instanceof Closeable ) {
	    EmuUtil.doClose( (Closeable) entries );
	  }
	}
      }
      node.sort( this.comparator );
      node.setChildrenLoaded( true );
    }
  }


  private void selectPath( java.util.List<String> nameItems )
  {
    TreeNode                 node     = this.rootNode;
    java.util.List<TreeNode> nodePath = new ArrayList<>();
    nodePath.add( node );
    int nameIdx = nameItems.size() - 1;
    while( (node != null) && (nameIdx >= 0) ) {
      if( node instanceof FileTreeNode ) {
	refreshNode( (FileTreeNode) node );
      }
      String name = nameItems.get( nameIdx );
      int    n    = node.getChildCount();
      for( int i = 0; i < n; i++ ) {
	TreeNode child = node.getChildAt( i );
	if( child != null ) {
	  String s = child.toString();
	  if( s != null ) {
	    if( s.equals( name ) ) {
	      nodePath.add( child );
	      node = child;
	      break;
	    }
	  }
	}
      }
      --nameIdx;
    }
    if( nodePath.size() > 1 ) {
      final TreePath tp   = new TreePath( nodePath.toArray() );
      final JTree    tree = this.tree;
      EventQueue.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    tree.expandPath( tp );
			    tree.makeVisible( tp );
			    tree.setSelectionPath( tp );
			  }
			} );
    }
  }
}
