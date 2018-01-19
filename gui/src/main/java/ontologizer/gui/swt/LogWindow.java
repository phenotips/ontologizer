package ontologizer.gui.swt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class LogWindow extends ApplicationWindow
{
    private static Logger logger = Logger.getLogger(LogWindow.class.getCanonicalName());

    private StyledText logStyledText;

    private FileDialog logFileDialog;

    public LogWindow(Display display)
    {
        super(display);

        this.logFileDialog = new FileDialog(this.shell, SWT.SAVE);
        this.logFileDialog.setOverwrite(true);
        this.logFileDialog.setFilterExtensions(new String[] { "*.txt", "*.log" });

        this.shell.setText("Ontologizer - Log");
        this.shell.setLayout(new FillLayout());

        this.logStyledText = new StyledText(this.shell, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);

        Menu menu = new Menu(this.shell, SWT.BAR);
        MenuItem logItem = new MenuItem(menu, SWT.CASCADE);
        logItem.setText("Log");
        Menu logMenu = new Menu(menu);
        logItem.setMenu(logMenu);

        MenuItem saveItem = new MenuItem(logMenu, SWT.NONE);
        saveItem.setText("Save Log...");
        saveItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent arg0)
            {
                String fileName = LogWindow.this.logFileDialog.open();
                if (fileName != null) {
                    FileWriter fw;
                    try {
                        fw = new FileWriter(new File(fileName));
                        fw.write(LogWindow.this.logStyledText.getText());
                        fw.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to write log to \"" + fileName + "\"", e);
                    }
                }
            }
        });
        this.shell.setMenuBar(menu);

        this.shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                e.doit = false;
                LogWindow.this.shell.setVisible(false);
            }
        });
    }

    @Override
    public void dispose()
    {
        this.shell.dispose();
    }

    public void addToLog(String txt)
    {
        this.logStyledText.append(txt);

        int docLength = this.logStyledText.getCharCount();
        if (docLength > 0) {
            this.logStyledText.setCaretOffset(docLength);
            this.logStyledText.showSelection();
        }
    }
}
