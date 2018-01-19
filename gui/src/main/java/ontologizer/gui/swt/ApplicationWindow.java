package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * The base class of all windows.
 *
 * @author Sebastian Bauer
 */
public class ApplicationWindow
{
    protected Shell shell;

    protected Display display;

    private Cursor waitCursor;

    private int waitCount;

    public ApplicationWindow(Display display)
    {
        this.display = display;

        this.waitCursor = display.getSystemCursor(SWT.CURSOR_WAIT);
        this.shell = new Shell(display);
    }

    public void showWaitPointer()
    {
        if (this.waitCount == 0) {
            this.shell.setCursor(this.waitCursor);
        }
        this.waitCount++;
    }

    public void hideWaitPointer()
    {
        if (this.waitCount == 0) {
            return;
        }
        this.waitCount--;
        if (this.waitCount == 0) {
            this.shell.setCursor(null);
        }
    }

    /**
     * Open the window.
     */
    public void open()
    {
        this.shell.open();
    }

    /**
     * Dispose the window.
     */
    public void dispose()
    {
        if (!this.shell.isDisposed()) {
            this.shell.dispose();
        }
    }

    protected void addSimpleSelectionAction(Widget w, final ISimpleAction act)
    {
        w.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                act.act();
            }
        });
    }

    public void setVisible(boolean b)
    {
        this.shell.setVisible(b);
    }

}
