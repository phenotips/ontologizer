package ontologizer.gui.swt.result;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import ontologizer.GlobalPreferences;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphGenerationFinished;

/**
 * An abstract class for the result composite.
 *
 * @author Sebastian Bauer
 */
public abstract class AbstractResultComposite extends Composite
{
    /** The GO graph */
    protected Ontology go;

    /** The association container */
    protected AssociationContainer associationContainer;

    /** An array of all properties in arbitrary order */
    protected AbstractGOTermProperties[] props;

    /** The results */
    private AbstractGOTermsResult result;

    /** The composite for the sub term filter */
    private TermFilterSelectionComposite subtermFilterComposite;

    /** The composite for the graph display */
    protected GraphCanvas graphVisual;

    /**
     * Stores the terms ids of checked terms (i.e. terms which are going to be included within the graph, aka the terms
     * inducing the go graph )
     */
    private HashSet<TermID> checkedTerms;

    /**
     * The emanating term (term which all other terms must be descendents from in order to be displayed
     */
    private Term emanatingTerm;

    /** The attributes provider */
    private AbstractDotAttributesProvider dotNodeAttributesProvider;

    public AbstractResultComposite(Composite parent, int style)
    {
        super(parent, style);

        initializeCheckedTerms();
    }

    /** Returns the name of the result */
    abstract public String getTitle();

    protected static class GOIDComparator implements Comparator<AbstractGOTermProperties>
    {
        private int direction;

        public GOIDComparator(int direction)
        {
            this.direction = direction;
        }

        @Override
        public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
        {
            return (o1.goTerm.getID().id - o2.goTerm.getID().id) * this.direction;
        }
    };

    protected static class GONameComparator implements Comparator<AbstractGOTermProperties>
    {
        private int direction;

        public GONameComparator(int direction)
        {
            this.direction = direction;
        }

        @Override
        public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
        {
            return (o1.goTerm.getName().compareToIgnoreCase(o2.goTerm.getName())) * this.direction;
        }
    };

    /**
     * Returns the comparator for the checked field.
     *
     * @param direction
     * @return
     */
    protected Comparator<AbstractGOTermProperties> getCheckedComparator(final int direction)
    {
        return new Comparator<AbstractGOTermProperties>()
        {
            @Override
            public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
            {
                boolean c1 = AbstractResultComposite.this.checkedTerms.contains(o1.goTerm.getID());
                boolean c2 = AbstractResultComposite.this.checkedTerms.contains(o2.goTerm.getID());

                if (c1 == c2) {
                    return 0;
                }
                if (c1) {
                    return -1 * direction;
                }
                return 1 * direction;
            }
        };
    }

    /**
     * Set the currently displayed results. When overwritten, this needs to be called by overwriting methods!
     *
     * @param result
     */
    public void setResult(AbstractGOTermsResult result)
    {
        this.go = result.getGO();
        this.associationContainer = result.getAssociations();
        this.result = result;

        /* Build the prop list */
        LinkedList<AbstractGOTermProperties> propList = new LinkedList<>();
        for (AbstractGOTermProperties prop : result) {
            propList.add(prop);
        }

        /* Now convert to an array */
        this.props = new AbstractGOTermProperties[propList.size()];
        propList.toArray(this.props);

        /* For the suggestion list */
        Term[] terms = new Term[this.props.length];
        for (int i = 0; i < terms.length; i++) {
            terms[i] = this.props[i].goTerm;
        }
        this.subtermFilterComposite.setSupportedTerms(terms);

    }

    /**
     * Create the subterm filter.
     *
     * @param parent
     */
    protected void createSubtermFilter(Composite parent)
    {
        this.subtermFilterComposite = new TermFilterSelectionComposite(parent, 0);
        this.subtermFilterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.subtermFilterComposite.setNewTermAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                AbstractResultComposite.this.emanatingTerm =
                    AbstractResultComposite.this.subtermFilterComposite.getSelectedTerm();
                newEmanatingTermSelected(AbstractResultComposite.this.emanatingTerm);
            }
        });
    }

    /**
     * Overwritable method which is called after choosing a new emananting term.
     *
     * @param term
     */
    protected void newEmanatingTermSelected(Term term)
    {
    }

    /**
     * Returns the emanating term (which may be null).
     *
     * @return
     */
    public Term getEmanatingTerm()
    {
        return this.emanatingTerm;
    }

    /**
     * Returns whether the given term should be displayed.
     *
     * @param term
     * @return
     */
    protected boolean shouldTermDisplayed(Term term)
    {
        if (this.emanatingTerm != null && !this.emanatingTerm.equals(term)) {
            if (!(this.go.existsPath(this.emanatingTerm.getID(), term.getID()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes the checked Terms, i.e. after this call, now term is checked.
     */
    protected void initializeCheckedTerms()
    {
        this.checkedTerms = new HashSet<>();
    }

    protected void addToCheckedTerms(TermID tid)
    {
        this.checkedTerms.add(tid);
    }

    protected void removeFromCheckedTerms(TermID tid)
    {
        this.checkedTerms.remove(tid);
    }

    /**
     * Returns whether the given term is checked.
     *
     * @param tid
     * @return
     */
    protected boolean isCheckedTerm(TermID tid)
    {
        return this.checkedTerms.contains(tid);
    }

    protected Collection<TermID> getCheckedTermsCollection()
    {
        return this.checkedTerms;
    }

    /**
     * Returns the number of checked terms.
     *
     * @return
     */
    protected int getNumberOfCheckedTerms()
    {
        return this.checkedTerms.size();
    }

    /**
     * Helper function to create a new graph generation thread.
     *
     * @param finished
     * @return
     */
    protected GraphGenerationThread createGraphGenerationThread(IGraphGenerationFinished finished,
        AbstractDotAttributesProvider attrProvider)
    {
        GraphGenerationThread ggt =
            new GraphGenerationThread(getDisplay(), GlobalPreferences.getDOTPath(), finished, attrProvider);
        ggt.go = this.go;
        ggt.emanatingTerm = getEmanatingTerm();
        ggt.leafTerms.addAll(getCheckedTermsCollection());
        ggt.result = this.result;
        return ggt;
    }

    /**
     * Update the displayed graph.
     */
    public void updateDisplayedGraph()
    {
        GraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished()
        {
            @Override
            public void finished(boolean success, String message, File pngFile, File dotFile)
            {
                if (success) {
                    try {
                        AbstractResultComposite.this.graphVisual.setLayoutedDotFile(dotFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
                    mbox.setText("Ontologizer - Error");
                    mbox.open();
                }
            }
        }, this.dotNodeAttributesProvider);
        ggt.start();
    }

    /**
     * Sets the dot node attributes provider.
     *
     * @param attributesProvider
     */
    protected void setDotNodeAttributesProvider(AbstractDotAttributesProvider attributesProvider)
    {
        this.dotNodeAttributesProvider = attributesProvider;
    }
}
