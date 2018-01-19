package ontologizer.gui.swt.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.FolderComposite;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphCanvas;
import ontologizer.gui.swt.support.IMinimizedAdapter;
import ontologizer.gui.swt.support.IRestoredAdapter;

/**
 * Implements the logic of the result controls.
 *
 * @author Sebastian Bauer
 */
public class ResultControls extends Composite
{
    private SashForm verticalSashForm;

    private SashForm upperSashForm;

    private FolderComposite tableComposite;

    private FolderComposite graphComposite;

    private FolderComposite browserComposite;

    private IGraphCanvas graphCanvas;

    private Browser browser;

    private IMinimizedAdapter minimizedAdapter;

    private ISimpleAction restoreAction = new ISimpleAction()
    {
        @Override
        public void act()
        {
            ResultControls.this.upperSashForm.setMaximizedControl(null);
            ResultControls.this.verticalSashForm.setMaximizedControl(null);
        }
    };

    public ResultControls(Composite parent, int style)
    {
        super(parent, style);

        setLayout(new FillLayout());

        this.verticalSashForm = new SashForm(this, SWT.VERTICAL);
        this.upperSashForm = new SashForm(this.verticalSashForm, SWT.HORIZONTAL);

        /* Table */
        this.tableComposite = new FolderComposite(this.upperSashForm, 0)
        {
            @Override
            protected Composite createContents(Composite parent)
            {
                Composite comp = new Composite(parent, 0);
                comp.setLayout(new FillLayout());
                return comp;
            }
        };
        this.tableComposite.setText("Table");
        this.tableComposite.addMaximizeAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                ResultControls.this.verticalSashForm.setMaximizedControl(ResultControls.this.upperSashForm);
                ResultControls.this.upperSashForm.setMaximizedControl(ResultControls.this.tableComposite);
            }
        });
        this.tableComposite.addRestoreAction(this.restoreAction);

        /* Graph */
        this.graphComposite = new FolderComposite(this.upperSashForm, 0)
        {
            @Override
            protected Composite createContents(Composite parent)
            {
                Composite comp = new Composite(parent, 0);
                comp.setLayout(new FillLayout());
                ResultControls.this.graphCanvas = new GraphCanvas(comp, 0);
                return comp;
            }
        };
        this.graphComposite.setText("Graph");
        this.graphComposite.addMaximizeAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                ResultControls.this.verticalSashForm.setMaximizedControl(ResultControls.this.upperSashForm);
                ResultControls.this.upperSashForm.setMaximizedControl(ResultControls.this.graphComposite);
            }
        });
        this.graphComposite.addRestoreAction(this.restoreAction);
        this.graphComposite.addMinimizeAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                if (ResultControls.this.minimizedAdapter != null) {
                    ResultControls.this.graphComposite.setVisible(false);
                    ResultControls.this.upperSashForm.layout();

                    ResultControls.this.minimizedAdapter.addMinimized("Graph", new IRestoredAdapter()
                    {
                        @Override
                        public void restored()
                        {
                            ResultControls.this.graphComposite.setVisible(true);
                            ResultControls.this.upperSashForm.layout();
                        }
                    });
                }
            }
        });

        /* Browser */
        this.browserComposite = new FolderComposite(this.verticalSashForm, 0)
        {
            @Override
            protected Composite createContents(Composite parent)
            {
                Composite comp = new Composite(parent, 0);
                comp.setLayout(new FillLayout());
                ResultControls.this.browser = new Browser(comp, SWT.BORDER);
                return comp;
            }
        };
        this.browserComposite.setText("Browser");
        this.browserComposite.addMaximizeAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                ResultControls.this.verticalSashForm.setMaximizedControl(ResultControls.this.browserComposite);
            }
        });
        this.browserComposite.addRestoreAction(this.restoreAction);
        this.browserComposite.addMinimizeAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                if (ResultControls.this.minimizedAdapter != null) {
                    ResultControls.this.browserComposite.setVisible(false);
                    ResultControls.this.verticalSashForm.layout();

                    ResultControls.this.minimizedAdapter.addMinimized("Browser", new IRestoredAdapter()
                    {
                        @Override
                        public void restored()
                        {
                            ResultControls.this.browserComposite.setVisible(true);
                            ResultControls.this.verticalSashForm.layout();
                        }
                    });
                }
            }
        });

    }

    public Composite getTableComposite()
    {
        return this.tableComposite.getContents();
    }

    public void addBrowserLocationListener(LocationListener ll)
    {
        this.browser.addLocationListener(ll);
    }

    public IGraphCanvas getGraphCanvas()
    {
        return this.graphCanvas;
    }

    public Browser getBrowser()
    {
        return this.browser;
    }

    public void setMinimizedAdapter(IMinimizedAdapter minimizedAdapter)
    {
        this.minimizedAdapter = minimizedAdapter;
    }
}
