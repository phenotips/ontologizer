/*
 * Created on 12.04.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.SemanticResult;
import ontologizer.calculation.svd.SVDResult;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.result.AbstractResultComposite;
import ontologizer.gui.swt.result.EnrichedGOTermsComposite;
import ontologizer.gui.swt.result.IGraphAction;
import ontologizer.gui.swt.result.ITableAction;
import ontologizer.gui.swt.result.PValuesSVDGOTermsComposite;
import ontologizer.gui.swt.result.SVDGOTermsComposite;
import ontologizer.gui.swt.result.SemanticSimilarityComposite;
import ontologizer.gui.swt.support.IMinimizedAdapter;
import ontologizer.gui.swt.support.IRestoredAdapter;
import ontologizer.gui.swt.support.SWTUtil;

/**
 * The window which displays the results of an analysis.
 *
 * @author Sebastian Bauer
 */
public class ResultWindow extends ApplicationWindow
{
    private CTabFolder cTabFolder = null;

    private ToolBar toolbar = null;

    private Composite statusComposite = null;

    private Composite minimizedComposite = null;

    private Composite progressComposite = null;

    private Text progressText = null;

    private ProgressBar progressBar = null;

    private FileDialog graphOutputDialog;

    private FileDialog tableOutputDialog;

    /* No need to dispose this! */
    private Cursor appStartingCursor;

    /**
     * The constructor.
     *
     * @param display
     */
    public ResultWindow(Display display)
    {
        super(display);

        /*
         * Prevent the disposal of the window on a close event, but make the window invisible
         */
        this.shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                e.doit = false;
                ResultWindow.this.shell.setVisible(false);
            }
        });

        this.appStartingCursor = display.getSystemCursor(SWT.CURSOR_APPSTARTING);

        createSShell(display);

        this.graphOutputDialog = new FileDialog(this.shell, SWT.SAVE);
        this.tableOutputDialog = new FileDialog(this.shell, SWT.SAVE);

        this.shell.open();
    }

    public void addResults(SemanticResult sr)
    {
        boolean added = false;

        this.cTabFolder.setRedraw(false);

        CTabItem cTabItem = new CTabItem(this.cTabFolder, SWT.NONE);
        cTabItem.setText(sr.name);
        SemanticSimilarityComposite ssc = new SemanticSimilarityComposite(this.cTabFolder, 0);
        ssc.setResult(sr);
        ssc.setMinimizedAdapter(this.minimizedAdapter);
        cTabItem.setControl(ssc);
        added = true;

        if (added && this.cTabFolder.getSelectionIndex() == -1) {
            this.cTabFolder.setSelection(0);
            updateWindowTitle();
        }
        this.cTabFolder.setRedraw(true);

    }

    public void addResults(AbstractGOTermsResult result)
    {
        boolean added = false;

        this.cTabFolder.setRedraw(false);
        if (result instanceof EnrichedGOTermsResult) {
            EnrichedGOTermsResult enrichedGOTermsResult = (EnrichedGOTermsResult) result;

            CTabItem cTabItem = new CTabItem(this.cTabFolder, SWT.NONE);
            EnrichedGOTermsComposite studyResultComposite = new EnrichedGOTermsComposite(this.cTabFolder, 0);
            studyResultComposite.setResult(enrichedGOTermsResult);
            cTabItem.setText(enrichedGOTermsResult.getStudySet().getName());
            cTabItem.setControl(studyResultComposite);
            added = true;
        } else if (result instanceof SVDResult) {
            SVDResult svdResult = (SVDResult) result;

            CTabItem cTabItem = new CTabItem(this.cTabFolder, SWT.NONE);
            SVDGOTermsComposite svdComposite;

            if (svdResult.isPValues()) {
                svdComposite = new PValuesSVDGOTermsComposite(this.cTabFolder, 0);
            } else {
                svdComposite = new SVDGOTermsComposite(this.cTabFolder, 0);
            }

            svdComposite.setResult(svdResult);
            cTabItem.setText(svdComposite.getTitle());
            ;
            cTabItem.setControl(svdComposite);
            added = true;
        }

        if (added && this.cTabFolder.getSelectionIndex() == -1) {
            this.cTabFolder.setSelection(0);
            updateWindowTitle();
        }
        this.cTabFolder.setRedraw(true);
    }

    public void appendLog(String text)
    {
        if (!this.progressText.isDisposed()) {
            this.progressText.setText(text);
        }
    }

    /**
     * Clears progress text after a period of time (asynchron)
     */
    public void clearProgressText()
    {
        this.shell.getDisplay().timerExec(1000, new Runnable()
        {
            @Override
            public void run()
            {
                if (!ResultWindow.this.progressText.isDisposed()) {
                    ResultWindow.this.progressText.setText("");
                }
            }
        });
    }

    /**
     * Returns the IGraphAction instance.
     *
     * @return
     */
    private IGraphAction getSelectedCompositeAsGraphAction()
    {
        if (this.cTabFolder.getSelection() == null) {
            return null;
        }
        Control c = this.cTabFolder.getSelection().getControl();

        if (c instanceof IGraphAction) {
            return (IGraphAction) c;
        }
        return null;
    }

    private ITableAction getSelectedCompositeAsTableAction()
    {
        if (this.cTabFolder.getSelection() == null) {
            return null;
        }
        Control c = this.cTabFolder.getSelection().getControl();
        if (c instanceof ITableAction) {
            return (ITableAction) c;
        }
        return null;
    }

    /**
     * Returns the currently selected result component.
     *
     * @return
     */
    private AbstractResultComposite getSelectedResultComposite()
    {
        if (this.cTabFolder.getSelection() == null) {
            return null;
        }
        Control c = this.cTabFolder.getSelection().getControl();

        if (c instanceof AbstractResultComposite) {
            return (AbstractResultComposite) c;
        }
        return null;
    }

    /**
     * Returns the currently selected result component if it is an enrichment result.
     *
     * @return
     */
    private EnrichedGOTermsComposite getSelectedResultCompositeIfEnriched()
    {
        AbstractResultComposite comp = getSelectedResultComposite();
        if (comp == null) {
            return null;
        }

        if (comp instanceof EnrichedGOTermsComposite) {
            return (EnrichedGOTermsComposite) comp;
        }

        return null;
    }

    private void updateWindowTitle()
    {
        AbstractResultComposite comp = getSelectedResultComposite();
        if (comp != null) {
            this.shell.setText("Ontologizer - Results for " + comp.getTitle());
        } else {
            this.shell.setText("Ontologizer - Results");
        }
    }

    /**
     * This method initializes sShell
     */
    private void createSShell(Display display)
    {
        this.shell.setText("Ontologizer - Results");
        this.shell.setLayout(new GridLayout());
        createToolBar(this.shell);
        createCTabFolder();
        createStatusComposite();
        this.shell.setSize(new org.eclipse.swt.graphics.Point(649, 486));
    }

    /**
     * Creates the toolbar.
     *
     * @param parent
     */
    private void createToolBar(Composite parent)
    {
        /**
         * A listener suitable for DropDown buttons.
         *
         * @author Sebastian Bauer
         */
        abstract class DropDownListener extends SelectionAdapter
        {
            private Menu menu;

            private Composite parent;

            public DropDownListener(Composite parent)
            {
                this.parent = parent;
            }

            abstract public Menu createMenu(Composite parent);

            abstract void defaultSelected();

            protected void processMenuEvent(final ToolItem toolItem, final MenuItem item)
            {
                final String s = item.getText().replace("...", "");
                toolItem.setToolTipText(s);
                this.menu.setVisible(false);
            }

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (this.menu == null) {
                    this.menu = createMenu(this.parent);
                }

                if (e.detail == SWT.ARROW) {
                    if (this.menu.isVisible()) {
                        this.menu.setVisible(false);
                    } else {
                        final ToolItem toolItem = (ToolItem) e.widget;
                        final ToolBar toolBar = toolItem.getParent();
                        Rectangle toolItemBounds = toolItem.getBounds();
                        Point point = toolBar.toDisplay(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
                        this.menu.setLocation(point);
                        this.menu.setVisible(true);
                    }
                } else {
                    defaultSelected();
                }
            }
        }
        ;

        this.toolbar = new ToolBar(parent, SWT.FLAT);
        final ToolItem saveTableItem = new ToolItem(this.toolbar, SWT.DROP_DOWN);
        saveTableItem.setToolTipText("Save Table");
        saveTableItem.setImage(Images.loadImage("savetable.png"));
        final ToolItem saveGraphItem = new ToolItem(this.toolbar, SWT.DROP_DOWN);
        saveGraphItem.setToolTipText("Save Graph");
        saveGraphItem.setImage(Images.loadImage("savegraph.png"));
        ToolItem previewGraphItem = new ToolItem(this.toolbar, 0);
        previewGraphItem.setToolTipText("Preview Graph");
        previewGraphItem.setImage(Images.loadImage("previewgraph.png"));

        new ToolItem(this.toolbar, SWT.SEPARATOR);

        ToolItem zoomOutItem = new ToolItem(this.toolbar, 0);
        zoomOutItem.setToolTipText("Zoom Out");
        zoomOutItem.setImage(Images.loadImage("zoomout.png"));
        ToolItem zoomInItem = new ToolItem(this.toolbar, 0);
        zoomInItem.setImage(Images.loadImage("zoomin.png"));
        zoomInItem.setToolTipText("Zoom In");
        ToolItem resetZoomItem = new ToolItem(this.toolbar, 0);
        resetZoomItem.setImage(Images.loadImage("resetzoom.png"));
        resetZoomItem.setToolTipText("Reset Zoom");
        final ToolItem scaleToFitItem = new ToolItem(this.toolbar, SWT.CHECK);
        scaleToFitItem.setToolTipText("Scale to Fit");
        scaleToFitItem.setImage(Images.loadImage("scaletofit.png"));
        scaleToFitItem.setSelection(true);

        /* Add listener */
        saveTableItem.addSelectionListener(new DropDownListener(parent.getShell())
        {
            private int tableActionNum;

            @Override
            public Menu createMenu(Composite parent)
            {
                Menu menu = new Menu(parent);
                final MenuItem menuItem1 = new MenuItem(menu, SWT.NULL);
                menuItem1.setText("Save Result as ASCII Table...");
                menuItem1.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        tableActionNum = 0;
                        processMenuEvent(saveTableItem, menuItem1);
                        performAction();
                    }
                });

                final MenuItem menuItem2 = new MenuItem(menu, SWT.NULL);
                menuItem2.setText("Save Result as HTML...");
                menuItem2.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        tableActionNum = 1;
                        processMenuEvent(saveTableItem, menuItem2);
                        performAction();
                    }
                });

                final MenuItem menuItem4 = new MenuItem(menu, SWT.NULL);
                menuItem4.setText("Save Result as Latex Document...");
                menuItem4.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        tableActionNum = 3;
                        processMenuEvent(saveTableItem, menuItem4);
                        performAction();
                    }
                });

                final MenuItem menuItem3 = new MenuItem(menu, SWT.NULL);
                menuItem3.setText("Save Study Set with Annotations...");
                menuItem3.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        tableActionNum = 2;
                        processMenuEvent(saveTableItem, menuItem3);
                        performAction();
                    }
                });

                return menu;
            }

            @Override
            protected void defaultSelected()
            {
                performAction();
            }

            private void performAction()
            {
                ITableAction tblAction = getSelectedCompositeAsTableAction();
                if (tblAction != null) {
                    String path = ResultWindow.this.tableOutputDialog.open();
                    if (path != null) {
                        switch (this.tableActionNum) {
                            case 0:
                                tblAction.tableSave(path);
                                break;
                            case 1:
                                tblAction.htmlSave(path);
                                break;
                            case 2:
                                tblAction.tableAnnotatedSetSave(path);
                                break;
                            case 3: {
                                if (!path.endsWith(".tex")) {
                                    path = path + ".tex";
                                }
                                tblAction.latexSave(path);
                                break;
                            }
                        }
                    }
                }
            }
        });

        saveGraphItem.addSelectionListener(new DropDownListener(parent.getShell())
        {
            private String extension;

            @Override
            public Menu createMenu(Composite parent)
            {
                Menu menu = new Menu(parent);

                final MenuItem menuItem0 = new MenuItem(menu, SWT.NULL);
                menuItem0.setText("Save Graph...");
                menuItem0.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        extension = null;
                        processMenuEvent(saveGraphItem, menuItem0);
                        performAction();
                    }
                });

                final MenuItem menuItem1 = new MenuItem(menu, SWT.NULL);
                menuItem1.setText("Save Graph as PNG...");
                menuItem1.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        extension = "png";
                        processMenuEvent(saveGraphItem, menuItem1);
                        performAction();
                    }
                });

                final MenuItem menuItem2 = new MenuItem(menu, SWT.NULL);
                menuItem2.setText("Save Graph as SVG...");
                menuItem2.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        extension = "svg";
                        processMenuEvent(saveGraphItem, menuItem2);
                        performAction();
                    }
                });

                final MenuItem menuItem3 = new MenuItem(menu, SWT.NULL);
                menuItem3.setText("Save Graph as DOT...");
                menuItem3.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        extension = "dot";
                        processMenuEvent(saveGraphItem, menuItem3);
                        performAction();
                    }
                });

                final MenuItem menuItem4 = new MenuItem(menu, SWT.NULL);
                menuItem4.setText("Save Graph as PS...");
                menuItem4.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        extension = "ps";
                        processMenuEvent(saveGraphItem, menuItem4);
                        performAction();
                    }
                });

                return menu;
            }

            @Override
            void defaultSelected()
            {
                performAction();
            }

            private void performAction()
            {
                IGraphAction comp = getSelectedCompositeAsGraphAction();
                if (comp != null) {
                    if (this.extension == null) {
                        ResultWindow.this.graphOutputDialog
                            .setFilterExtensions(new String[] { "*.png", "*.dot", "*.svg", "*.ps" });
                    } else {
                        ResultWindow.this.graphOutputDialog.setFilterExtensions(new String[] { "*." + this.extension });
                    }

                    String path = ResultWindow.this.graphOutputDialog.open();
                    if (path != null) {
                        /*
                         * If explicit extension has been given, ensure that the name actually has this suffix.
                         */
                        if (this.extension != null) {
                            if (!path.toLowerCase().endsWith("." + this.extension)) {
                                path = path + "." + this.extension;
                            }
                        }
                        comp.saveGraph(path);
                    }
                }
            }

        });

        previewGraphItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                AbstractResultComposite comp = getSelectedResultComposite();
                if (comp != null) {
                    comp.updateDisplayedGraph();
                }
            }
        });

        /* Add listener for graph buttons */
        zoomOutItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IGraphAction comp = getSelectedCompositeAsGraphAction();
                if (comp != null) {
                    comp.zoomOut();
                }
                scaleToFitItem.setSelection(false);
            }
        });
        zoomInItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IGraphAction comp = getSelectedCompositeAsGraphAction();
                if (comp != null) {
                    comp.zoomIn();
                }
                scaleToFitItem.setSelection(false);
            }
        });
        resetZoomItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IGraphAction comp = getSelectedCompositeAsGraphAction();
                if (comp != null) {
                    comp.resetZoom();
                }
                scaleToFitItem.setSelection(false);
            }
        });
        scaleToFitItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IGraphAction comp = getSelectedCompositeAsGraphAction();
                if (comp != null) {
                    comp.setScaleToFit(scaleToFitItem.getSelection());
                }
            }
        });
    }

    /**
     * This method initializes cTabFolder
     */
    private void createCTabFolder()
    {
        GridData gridData = new org.eclipse.swt.layout.GridData();
        gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL; // Generated
        gridData.grabExcessHorizontalSpace = true; // Generated
        gridData.grabExcessVerticalSpace = true; // Generated
        gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL; // Generated
        this.cTabFolder = new CTabFolder(this.shell, SWT.BORDER);
        this.cTabFolder.setLayoutData(gridData); // Generated
        this.cTabFolder.setSimple(false);

        this.cTabFolder.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateWindowTitle();
            }
        });
    }

    /**
     * This method initializes progressComposite
     */
    private void createStatusComposite()
    {
        this.statusComposite = new Composite(this.shell, 0);
        this.statusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        GridLayout statusLayout = SWTUtil.newEmptyMarginGridLayout(2);
        statusLayout.horizontalSpacing = 0;
        this.statusComposite.setLayout(statusLayout);

        this.progressComposite = new Composite(this.statusComposite, SWT.NONE);
        this.progressComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.progressComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));

        this.progressText = new Text(this.progressComposite, SWT.READ_ONLY);
        this.progressText.setBackground(this.shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        this.progressText.setEditable(false);
        this.progressText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.progressBar = new ProgressBar(this.progressComposite, SWT.NONE);
        this.progressBar.setVisible(false);
    }

    /**
     * Makes the progressbar invisible.
     */
    public void hideProgressBar()
    {
        this.progressBar.setVisible(false);
    }

    /**
     * Makes the progressbar visible.
     */
    public void showProgressBar()
    {
        this.progressBar.setVisible(true);
    }

    /**
     * Turns on/off the busy pointer.
     *
     * @param busy
     */
    public void setBusyPointer(boolean busy)
    {
        if (busy) {
            this.shell.setCursor(this.appStartingCursor);
        } else {
            this.shell.setCursor(null);
        }
    }

    /**
     * Returns whether the window is disposed.
     *
     * @return
     */
    public boolean isDisposed()
    {
        return this.shell.isDisposed();
    }

    /**
     * Add a new action, performed before the window gets disposed.
     *
     * @param action
     */
    public void addDisposeAction(final ISimpleAction action)
    {
        this.shell.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                action.act();
            }
        });
    }

    /**
     * Add a new action, performed before the window is closed.
     *
     * @param action
     */
    public void addCloseAction(final ISimpleAction action)
    {
        this.shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                action.act();
            }
        });
    }

    /**
     * Initialize the progress bar.
     *
     * @param max specifies the end of the progressbar's range.
     */
    public void initProgress(int max)
    {
        this.progressBar.setMaximum(max);
    }

    /**
     * Updates the progress bar according to the given value.
     *
     * @param p
     */
    public void updateProgress(int p)
    {
        this.progressBar.setSelection(p);
    }

    /**
     * This is the minimized adapter whose method is called whenever something needs to minimized.
     */
    private IMinimizedAdapter minimizedAdapter = new IMinimizedAdapter()
    {
        @Override
        public Object addMinimized(String name, final IRestoredAdapter adapter)
        {
            if (ResultWindow.this.minimizedComposite == null) {
                ResultWindow.this.minimizedComposite = new Composite(ResultWindow.this.statusComposite, SWT.NONE);
                ResultWindow.this.minimizedComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
            }

            final Button but = new Button(ResultWindow.this.minimizedComposite, 0);
            but.setText(name);
            but.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    adapter.restored();
                    but.dispose();
                    ResultWindow.this.minimizedComposite.layout();
                    ResultWindow.this.statusComposite.layout();
                    ResultWindow.this.statusComposite.getParent().layout();
                }
            });
            if (!ResultWindow.this.minimizedComposite.isVisible()) {
                ResultWindow.this.minimizedComposite.setVisible(true);
            }
            ResultWindow.this.minimizedComposite.layout();
            ResultWindow.this.statusComposite.layout();
            ResultWindow.this.statusComposite.getParent().layout();
            return null;
        };
    };
}
