package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import ontologizer.GlobalPreferences;
import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;

/**
 * This class implements the preferences window.
 *
 * @author Sebastian Bauer
 */
public class PreferencesWindow extends ApplicationWindow
{
    private static String resamplingToolTipText =
        "Specifies the number of resampling steps which are performed for a permutation based multiple test procedure.";

    private Button okButton;

    private Text proxyText;

    private Spinner portSpinner;

    private FileGridCompositeWidgets dotFileComposite;

    private Spinner permutationSpinner;

    private Button wrapColumnCheckbox;

    private Spinner wrapColumnSpinner;

    private Spinner alphaSpinner;

    private Button alphaAutoButton;

    private Spinner upperAlphaSpinner;

    private Spinner betaSpinner;

    private Button betaAutoButton;

    private Spinner upperBetaSpinner;

    private Spinner expectedNumberSpinner;

    private Button expectedNumberAutoButton;

    private Spinner mcmcStepsSpinner;

    private final static int ALPHA_BETA_DIGITS = 2;

    /**
     * Constructor.
     *
     * @param display
     */
    public PreferencesWindow(Display display)
    {
        super(display);

        this.shell.setText("Ontologizer - Preferences");

        this.shell.setLayout(new GridLayout(1, false));

        /*
         * Prevent the disposal of the window on a close event, but make the window invisible
         */
        this.shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                e.doit = false;
                PreferencesWindow.this.shell.setVisible(false);
            }
        });

        TabFolder tabFolder = new TabFolder(this.shell, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        TabItem generalItem = new TabItem(tabFolder, SWT.NONE);
        generalItem.setText("General");
        TabItem b2gItem = new TabItem(tabFolder, SWT.NONE);
        b2gItem.setText("MGSA");

        /* Dot composite */
        Composite composite = new Composite(tabFolder, 0);
        composite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));

        this.dotFileComposite = new FileGridCompositeWidgets(composite);
        this.dotFileComposite.setToolTipText(
            "Specifies the path of the dot command of the GraphViz package, which is used for layouting the graph.");
        this.dotFileComposite.setLabel("DOT command");

        Label wrapLabel = new Label(composite, 0);
        wrapLabel.setText("Wrap GO names");
        wrapLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER));

        Composite wrapComposite = new Composite(composite, 0);
        wrapComposite.setLayoutData(new GridData(SWT.FILL, 0, true, false, 2, 1));
        wrapComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
        this.wrapColumnCheckbox = new Button(wrapComposite, SWT.CHECK);
        this.wrapColumnCheckbox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateWrapEnableState();
            }
        });

        Label columnLabel = new Label(wrapComposite, 0);
        columnLabel.setText("At Column");
        this.wrapColumnSpinner = new Spinner(wrapComposite, SWT.BORDER);
        this.wrapColumnSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));

        Label permutationLabel = new Label(composite, 0);
        permutationLabel.setText("Resampling Steps");
        permutationLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        permutationLabel.setToolTipText(resamplingToolTipText);
        this.permutationSpinner = new Spinner(composite, SWT.BORDER);
        this.permutationSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 2, 1));
        this.permutationSpinner.setMinimum(100);
        this.permutationSpinner.setMaximum(5000);
        this.permutationSpinner.setToolTipText(resamplingToolTipText);

        /* Proxy Composite */
        Label proxyLabel = new Label(composite, 0);
        proxyLabel.setText("Proxy");
        proxyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        this.proxyText = new Text(composite, SWT.BORDER);
        this.proxyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.portSpinner = new Spinner(composite, SWT.BORDER);
        this.portSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        this.portSpinner.setMaximum(65535);

        generalItem.setControl(composite);

        if (true)// CalculationRegistry.experimentalActivated())
        {
            Composite b2gComp = new Composite(tabFolder, 0);
            b2gItem.setControl(b2gComp);
            b2gComp.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
            composite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

            Label alphaLabel = new Label(b2gComp, 0);
            alphaLabel.setText("Alpha (in percent)");
            alphaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            this.alphaSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.alphaSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.alphaSpinner.setMinimum(1);
            this.alphaSpinner.setMaximum(99 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.alphaSpinner.setSelection(10 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.alphaSpinner.setDigits(ALPHA_BETA_DIGITS);
            this.alphaSpinner.setEnabled(false);
            this.alphaAutoButton = new Button(b2gComp, SWT.CHECK);
            this.alphaAutoButton.setText("Auto");
            this.alphaAutoButton.setSelection(true);
            this.alphaAutoButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    PreferencesWindow.this.alphaSpinner
                        .setEnabled(!PreferencesWindow.this.alphaAutoButton.getSelection());
                }
            });

            Label upperAlphaLabel = new Label(b2gComp, 0);
            upperAlphaLabel.setText("Upper bound for alpha");
            upperAlphaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            this.upperAlphaSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.upperAlphaSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.upperAlphaSpinner.setMinimum(1);
            this.upperAlphaSpinner.setMaximum(100 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.upperAlphaSpinner.setSelection(100 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.upperAlphaSpinner.setDigits(ALPHA_BETA_DIGITS);
            this.upperAlphaSpinner.setEnabled(true);
            new Label(b2gComp, 0);

            Label betaLabel = new Label(b2gComp, 0);
            betaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            betaLabel.setText("Beta (in percent)");
            this.betaSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.betaSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.betaSpinner.setMinimum(1);
            this.betaSpinner.setMaximum(99 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.betaSpinner.setSelection(25 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.betaSpinner.setDigits(ALPHA_BETA_DIGITS);
            this.betaSpinner.setEnabled(false);
            this.betaAutoButton = new Button(b2gComp, SWT.CHECK);
            this.betaAutoButton.setText("Auto");
            this.betaAutoButton.setSelection(true);
            this.betaAutoButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    PreferencesWindow.this.betaSpinner
                        .setEnabled(!PreferencesWindow.this.betaAutoButton.getSelection());
                }
            });

            Label upperBetaLabel = new Label(b2gComp, 0);
            upperBetaLabel.setText("Upper bound for beta");
            upperBetaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            this.upperBetaSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.upperBetaSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.upperBetaSpinner.setMinimum(1);
            this.upperBetaSpinner.setMaximum(100 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.upperBetaSpinner.setSelection(100 * (int) Math.pow(10, ALPHA_BETA_DIGITS));
            this.upperBetaSpinner.setDigits(ALPHA_BETA_DIGITS);
            this.upperBetaSpinner.setEnabled(true);
            new Label(b2gComp, 0);

            Label priorLabel = new Label(b2gComp, 0);
            priorLabel.setText("Expected number of terms");
            priorLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            this.expectedNumberSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.expectedNumberSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.expectedNumberSpinner.setMinimum(1);
            this.expectedNumberSpinner.setMaximum(50);
            this.expectedNumberSpinner.setSelection(5);
            this.expectedNumberSpinner.setEnabled(false);
            this.expectedNumberAutoButton = new Button(b2gComp, SWT.CHECK);
            this.expectedNumberAutoButton.setText("Auto");
            this.expectedNumberAutoButton.setSelection(true);
            this.expectedNumberAutoButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    PreferencesWindow.this.expectedNumberSpinner
                        .setEnabled(!PreferencesWindow.this.expectedNumberAutoButton.getSelection());
                }
            });

            Label mcmcStepsLabel = new Label(b2gComp, 0);
            mcmcStepsLabel.setText("Number of steps for MCMC");
            mcmcStepsLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            this.mcmcStepsSpinner = new Spinner(b2gComp, SWT.BORDER);
            this.mcmcStepsSpinner.setLayoutData(new GridData(SWT.FILL, 0, true, false, 1, 1));
            this.mcmcStepsSpinner.setMaximum(10000000);
            this.mcmcStepsSpinner.setIncrement(50000);
            this.mcmcStepsSpinner.setPageIncrement(100000);
            this.mcmcStepsSpinner.setSelection(500000);
            new Label(b2gComp, 0);
        }

        /* Button composite */
        SelectionAdapter closeWindowAdapter = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                PreferencesWindow.this.shell.setVisible(false);
            }
        };
        Composite buttonComposite = new Composite(this.shell, 0);
        buttonComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL));
        this.okButton = new Button(buttonComposite, 0);
        this.okButton.setText("Ok");
        this.okButton.setToolTipText("Accept the settings.");
        this.okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        this.okButton.addSelectionListener(closeWindowAdapter);
        Button cancelButton = new Button(buttonComposite, 0);
        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Decline the settings.");
        cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        cancelButton.addSelectionListener(closeWindowAdapter);

        this.shell.pack();
        if (this.shell.getSize().x < 250) {
            this.shell.setSize(250, this.shell.getSize().y);
        }
    }

    /**
     * Opens the shell window using the current settings.
     *
     * @return
     */
    @Override
    public void open()
    {
        if (!this.shell.isVisible()) {
            /* Initialize the widgets' contents */
            this.dotFileComposite.setPath(GlobalPreferences.getDOTPath());
            this.permutationSpinner.setSelection(GlobalPreferences.getNumberOfPermutations());
            this.portSpinner.setSelection(GlobalPreferences.getProxyPort());
            this.upperAlphaSpinner
                .setSelection((int) (GlobalPreferences.getUpperAlpha() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
            this.upperBetaSpinner
                .setSelection((int) (GlobalPreferences.getUpperBeta() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
            this.mcmcStepsSpinner.setSelection(GlobalPreferences.getMcmcSteps());

            if (!Double.isNaN(GlobalPreferences.getAlpha())) {
                this.alphaSpinner
                    .setSelection((int) (GlobalPreferences.getAlpha() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
                this.alphaSpinner.setEnabled(true);
                this.alphaAutoButton.setSelection(false);
            }
            if (!Double.isNaN(GlobalPreferences.getBeta())) {
                this.betaSpinner
                    .setSelection((int) (GlobalPreferences.getBeta() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
                this.betaSpinner.setEnabled(true);
                this.betaAutoButton.setSelection(false);
            }

            if (GlobalPreferences.getExpectedNumber() > 0) {
                this.expectedNumberSpinner.setSelection(GlobalPreferences.getExpectedNumber());
                this.expectedNumberSpinner.setEnabled(true);
                this.expectedNumberAutoButton.setSelection(false);
            }

            int wc = GlobalPreferences.getWrapColumn();
            if (wc == -1) {
                this.wrapColumnCheckbox.setSelection(false);
            } else {
                this.wrapColumnCheckbox.setSelection(true);
                this.wrapColumnSpinner.setSelection(wc);
            }
            if (GlobalPreferences.getProxyHost() != null) {
                this.proxyText.setText(GlobalPreferences.getProxyHost());
            }
        }
        updateWrapEnableState();
        super.open();
    }

    /**
     * Disposes the window.
     */
    @Override
    public void dispose()
    {
        this.shell.dispose();
    }

    /**
     * Executes the given action on a accept preferences event.
     *
     * @param ba
     */
    public void addAcceptPreferencesAction(final ISimpleAction ba)
    {
        this.okButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                ba.act();
            }
        });
    }

    /**
     * Returns the dot path.
     *
     * @return
     */
    public String getDOTPath()
    {
        return this.dotFileComposite.getPath();
    }

    /**
     * Returns the number of permutations.
     *
     * @return
     */
    public int getNumberOfPermutations()
    {
        return this.permutationSpinner.getSelection();
    }

    /**
     * Returns the proxy port.
     *
     * @return
     */
    public int getProxyPort()
    {
        return this.portSpinner.getSelection();
    }

    /**
     * Returns the proxy host.
     *
     * @return
     */
    public String getProxyHost()
    {
        return this.proxyText.getText();
    }

    /**
     * Returns the wrap column or -1 if this feature should be disabled.
     *
     * @return
     */
    public int getWrapColumn()
    {
        if (!this.wrapColumnCheckbox.getSelection()) {
            return -1;
        }

        return this.wrapColumnSpinner.getSelection();
    }

    /**
     * Updates the wrap enables state according to the selection state of the wrap checkbox.
     */
    private void updateWrapEnableState()
    {
        this.wrapColumnSpinner.setEnabled(this.wrapColumnCheckbox.getSelection());
    }

    /**
     * Returns the selected alpha.
     *
     * @return the selected alpha or NaN if alpha value is not given.
     */
    public double getAlpha()
    {
        if (this.alphaAutoButton != null) {
            if (this.alphaAutoButton.getSelection()) {
                return Double.NaN;
            }
        }

        if (this.alphaSpinner != null) {
            return this.alphaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
        }
        return Double.NaN;
    }

    public double getUpperAlpha()
    {
        return this.upperAlphaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
    }

    /**
     * Returns the selected beta.
     *
     * @return the selected beta or NaN if alpha value is not given.
     */
    public double getBeta()
    {
        if (this.betaAutoButton != null) {
            if (this.betaAutoButton.getSelection()) {
                return Double.NaN;
            }
        }

        if (this.betaSpinner != null) {
            return this.betaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
        }
        return 0.1;
    }

    public double getUpperBeta()
    {
        return this.upperBetaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
    }

    public int getExpectedNumberOfTerms()
    {
        if (this.expectedNumberAutoButton != null) {
            if (this.expectedNumberAutoButton.getSelection()) {
                return -1;
            }
        }

        if (this.expectedNumberSpinner != null) {
            return this.expectedNumberSpinner.getSelection();
        }
        return 1;
    }

    /**
     * Returns the number of MCMC steps to be performed.
     *
     * @return
     */
    public int getNumberOfMCMCSteps()
    {
        if (this.mcmcStepsSpinner != null) {
            return this.mcmcStepsSpinner.getSelection();
        }
        return 500000;
    }
}
