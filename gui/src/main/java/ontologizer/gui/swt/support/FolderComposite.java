package ontologizer.gui.swt.support;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import ontologizer.gui.swt.ISimpleAction;

/**
 * A composite with a folder look.
 *
 * @author Sebastian Bauer
 */
public abstract class FolderComposite extends Composite
{
    private CTabFolder folder;

    private CTabItem tabItem;

    private Composite contents;

    private LinkedList<ISimpleAction> maximizeActionList = new LinkedList<>();

    private LinkedList<ISimpleAction> minimizeActionList = new LinkedList<>();

    private LinkedList<ISimpleAction> restoreActionList = new LinkedList<>();

    public FolderComposite(Composite parent, int style)
    {
        super(parent, style);
        setLayout(new FillLayout());

        this.folder = new CTabFolder(this, SWT.BORDER);
        this.folder.setMaximizeVisible(true);
        this.folder.setSingle(true);
        this.folder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        this.tabItem = new CTabItem(this.folder, 0);

        this.contents = createContents(this.folder);
        this.tabItem.setControl(this.contents);
        this.folder.setSelection(0);

        this.folder.addCTabFolder2Listener(new CTabFolder2Adapter()
        {
            @Override
            public void maximize(CTabFolderEvent event)
            {
                FolderComposite.this.folder.setMaximized(true);
                for (ISimpleAction act : FolderComposite.this.maximizeActionList) {
                    act.act();
                }
            }

            @Override
            public void restore(CTabFolderEvent event)
            {
                FolderComposite.this.folder.setMaximized(false);
                for (ISimpleAction act : FolderComposite.this.restoreActionList) {
                    act.act();
                }
            }

            @Override
            public void minimize(CTabFolderEvent event)
            {
                for (ISimpleAction act : FolderComposite.this.minimizeActionList) {
                    act.act();
                }
            }
        });
    }

    /**
     * Sets the title of the folder.
     *
     * @param text
     */
    public void setText(String text)
    {
        this.tabItem.setText(text);
    }

    protected abstract Composite createContents(Composite parent);

    /**
     * Returns the composite in which the actual contents should be placed.
     *
     * @return
     */
    public Composite getContents()
    {
        return this.contents;
    }

    public void setMaximized(boolean max)
    {
        this.folder.setMaximized(max);
    }

    public void addMaximizeAction(ISimpleAction action)
    {
        this.folder.setMaximizeVisible(true);
        this.maximizeActionList.add(action);
    }

    public void addRestoreAction(ISimpleAction action)
    {
        this.restoreActionList.add(action);
    }

    public void addMinimizeAction(ISimpleAction action)
    {
        this.folder.setMinimizeVisible(true);
        this.minimizeActionList.add(action);
    }

}
