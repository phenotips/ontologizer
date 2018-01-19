package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import ontologizer.FileCache;

public class FileCacheWindow extends ApplicationWindow
{
    private Table table;

    private Text directoryText;

    private Button removeButton;

    private TableColumn fileNameColumn;

    private TableColumn urlColumn;

    private TableColumn downloadedColumn;

    public FileCacheWindow(Display display)
    {
        super(display);

        this.shell.setText("Ontologizer - File Cache");
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
                FileCacheWindow.this.shell.setVisible(false);
            }
        });

        this.directoryText = new Text(this.shell, SWT.READ_ONLY);
        this.directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.table = new Table(this.shell, SWT.BORDER | SWT.FULL_SELECTION);
        this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        this.table.setHeaderVisible(true);

        this.fileNameColumn = new TableColumn(this.table, 0);
        this.fileNameColumn.setText("Filename");
        this.fileNameColumn.pack();

        this.urlColumn = new TableColumn(this.table, 0);
        this.urlColumn.setText("URL");
        this.urlColumn.pack();

        this.downloadedColumn = new TableColumn(this.table, 0);
        this.downloadedColumn.setText("Downloaded at");
        this.downloadedColumn.pack();

        this.removeButton = new Button(this.shell, 0);
        this.removeButton.setText("Remove");

        updateView();

        this.shell.pack();
    }

    @Override
    public void open()
    {

        updateView();

        this.shell.pack();
        super.open();
    }

    public void setDirectoryText(String directory)
    {
        this.directoryText.setText("Contents of '" + directory + "'");
    }

    public void updateView()
    {
        this.table.removeAll();
        FileCache.visitFiles(new FileCache.IFileVisitor()
        {
            @Override
            public boolean visit(String filename, String url, String info, String downloadedAt)
            {
                TableItem item = new TableItem(FileCacheWindow.this.table, 0);
                item.setText(0, filename);
                item.setText(1, url);
                item.setText(2, downloadedAt);
                return true;
            }
        });
        this.fileNameColumn.pack();
        this.urlColumn.pack();
    }

    public void addRemoveAction(final ISimpleAction a)
    {
        this.removeButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Returns the URL of the selected entry.
     */
    public String getSelectedURL()
    {
        TableItem[] sel = this.table.getSelection();
        if (sel != null && sel.length > 0) {
            return sel[0].getText(1);
        }
        return null;
    }
}
