/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import ontologizer.gui.swt.support.SWTUtil;

public abstract class WizardWindow extends ApplicationWindow
{
    private Composite stackComposite;

    private StackLayout stackLayout;

    private StyledText descriptionLabel;

    private Button prevButton;

    private Button nextButton;

    private Button finishButton;

    private Button cancelButton;

    private List<SinglePage> pageList = new LinkedList<>();

    /** Index of currently displayed page */
    protected int currentPage;

    public static interface PageCallback
    {
        boolean completed();
    };

    protected static class SinglePage
    {
        private Composite page;

        private String description;

        private PageCallback pcb;

        public SinglePage(Composite page, String description, PageCallback pcb)
        {
            this.page = page;
            this.description = description;
            this.pcb = pcb;
        }

        public SinglePage(Composite page, String description)
        {
            this(page, description, null);
        }

        public Composite getPage()
        {
            return this.page;
        }

        public String getDescription()
        {
            return this.description;
        }

        public PageCallback getPageCallback()
        {
            return this.pcb;
        }
    }

    public WizardWindow(Display display)
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
                cancel();
            }
        });

        GridData gd;

        this.shell.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

        Composite titleComposite = new Composite(this.shell, 0);
        titleComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        titleComposite.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
        GridLayout layout = SWTUtil.newEmptyMarginGridLayout(1);
        layout.verticalSpacing = 0;
        titleComposite.setLayout(layout);
        titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        Composite descriptionComposite = new Composite(titleComposite, 0);
        descriptionComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        descriptionComposite.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
        descriptionComposite.setLayout(new GridLayout());
        descriptionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        this.descriptionLabel = new StyledText(descriptionComposite, SWT.WRAP | SWT.READ_ONLY);
        this.descriptionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        Label sep = new Label(titleComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = false;
        gd.horizontalSpan = 4;
        sep.setLayoutData(gd);

        this.stackLayout = new StackLayout();
        this.stackComposite = new Composite(this.shell, 0);
        this.stackComposite.setLayout(this.stackLayout);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        gd.horizontalSpan = 4;
        this.stackComposite.setLayoutData(gd);

        addPages(this.stackComposite);

        sep = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = false;
        gd.horizontalSpan = 4;
        sep.setLayoutData(gd);

        Composite buttonComposite = new Composite(this.shell, 0);
        buttonComposite.setLayout(new GridLayout(4, false));
        buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL));

        this.prevButton = new Button(buttonComposite, 0);
        this.prevButton.setText("< Prev");
        this.prevButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.prevButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (completedCurrentPage()) {
                    showPage(WizardWindow.this.currentPage - 1);
                }
            }
        });

        this.nextButton = new Button(buttonComposite, 0);
        this.nextButton.setText("> Next");
        this.nextButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.nextButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (completedCurrentPage()) {
                    showPage(WizardWindow.this.currentPage + 1);
                }
            }
        });

        this.finishButton = new Button(buttonComposite, 0);
        this.finishButton.setText("Finish");
        this.finishButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.finishButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (completedCurrentPage() && finish()) {
                    WizardWindow.this.shell.setVisible(false);
                }
            }
        });

        this.cancelButton = new Button(buttonComposite, 0);
        this.cancelButton.setText("Cancel");
        this.cancelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.cancelButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                cancel();
            }
        });

        reset();
        showPage(0);
        this.shell.pack();
    }

    /**
     * Cancel the operation.
     */
    private void cancel()
    {
        this.shell.setVisible(false);
        reset();
        showPage(0);
    }

    /**
     * Resets the contents of the pages.
     */
    protected void reset()
    {
    }

    /**
     * @param page
     */
    protected void addPage(SinglePage page)
    {
        this.pageList.add(page);
    }

    /**
     * Returns the displayed page number for the given page index. You can overwrite this method if your wizard, e.g.,
     * defines less pages than amount of pages which are presented to the user (so some pages are recycled).
     *
     * @param which
     * @return
     */
    protected int getDisplayedPageNumber(int which)
    {
        return which;
    }

    /**
     * Shows the given page.
     *
     * @param which
     */
    protected void showPage(int which)
    {
        int displayedPageNumber = getDisplayedPageNumber(which);

        if (displayedPageNumber >= 0 && displayedPageNumber < this.pageList.size()) {
            this.currentPage = which;

            SinglePage sp = this.pageList.get(displayedPageNumber);
            this.descriptionLabel.setText(sp.getDescription());
            this.stackLayout.topControl = sp.getPage();
            this.stackComposite.layout();
            this.shell.layout();

            updateButtonStates();
        }
    }

    /**
     * Perform the action when a page selection has been completed.
     *
     * @return false if input was, e.g. invalid.
     */
    private boolean completedCurrentPage()
    {
        int displayedPageNumber = getDisplayedPageNumber(this.currentPage);

        if (displayedPageNumber >= 0 && displayedPageNumber < this.pageList.size()) {
            SinglePage sp = this.pageList.get(displayedPageNumber);
            if (sp.getPageCallback() != null) {
                return sp.getPageCallback().completed();
            }
            return true;
        }
        return false;
    }

    /**
     * Update the buttons' states.
     */
    private void updateButtonStates()
    {
        this.prevButton.setEnabled(this.currentPage > 0);
    }

    /**
     * Clears the error message.
     */
    protected void clearError()
    {
        this.nextButton.setEnabled(true);
        this.finishButton.setEnabled(true);

        int displayedPageNumber = getDisplayedPageNumber(this.currentPage);
        if (displayedPageNumber >= 0 && displayedPageNumber < this.pageList.size()) {
            SinglePage sp = this.pageList.get(displayedPageNumber);
            this.descriptionLabel.setText(sp.getDescription());
        }
    }

    /**
     * Displays an error messsage.
     *
     * @param err
     */
    protected void displayError(String err)
    {
        this.nextButton.setEnabled(false);
        this.finishButton.setEnabled(false);

        if (err != null) {
            this.descriptionLabel.setText(err);
        } else {
            int displayedPageNumber = getDisplayedPageNumber(this.currentPage);
            if (displayedPageNumber >= 0 && displayedPageNumber < this.pageList.size()) {
                SinglePage sp = this.pageList.get(displayedPageNumber);
                this.descriptionLabel.setText(sp.getDescription());
            }
        }
    }

    /**
     * Returns the parent which should be used to create the pages.
     *
     * @return
     */
    protected Composite getPageParent()
    {
        return this.stackComposite;
    }

    protected abstract void addPages(Composite parent);

    protected abstract boolean finish();
}
