/*
 * Created on 11.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import ontologizer.FileCache;
import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

/**
 * This is the window displaying the worksets.
 *
 * @author Sebastian Bauer
 */
public class WorkSetWindow extends ApplicationWindow
{
    private Tree workSetTree;

    private TreeColumn nameColumn;

    private TreeColumn pathColumn;

    private TreeColumn downloadedColumn;

    private Button invalidateButton;

    private Button downloadButton;

    private Button newButton;

    private Button deleteButton;

    private FileGridCompositeWidgets locationGridComposite;

    /**
     * Constructs the work set window.
     *
     * @param display
     */
    public WorkSetWindow(Display display)
    {
        super(display);

        this.shell.setText("Ontologizer - File Sets");
        this.shell.setLayout(new GridLayout());

        /*
         * Prevent the disposal of the window on a close event, but make the window invisible
         */
        this.shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                e.doit = false;
                WorkSetWindow.this.shell.setVisible(false);
            }
        });

        this.workSetTree = new Tree(this.shell, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gd.widthHint = 500;
        gd.heightHint = 400;
        this.workSetTree.setLayoutData(gd);

        this.nameColumn = new TreeColumn(this.workSetTree, 0);
        this.nameColumn.setText("Name");

        this.downloadedColumn = new TreeColumn(this.workSetTree, 0);
        this.downloadedColumn.setText("Last download at");

        this.pathColumn = new TreeColumn(this.workSetTree, 0);
        this.pathColumn.setText("Path");

        this.workSetTree.setHeaderVisible(true);
        this.workSetTree.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String location = getSelectedAddress();
                if (location != null) {
                    WorkSetWindow.this.locationGridComposite.setEnabled(true);
                    WorkSetWindow.this.downloadButton.setEnabled(true);
                    WorkSetWindow.this.invalidateButton.setEnabled(true);
                    WorkSetWindow.this.locationGridComposite.setPath(location);
                } else {
                    WorkSetWindow.this.locationGridComposite.setPath("");
                    WorkSetWindow.this.invalidateButton.setEnabled(false);
                    WorkSetWindow.this.locationGridComposite.setEnabled(false);
                    WorkSetWindow.this.downloadButton.setEnabled(false);
                }
            }
        });

        this.nameColumn.pack();
        this.pathColumn.pack();
        this.downloadedColumn.pack();

        Composite textComposite = new Composite(this.shell, 0);
        textComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(5));
        textComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.locationGridComposite = new FileGridCompositeWidgets(textComposite);
        this.locationGridComposite.setLabel("Location");
        this.downloadButton = new Button(textComposite, 0);
        this.downloadButton.setText("Download");
        this.invalidateButton = new Button(textComposite, 0);
        this.invalidateButton.setText("Invalidate");

        // Composite buttonComposite = new Composite(shell,0);
        // buttonComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));

        // newButton = new Button(buttonComposite,0);
        // newButton.setText("New");

        // deleteButton = new Button(buttonComposite,0);
        // deleteButton.setText("Delete");

        this.shell.pack();
    }

    /**
     * Updates the displayed work set list.
     *
     * @param wsl
     */
    public void updateWorkSetList(WorkSetList wsl)
    {
        WorkSet selectedWorkSet = getSelectedWorkSet();
        String selectedAddress = getSelectedAddress();

        this.workSetTree.setRedraw(false);
        this.workSetTree.removeAll();

        for (WorkSet ws : wsl) {
            String obo = ws.getOboPath();
            String association = ws.getAssociationPath();

            TreeItem ti = new TreeItem(this.workSetTree, 0);
            ti.setData(ws);
            ti.setText(0, ws.getName());

            TreeItem oboTi = new TreeItem(ti, 0);
            oboTi.setText(0, "Definitions");
            oboTi.setText(1, FileCache.getDownloadTime(obo));
            oboTi.setText(2, obo);
            oboTi.setData(0);

            TreeItem associationTi = new TreeItem(ti, 0);
            associationTi.setText("Association");
            associationTi.setText(1, FileCache.getDownloadTime(association));
            associationTi.setText(2, association);
            associationTi.setData(1);

            ti.setExpanded(true);

            if (selectedWorkSet != null) {
                if (selectedWorkSet.getName().equals(ws.getName())) {
                    if (selectedAddress != null) {
                        if (obo.equals(selectedAddress)) {
                            this.workSetTree.setSelection(oboTi);
                        } else if (association.equals(selectedAddress)) {
                            this.workSetTree.setSelection(associationTi);
                        }
                    } else {
                        this.workSetTree.setSelection(ti);
                    }
                }
            }
        }

        this.nameColumn.pack();
        this.pathColumn.pack();
        this.downloadedColumn.pack();

        this.workSetTree.setRedraw(true);
    }

    /**
     * Updates the given WorkSet (which is at the given index).
     *
     * @param idx
     * @param ws
     */
    public void updateWorkSet(int idx, WorkSet ws)
    {
        TreeItem ti = this.workSetTree.getItem(idx);
        TreeItem[] children = ti.getItems();
        TreeItem oboTi = children[0];
        TreeItem associationTi = children[1];
    }

    /**
     * Add an action executed on a click of the download button.
     *
     * @param act
     */
    public void addDownloadAction(final ISimpleAction act)
    {
        addSimpleSelectionAction(this.downloadButton, act);
    }

    /**
     * Add an action that is executed on a click of the new button.
     *
     * @param act
     */
    public void addNewAction(ISimpleAction act)
    {
        addSimpleSelectionAction(this.newButton, act);
    }

    /**
     * Add an action that is executed on a click of the invalidate button.
     *
     * @param act
     */
    public void addInvalidateAction(ISimpleAction act)
    {
        addSimpleSelectionAction(this.invalidateButton, act);
    }

    /**
     * Add an action that is executed on a click of the delete button.
     *
     * @param act
     */
    public void addDeleteAction(ISimpleAction act)
    {
        addSimpleSelectionAction(this.deleteButton, act);
    }

    /**
     * Return the selected work set.
     *
     * @return
     */
    public WorkSet getSelectedWorkSet()
    {
        TreeItem[] ti = this.workSetTree.getSelection();
        if (ti != null && ti.length > 0) {
            TreeItem p = ti[0];
            if (p.getParentItem() != null) {
                p = p.getParentItem();
            }

            return (WorkSet) p.getData();
        }
        return null;
    }

    /**
     * Returns the selected address.
     *
     * @return
     */
    public String getSelectedAddress()
    {
        TreeItem[] ti = this.workSetTree.getSelection();
        if (ti != null && ti.length > 0) {
            WorkSet ws;

            TreeItem p = ti[0];
            if (p.getParentItem() != null) {
                p = p.getParentItem();
            }

            ws = (WorkSet) p.getData();

            Object data = ti[0].getData();
            if (data instanceof Integer) {
                Integer i = (Integer) data;
                if (i == 0) {
                    return ws.getOboPath();
                } else {
                    return ws.getAssociationPath();
                }
            }
        }
        return null;
    }
}
