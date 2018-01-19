package ontologizer.gui.swt.support;

import java.io.File;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ontologizer.gui.swt.ISimpleAction;

/**
 * A custom widget which can be used to allow the user to specify a file.
 *
 * @author Sebastian Bauer
 */
public class FileGridCompositeWidgets
{
    private Label label;

    private Button labelButton;

    private Text text;

    private Button button;

    private String[] filterExts;

    private String[] filterNames;

    private Color errorColor;

    private Color textBackgroundColor;

    private String tooltip;

    private LinkedList<ISimpleAction> actions = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param parent
     */
    public FileGridCompositeWidgets(final Composite parent)
    {
        this(parent, false);
    }

    /**
     * Constructor.
     *
     * @param parent
     * @param checkable
     */
    public FileGridCompositeWidgets(final Composite parent, boolean checkable)
    {
        if (checkable) {
            this.labelButton = new Button(parent, checkable ? SWT.CHECK : 0);
            this.labelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            this.labelButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    updateEnabledState();
                }
            });

        } else {
            this.label = new Label(parent, 0);
            this.label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }

        this.text = new Text(parent, SWT.BORDER);
        this.text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        this.text.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                executeActions();
            }
        });
        this.button = new Button(parent, 0);
        this.button.setText("Browse...");
        this.button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.OPEN);
                if (FileGridCompositeWidgets.this.filterExts != null) {
                    fileDialog.setFilterExtensions(FileGridCompositeWidgets.this.filterExts);
                }
                if (FileGridCompositeWidgets.this.filterNames != null) {
                    fileDialog.setFilterNames(FileGridCompositeWidgets.this.filterNames);
                }

                if (FileGridCompositeWidgets.this.text.getText() != null) {
                    File f = new File(FileGridCompositeWidgets.this.text.getText());
                    fileDialog.setFileName(f.getName());
                    fileDialog.setFilterPath(f.getParent());
                }

                String fileName = fileDialog.open();
                if (fileName != null) {
                    FileGridCompositeWidgets.this.text.setText(fileName);
                    executeActions();
                }
            }
        });

        this.textBackgroundColor = this.text.getBackground();
        this.textBackgroundColor = new Color(parent.getDisplay(), this.textBackgroundColor.getRGB());
        this.errorColor = new Color(parent.getDisplay(), 255, 160, 160);

        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                FileGridCompositeWidgets.this.textBackgroundColor.dispose();
                FileGridCompositeWidgets.this.errorColor.dispose();
            }
        });

        updateEnabledState();
    }

    private void updateEnabledState()
    {
        if (this.labelButton != null) {
            this.text.setEnabled(this.labelButton.getSelection());
            this.button.setEnabled(this.labelButton.getSelection());
        }
    }

    /**
     * Sets the path.
     *
     * @param path
     */
    public void setPath(String path)
    {
        if (path == null) {
            path = "";
        }
        this.text.setText(path);

        if (this.labelButton != null) {
            this.labelButton.setSelection(path.length() > 0);
        }

        updateEnabledState();
    }

    /**
     * Returns the current path.
     *
     * @return
     */
    public String getPath()
    {
        if (this.labelButton != null && !this.labelButton.getSelection()) {
            return "";
        }

        return this.text.getText();
    }

    public void setLabel(String labelString)
    {
        if (this.label != null) {
            this.label.setText(labelString);
        } else {
            this.labelButton.setText(labelString);
        }
    }

    public void setFilterExtensions(String[] filterExts)
    {
        this.filterExts = filterExts;
    }

    public void setFilterNames(String[] filterNames)
    {
        this.filterNames = filterNames;
    }

    private void reallySetToolTipText(String string)
    {
        this.text.setToolTipText(string);
        if (this.label != null) {
            this.label.setToolTipText(string);
        } else {
            this.labelButton.setToolTipText(string);
        }
        this.button.setToolTipText(string);
    }

    public void setToolTipText(String string)
    {
        this.tooltip = string;
        reallySetToolTipText(string);
    }

    public void setEnabled(boolean state)
    {
        this.text.setEnabled(state);
        if (this.label != null) {
            this.label.setEnabled(state);
        } else {
            this.labelButton.setEnabled(state);
        }
        this.button.setEnabled(state);
    }

    public void setErrorString(String error)
    {
        if (error != null && error.length() > 0) {
            this.text.setBackground(this.errorColor);
            reallySetToolTipText(this.tooltip + "\n\n" + error);
        } else {
            this.text.setBackground(this.textBackgroundColor);
            reallySetToolTipText(this.tooltip);
        }
    }

    /**
     * Add an action that is called when the contents changes.
     *
     * @param act
     */
    public void addTextChangedAction(ISimpleAction act)
    {
        this.actions.add(act);
    }

    /**
     * Executes actions that were registered using addTextChangedAction().
     */
    private void executeActions()
    {
        for (ISimpleAction act : this.actions) {
            act.act();
        }
    }
}
