package ontologizer.gui.swt.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import ontologizer.go.Term;
import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.SWTUtil;
import sonumina.collections.FullStringIndex;

/**
 * A class comprising of a text field in which the user may enter a term. Terms can be selected via by using a selection
 * list.
 *
 * @author Sebastian Bauer
 */
public class TermFilterSelectionComposite extends Composite
{
    /** Contains terms that possibly could be chosen */
    private FullStringIndex<Term> fsi = new FullStringIndex<>();

    /** Contains the terms which are currently displayed within the suggestion list */
    private ArrayList<Term> suggestionList;

    /** The currently selected term */
    private Term selectedTerm;

    /* GUI Elements */
    private Label subtermFilterLabel;

    private Text subtermFilterText;

    private Button subtermFilterButton;

    private Shell subtermFilterSuggestionShell;

    private Table subtermFilterSuggestionTable;

    private TableColumn subtermFilterSuggestionTableIDColumn;

    private TableColumn subtermFilterSuggestionTableNameColumn;

    private TableColumn subtermFilterSuggestionTableNamespaceColumn;

    /** Action performed upon new term selection */
    private ISimpleAction newTermAction;

    /**
     * @param parent
     * @param style
     */
    public TermFilterSelectionComposite(Composite parent, int style)
    {
        super(parent, style);

        /* Subterm filter */
        this.subtermFilterSuggestionShell = new Shell(getShell(), SWT.TOOL | SWT.ON_TOP);
        this.subtermFilterSuggestionShell.setLayout(new FillLayout());
        this.subtermFilterSuggestionTable =
            new Table(this.subtermFilterSuggestionShell, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION);
        this.subtermFilterSuggestionTableIDColumn = new TableColumn(this.subtermFilterSuggestionTable, 0);
        this.subtermFilterSuggestionTableIDColumn.setText("Term ID");
        this.subtermFilterSuggestionTableNamespaceColumn = new TableColumn(this.subtermFilterSuggestionTable, 0);
        this.subtermFilterSuggestionTableNamespaceColumn.setText("Namespace");
        this.subtermFilterSuggestionTableNameColumn = new TableColumn(this.subtermFilterSuggestionTable, 0);
        this.subtermFilterSuggestionTableNameColumn.setText("Name");
        this.subtermFilterSuggestionTable.addListener(SWT.SetData, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                TableItem item = (TableItem) event.item;
                int tableIndex = TermFilterSelectionComposite.this.subtermFilterSuggestionTable.indexOf(item);

                item.setText(0, TermFilterSelectionComposite.this.suggestionList.get(tableIndex).getIDAsString());
                item.setText(1,
                    TermFilterSelectionComposite.this.suggestionList.get(tableIndex).getNamespaceAsString());
                item.setText(2, TermFilterSelectionComposite.this.suggestionList.get(tableIndex).getName());
            }
        });

        setLayout(SWTUtil.newEmptyMarginGridLayout(3));
        this.subtermFilterLabel = new Label(this, SWT.NONE);
        this.subtermFilterLabel.setText("Display terms emanating from");
        this.subtermFilterText = new Text(this, SWT.BORDER);
        this.subtermFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.subtermFilterText.setText("Gene Ontology");
        this.subtermFilterText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                /* Make the suggestion list invisible */
                TermFilterSelectionComposite.this.subtermFilterSuggestionShell.setVisible(false);
            }
        });
        this.subtermFilterText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                int i;
                int idx;

                switch (e.keyCode) {
                    case SWT.ARROW_DOWN:
                        idx = TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getSelectionIndex();
                        idx++;
                        if (idx == TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getItemCount()) {
                            idx = 0;
                        }
                        TermFilterSelectionComposite.this.subtermFilterSuggestionTable.setSelection(idx);
                        break;

                    case SWT.ARROW_UP:
                        idx = TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getSelectionIndex();
                        if (idx == 0) {
                            idx = TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getItemCount() - 1;
                        } else {
                            idx--;
                        }
                        TermFilterSelectionComposite.this.subtermFilterSuggestionTable.setSelection(idx);
                        break;

                    case 13: /* Return */
                        if (TermFilterSelectionComposite.this.subtermFilterSuggestionShell.isVisible()) {
                            idx = TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getSelectionIndex();
                            if (idx != -1) {
                                String name = TermFilterSelectionComposite.this.suggestionList.get(idx).getName();
                                TermFilterSelectionComposite.this.subtermFilterText.setText(name);
                                TermFilterSelectionComposite.this.subtermFilterText.setSelection(name.length());
                            }
                        }

                        /* Find the proper term. This is implemented very naively */
                        Iterator<Term> iter = TermFilterSelectionComposite.this.fsi
                            .contains(TermFilterSelectionComposite.this.subtermFilterText.getText()).iterator();
                        if (iter.hasNext()) {
                            TermFilterSelectionComposite.this.selectedTerm = iter.next();
                        } else {
                            TermFilterSelectionComposite.this.selectedTerm = null;
                        }
                        if (TermFilterSelectionComposite.this.newTermAction != null) {
                            TermFilterSelectionComposite.this.newTermAction.act();
                        }
                        break;
                }

            }
        });
        this.subtermFilterText.addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                String text = TermFilterSelectionComposite.this.subtermFilterText.getText().toLowerCase();

                /* Populate the term suggestion list */
                TermFilterSelectionComposite.this.suggestionList = new ArrayList<>();

                for (Term t : TermFilterSelectionComposite.this.fsi.contains(text)) {
                    TermFilterSelectionComposite.this.suggestionList.add(t);
                }

                /* Sort the suggestion list according to names of the terms alphabetically */
                Collections.sort(TermFilterSelectionComposite.this.suggestionList, new Comparator<Term>()
                {
                    @Override
                    public int compare(Term o1, Term o2)
                    {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                /*
                 * We display the list if either we have more than one suggestion or if the single suggestion is no
                 * exact match
                 */
                if (TermFilterSelectionComposite.this.suggestionList.size() > 1
                    || (TermFilterSelectionComposite.this.suggestionList.size() == 1
                        && !text.equalsIgnoreCase(TermFilterSelectionComposite.this.suggestionList.get(0).getName()))) {
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTable
                        .setItemCount(TermFilterSelectionComposite.this.suggestionList.size());
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTable.clearAll();
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTableIDColumn.pack();
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTableNameColumn.pack();
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTableNamespaceColumn.pack();
                    if (TermFilterSelectionComposite.this.subtermFilterSuggestionTableIDColumn.getWidth() < 85) {
                        TermFilterSelectionComposite.this.subtermFilterSuggestionTableIDColumn.setWidth(85);
                    }
                    if (TermFilterSelectionComposite.this.subtermFilterSuggestionTableNamespaceColumn.getWidth() < 30) {
                        TermFilterSelectionComposite.this.subtermFilterSuggestionTableNamespaceColumn.setWidth(30);
                    }
                    if (TermFilterSelectionComposite.this.subtermFilterSuggestionTableNameColumn.getWidth() < 100) {
                        TermFilterSelectionComposite.this.subtermFilterSuggestionTableNameColumn
                            .setWidth(
                                TermFilterSelectionComposite.this.subtermFilterSuggestionTable.getClientArea().width
                                    - TermFilterSelectionComposite.this.subtermFilterSuggestionTableIDColumn.getWidth()
                                    - TermFilterSelectionComposite.this.subtermFilterSuggestionTableNamespaceColumn
                                        .getWidth());
                    }

                    if (!TermFilterSelectionComposite.this.subtermFilterSuggestionShell.isVisible()) {
                        Point loc = TermFilterSelectionComposite.this.subtermFilterText.toDisplay(0, 0);
                        TermFilterSelectionComposite.this.subtermFilterSuggestionShell.setBounds(loc.x,
                            loc.y + TermFilterSelectionComposite.this.subtermFilterText.getSize().y,
                            TermFilterSelectionComposite.this.subtermFilterText.getSize().x, 250);
                        TermFilterSelectionComposite.this.subtermFilterSuggestionShell.setVisible(true);
                    }
                } else {
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTable
                        .setItemCount(TermFilterSelectionComposite.this.suggestionList.size());
                    TermFilterSelectionComposite.this.subtermFilterSuggestionTable.clearAll();
                    if (TermFilterSelectionComposite.this.subtermFilterSuggestionShell.isVisible()) {
                        TermFilterSelectionComposite.this.subtermFilterSuggestionShell.setVisible(false);
                    }
                }

            }
        });
        this.subtermFilterButton = new Button(this, 0);
        this.subtermFilterButton.setText("All");
        this.subtermFilterButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TermFilterSelectionComposite.this.subtermFilterText.setText("Gene Ontology");
                TermFilterSelectionComposite.this.selectedTerm = null;
                if (TermFilterSelectionComposite.this.newTermAction != null) {
                    TermFilterSelectionComposite.this.newTermAction.act();
                }
            }
        });

    }

    /**
     * Set the terms supported by this chooser.
     *
     * @param supportedTerms
     */
    public void setSupportedTerms(Term[] supportedTerms)
    {
        this.fsi.clear();
        for (Term supportedTerm : supportedTerms) {
            this.fsi.add(supportedTerm.getName(), supportedTerm);
            this.fsi.add(supportedTerm.getIDAsString(), supportedTerm);
        }
    }

    /**
     * Returns the currently selected term.
     *
     * @return
     */
    public Term getSelectedTerm()
    {
        return this.selectedTerm;
    }

    /**
     * Action to be executed if a new term is selected.
     *
     * @param act
     */
    public void setNewTermAction(ISimpleAction act)
    {
        this.newTermAction = act;
    }
}
