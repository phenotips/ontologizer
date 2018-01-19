/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.filter.GeneFilter;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

/**
 * Class used for editing genes.
 *
 * @author Sebastian Bauer
 */
public class GeneEditor extends Composite
{
    private static Logger logger = Logger.getLogger(GeneFilter.class.getCanonicalName());

    private static String currentImportStudyFileName;

    private Ontology graph;

    private AssociationContainer assoc;

    private GeneFilter gfilter;

    private Shell tipShell;

    private StyledText tipShellStyledText;

    private String staticToolTipText;

    private StyledText text;

    private Composite setButtonComposite;

    private Button setAllButton;

    private Button setAppendButton;

    private Button setClearButton;

    private FontData data;

    private Font smallFont;

    private WorkSet displayedWorkSet;

    private ISimpleAction datasetsLoadedAction;

    private GraphWindow graphWindow;

    public interface INewNameListener
    {
        public void newName(String name);
    }

    private List<INewNameListener> newNameListeners;

    /** The anchor of the tooltip (as in carret) */
    private int tooltipCarret = -1;

    /**
     * Constructor for the GeneEditor class.
     *
     * @param parent
     * @param style
     */
    public GeneEditor(Composite parent, int style)
    {
        super(parent, style);

        this.newNameListeners = new ArrayList<>();

        this.tipShell = new Shell(parent.getShell(), SWT.ON_TOP | SWT.TOOL);
        this.tipShell.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        FillLayout fl = new FillLayout();
        fl.marginHeight = 2;
        fl.marginWidth = 2;
        this.tipShell.setLayout(fl);
        this.tipShellStyledText = new StyledText(this.tipShell, SWT.WRAP);
        this.tipShellStyledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        this.tipShellStyledText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        this.graphWindow = new GraphWindow(parent.getDisplay());

        this.data = getShell().getFont().getFontData()[0];
        this.smallFont = new Font(getShell().getDisplay(), this.data.getName(), this.data.getHeight() * 9 / 10,
            this.data.getStyle());

        /* The composite's contents */
        setLayout(new GridLayout());

        this.text = new StyledText(this, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
        this.text.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridLayout gridLayout5 = new GridLayout();
        gridLayout5.numColumns = 3; // Generated
        gridLayout5.marginHeight = 0; // Generated
        gridLayout5.marginWidth = 0; // Generated
        gridLayout5.horizontalSpacing = 2; // Generated

        /* set button composite */
        this.setButtonComposite = new Composite(this, SWT.NONE);
        this.setButtonComposite.setLayout(gridLayout5);
        this.setButtonComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));

        this.setAppendButton = new Button(this.setButtonComposite, SWT.NONE);
        this.setAppendButton.setText("Append Set...");
        this.setAppendButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.setAppendButton.setToolTipText(
            "Opens a file dialog where an ASCII file can be choosen whose contents is appended to the gene set editor.");
        this.setAppendButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
                if (currentImportStudyFileName != null) {
                    File f = new File(currentImportStudyFileName);
                    fileDialog.setFilterPath(f.getParent());
                    fileDialog.setFileName(f.getName());
                }

                String fileName = fileDialog.open();
                if (fileName != null) {
                    appendFileContents(fileName);
                }
            }
        });

        this.setAllButton = new Button(this.setButtonComposite, SWT.NONE);
        this.setAllButton.setText("Take Them All");
        this.setAllButton.setToolTipText("Pastes all available identifieres");
        this.setAllButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.setAllButton.setEnabled(false);
        this.setAllButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (GeneEditor.this.text.getContent().getCharCount() != 0 && GeneEditor.this.assoc != null) {
                    MessageBox mb = new MessageBox(GeneEditor.this.setAllButton.getShell(), SWT.YES | SWT.NO);
                    mb.setMessage(
                        "Do you really want to replace the current content\nwith all available identifieres?");
                    if (mb.open() == SWT.NO) {
                        return;
                    }
                }
                StringBuilder str = new StringBuilder();

                for (ByteString g2a : GeneEditor.this.assoc.getAllAnnotatedGenes()) {
                    str.append(g2a.toString());
                    str.append("\n");
                }
                GeneEditor.this.text.setText(str.toString());
            }
        });

        this.setClearButton = new Button(this.setButtonComposite, SWT.NONE);
        this.setClearButton.setText("Clear");
        this.setClearButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.setClearButton.setToolTipText("Erases all the contents of the gene set editor.");
        this.setClearButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                clear();
            }
        });

        /* Listeners */

        this.text.addLineStyleListener(new LineStyleListener()
        {
            @Override
            public void lineGetStyle(LineStyleEvent event)
            {
                if (GeneEditor.this.assoc != null) {
                    String gene = getGeneName(event.lineText);
                    Gene2Associations gene2Associations = getG2A(new ByteString(gene));
                    if (gene2Associations != null) {
                        event.styles = new StyleRange[1];
                        event.styles[0] = new StyleRange(event.lineOffset, gene.length(), null, null);
                        event.styles[0].fontStyle = SWT.BOLD;
                    }
                }

            }
        });
        this.text.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.F2 && GeneEditor.this.graph != null && GeneEditor.this.assoc != null) {
                    int carret;

                    /* If a tooltip is active we use the stored anchor */
                    if (GeneEditor.this.tooltipCarret != -1) {
                        carret = GeneEditor.this.tooltipCarret;
                    } else {
                        carret = GeneEditor.this.text.getCaretOffset();
                    }

                    int lineIdx = GeneEditor.this.text.getLineAtOffset(carret);

                    int offset1 = GeneEditor.this.text.getOffsetAtLine(lineIdx);
                    int offset2;

                    if (lineIdx < GeneEditor.this.text.getLineCount() - 1) {
                        offset2 = GeneEditor.this.text.getOffsetAtLine(lineIdx + 1) - 1;
                    } else {
                        offset2 = GeneEditor.this.text.getCharCount() - 1;
                    }

                    if (offset1 <= offset2 && offset2 < GeneEditor.this.text.getCharCount()) {
                        String line = GeneEditor.this.text.getText(offset1, offset2).trim();
                        String geneName = getGeneName(line);

                        Gene2Associations gene2Associations = getG2A(new ByteString(geneName));
                        if (gene2Associations != null) {
                            Set<TermID> set = new HashSet<>();
                            for (Association a : gene2Associations) {
                                set.add(a.getTermID());
                            }

                            if (set.size() > 0) {
                                GeneEditor.this.graphWindow.setVisibleTerms(GeneEditor.this.graph, set);
                                GeneEditor.this.graphWindow.setVisible(true);
                            }
                        }
                    }
                } else {
                    GeneEditor.this.tooltipCarret = -1;
                    GeneEditor.this.tipShell.setVisible(false);
                }

            }
        });

        Menu contextMenu = new Menu(this.text);
        MenuItem cutItem = new MenuItem(contextMenu, 0);
        cutItem.setText("Cut");
        cutItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                GeneEditor.this.text.cut();
            }
        });

        MenuItem copyItem = new MenuItem(contextMenu, 0);
        copyItem.setText("Copy");
        copyItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                GeneEditor.this.text.copy();
            }
        });

        MenuItem pasteItem = new MenuItem(contextMenu, 0);
        pasteItem.setText("Paste");
        pasteItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                GeneEditor.this.text.paste();
            }
        });

        MenuItem eraseItem = new MenuItem(contextMenu, 0);
        eraseItem.setText("Erase");
        eraseItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Point sel = GeneEditor.this.text.getSelection();
                GeneEditor.this.text.replaceTextRange(sel.x, sel.y - sel.x, "");
            }
        });

        MenuItem selectAllItem = new MenuItem(contextMenu, 0);
        selectAllItem.setText("Select All");
        selectAllItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                GeneEditor.this.text.selectAll();
            }
        });
        this.text.setMenu(contextMenu);

        this.text.addMouseMoveListener(new MouseMoveListener()
        {
            @Override
            public void mouseMove(MouseEvent e)
            {
                if (GeneEditor.this.tipShell.isVisible()) {
                    GeneEditor.this.tipShell.setVisible(false);
                }
            }
        });
        this.text.addMouseTrackListener(new MouseTrackAdapter()
        {
            @Override
            public void mouseHover(MouseEvent e)
            {
                boolean visible = false;

                Point p = GeneEditor.this.text.toDisplay(e.x, e.y);

                if (GeneEditor.this.assoc == null) {
                    displayStaticToolTip(p.x, p.y);
                    return;
                }

                try {
                    int lineIdx = GeneEditor.this.text.getLineIndex(e.y);
                    int offset1 = GeneEditor.this.text.getOffsetAtLine(lineIdx);
                    int offset2;

                    if (lineIdx < GeneEditor.this.text.getLineCount() - 1) {
                        offset2 = GeneEditor.this.text.getOffsetAtLine(lineIdx + 1) - 1;
                    } else {
                        offset2 = GeneEditor.this.text.getCharCount() - 1;
                    }

                    if (offset1 <= offset2 && offset2 < GeneEditor.this.text.getCharCount()) {
                        String line = GeneEditor.this.text.getText(offset1, offset2).trim();
                        String geneName = getGeneName(line);

                        int offsetAtLocation = GeneEditor.this.text.getOffsetAtLocation(new Point(e.x, e.y));
                        if ((offsetAtLocation - offset1) < geneName.length()) {
                            Gene2Associations gene2Associations = getG2A(new ByteString(geneName));
                            if (gene2Associations != null) {
                                StringBuilder str = new StringBuilder();

                                int size = gene2Associations.getAssociations().size();

                                str.append(geneName + " has " + size + " direct ");
                                if (size == 1) {
                                    str.append("annotation.");
                                } else {
                                    str.append("annotations.");
                                }

                                int count = 0;
                                for (Association ga : gene2Associations) {
                                    str.append("\n");

                                    Term t = GeneEditor.this.graph.getTerm(ga.getTermID());
                                    if (t != null) {
                                        str.append(t.getName());
                                        str.append(" (");
                                    }
                                    str.append(ga.getTermID().toString());
                                    if (t != null) {
                                        str.append(")");
                                    }

                                    count++;
                                }

                                GeneEditor.this.tipShellStyledText.setText(str.toString());
                                GeneEditor.this.tipShellStyledText.append("\n");

                                /* Add bullets */
                                StyleRange sr = new StyleRange();
                                sr.metrics = new GlyphMetrics(0, 0, 10);
                                Bullet b = new Bullet(sr);
                                GeneEditor.this.tipShellStyledText.setLineBullet(1, count, b);

                                /* Add info */
                                int start = GeneEditor.this.tipShellStyledText.getCharCount();
                                GeneEditor.this.tipShellStyledText.append("Press 'F2' for induced graph.");
                                int end = GeneEditor.this.tipShellStyledText.getCharCount();

                                sr = new StyleRange();
                                sr.font = GeneEditor.this.smallFont;
                                sr.start = start;
                                sr.length = end - start;
                                GeneEditor.this.tipShellStyledText.setStyleRange(sr);

                                GeneEditor.this.tipShellStyledText.setLineAlignment(count + 1, 1, SWT.RIGHT);

                                GeneEditor.this.tipShell.layout();
                                GeneEditor.this.tipShell.pack();
                                GeneEditor.this.tipShell.setLocation(p.x - GeneEditor.this.tipShell.getSize().x / 2,
                                    p.y + GeneEditor.this.text.getLineHeight(offset1));

                                visible = true;
                            }

                        }
                    }
                } catch (IllegalArgumentException ex) {
                }
                GeneEditor.this.tipShell.setVisible(visible);

                /* If no tooltip is displayed, display the standard one */
                if (visible == false) {
                    displayStaticToolTip(p.x, p.y);
                }
            }
        });
        /* Turn the attachment into a drop target, we support link operations only */
        DropTarget attachmentDropTarget = new DropTarget(this.text, DND.DROP_COPY | DND.DROP_DEFAULT);
        attachmentDropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        attachmentDropTarget.addDropListener(new DropTargetAdapter()
        {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                if (event.detail == DND.DROP_DEFAULT) {
                    event.detail = DND.DROP_COPY;
                }
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                String[] names = (String[]) event.data;

                for (String name : names) {
                    appendFileContents(name);
                }
            }
        });

        addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                GeneEditor.this.smallFont.dispose();
            }
        });
    }

    /**
     * Returns the total number of entries.
     *
     * @return
     */
    public int getNumberOfEntries()
    {
        int chars = this.text.getCharCount();
        int lineCount = this.text.getLineCount();
        if (chars > 0) {
            String last = this.text.getText(chars - 1, chars - 1);
            if (last.equals("\n")) {
                lineCount--;
            }
        }
        return lineCount;
    }

    /**
     * Returns the gene association associated with the given gene. May employ the filter.
     *
     * @param gene
     * @return
     */
    private Gene2Associations getG2A(ByteString gene)
    {
        Gene2Associations gene2Associations = this.assoc.get(gene);
        if (gene2Associations == null && this.gfilter != null) {
            gene = this.gfilter.mapGene(gene);
            gene2Associations = this.assoc.get(gene);
        }
        return gene2Associations;
    }

    /**
     * Returns the number of known entries.
     *
     * @return
     */
    public int getNumberOfKnownEntries()
    {
        int known = -1;

        if (this.assoc != null) {
            known = 0;
            for (String l : getLines()) {
                String gene = getGeneName(l);
                Gene2Associations gene2Associations = getG2A(new ByteString(gene));
                if (gene2Associations != null) {
                    known++;
                }
            }
        }

        return known;
    }

    public String getText()
    {
        return this.text.getText();
    }

    public void setText(String text)
    {
        this.text.setText(text);
    }

    /**
     * Returns whether the document is empty.
     *
     * @return
     */
    public boolean isEmpty()
    {
        return this.text.getCharCount() == 0;
    }

    /**
     * Returns the genename part of the given line.
     *
     * @param line
     * @return
     */
    private String getGeneName(String line)
    {
        /* If full line is gene name than we accept it as gene name */
        if (this.assoc.containsGene(new ByteString(line.trim()))) {
            return line;
        }

        int pos = line.indexOf(' ');
        if (pos == -1) {
            pos = line.indexOf('\t');
        }
        if (pos != -1) {
            line = line.substring(0, pos);
        }

        return line;
    }

    /**
     * Appends the contents of the given file.
     *
     * @param name
     */
    public void appendFileContents(String name)
    {
        boolean wasEmpty = this.text.getCharCount() == 0;

        BufferedReader is;
        try {
            is = new BufferedReader(new FileReader(name));
            StringBuilder sb = new StringBuilder();
            String inputLine;

            while ((inputLine = is.readLine()) != null) {
                sb.append(inputLine);
                sb.append("\n");
            }
            this.text.append(sb.toString());

            if (wasEmpty) {
                for (INewNameListener newNameListener : this.newNameListeners) {
                    newNameListener.newName(new File(name).getName());
                }
            }
        } catch (Exception e) {
            Ontologizer.logException(e);
        }
    }

    /**
     * Sets the given option on which base the genes are highlighted.
     *
     * @param ws
     * @param mappingFile
     */
    public void setWorkSet(WorkSet ws, final String mappingFile)
    {
        if (this.displayedWorkSet != null) {
            WorkSetLoadThread.releaseDatafiles(this.displayedWorkSet);
        }

        this.displayedWorkSet = ws.clone();
        this.graph = null;
        this.assoc = null;
        this.gfilter = null;
        this.setAllButton.setEnabled(false);

        WorkSetLoadThread.obtainDatafiles(ws,
            new Runnable()
            {
                @Override
                public void run()
                {
                    GeneEditor.this.text.getDisplay().asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            GeneEditor.this.graph =
                                WorkSetLoadThread.getGraph(GeneEditor.this.displayedWorkSet.getOboPath());
                            GeneEditor.this.assoc = WorkSetLoadThread
                                .getAssociations(GeneEditor.this.displayedWorkSet.getAssociationPath());
                            try {
                                if (mappingFile != null && mappingFile.length() != 0) {
                                    GeneEditor.this.gfilter = new GeneFilter(new File(mappingFile));
                                }
                            } catch (FileNotFoundException e) {
                                logger.log(Level.WARNING, "Couldn't load the mapping file", e);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Couldn't load the mapping file", e);
                            }
                            if (GeneEditor.this.gfilter == null) {
                                logger.info("Ontology and associations loaded");
                            } else {
                                logger.info("Ontology, associations, and mapping loaded");
                            }

                            GeneEditor.this.text.redraw();
                            GeneEditor.this.setAllButton.setEnabled(true);
                            if (GeneEditor.this.datasetsLoadedAction != null) {
                                GeneEditor.this.datasetsLoadedAction.act();
                            }
                        }
                    });

                }
            });
    }

    /**
     * Sets the given option on which base the genes are highlighted.
     *
     * @param ws
     */
    public void setWorkSet(WorkSet ws)
    {
        setWorkSet(ws, null);
    }

    /**
     * Add an action that is called as soon as a datafile has been successfully loaded.
     *
     * @param loadedAction
     */
    public void addDatafilesLoadedListener(ISimpleAction loadedAction)
    {
        this.datasetsLoadedAction = loadedAction;
    }

    /**
     * Returns the contents as an array of lines.
     *
     * @return
     */
    public String[] getLines()
    {
        String[] genes = this.text.getText().split("\n");
        for (int i = 0; i < genes.length; i++) {
            genes[i] = genes[i].trim();
        }
        return genes;
    }

    /**
     * Clears the contents of this editor.
     */
    public void clear()
    {
        this.text.replaceTextRange(0, this.text.getCharCount(), "");
    }

    /**
     * Adds a new name listener which is called whenever the name of the gene set would change (only the case, if the
     * document was empty and a file was appended/loaded)
     *
     * @param newNameListener
     */
    public void addNewNameListener(INewNameListener newNameListener)
    {
        this.newNameListeners.add(newNameListener);
    }

    /**
     * Removes the new name listener.
     *
     * @param newNameListener
     */
    public void removeNewNameListener(INewNameListener newNameListener)
    {
        this.newNameListeners.remove(newNameListener);
    }

    /**
     * Sets a standard tool tip text which is displayed, if no other tool tip can be displayed.
     */
    @Override
    public void setToolTipText(String string)
    {
        this.staticToolTipText = string;
    }

    /**
     * Displays the static tool tip.
     *
     * @param x
     * @param y
     */
    private void displayStaticToolTip(int x, int y)
    {
        if (this.staticToolTipText != null) {
            this.tipShellStyledText.setText(this.staticToolTipText);
            this.tipShell.layout();
            this.tipShell.pack();
            this.tipShell.setLocation(x - this.tipShell.getSize().x / 2, y);
            this.tipShell.setVisible(true);
        }
    }
}
