/*
 * Created on 15.04.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.result;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries.SeriesType;

import ontologizer.GlobalPreferences;
import ontologizer.association.Gene2Associations;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOEnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.calculation.b2g.FixedAlphaBetaScore;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Namespace;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.types.ByteString;

/**
 * This is the composite for the result for a EnrichedGOTermsResult results.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGOTermsComposite extends AbstractResultComposite implements IGraphAction, ITableAction
{
    private static Logger logger = Logger.getLogger(EnrichedGOTermsComposite.class.getCanonicalName());

    /** Defines the significance resolution */
    private static final int SIGNIFICANCE_RESOLUTION = 10000;

    private static final int ACTIVITY = 0;

    private static final int GOID = 1;

    private static final int NAME = 2;

    private static final int NAMESPACE = 3;

    private static final int PVAL = 4;

    private static final int ADJPVAL = 5;

    private static final int MARG = 6;

    private static final int RANK = 7;

    private static final int POP = 8;

    private static final int STUDY = 9;

    private static final int LAST = 10;

    /**
     * Indicates whether pvalues should be handled as marginals (TODO: needs to be done in a more abstract way)
     */
    private static boolean useMarginal = false;

    /* Texts */
    private static String NOBROWSER_TOOLTIP = "The SWT browser widget could not " +
        "be instantiated. Please ensure that your system fulfills the requirements " +
        "of the SWT browser. Further information can be obtained from the FAQ at " +
        "http://www.eclipse.org/swt.";

    /* Attributes */
    private SashForm verticalSashForm;

    private Composite tableComposite = null;

    private Table table = null;

    private TableColumn[] columns = new TableColumn[LAST];

    private SashForm termSashForm;

    private CTabFolder tableFolder;

    private GraphCanvas graphVisual = null;

    private Composite significanceComposite;

    private Label significanceLabel;

    private Spinner significanceSpinner;

    private Link significanceLink;

    /** The browser displaying information about currently selected term */
    private Browser browser = null;

    /** Uses as fallback if browser is not available */
    private StyledText styledText = null;

    private int sortColumn = -1;

    private int sortDirection = SWT.UP;

    /** Maps the terms to a P-value rank */
    private HashMap<TermID, Integer> termID2PValueRank;

    /** Used to find the position of a table line given the term id */
    private HashMap<Integer, Integer> termID2ListLine;

    /** Used to find the proper term position given the table position */
    private HashMap<Integer, Integer> line2TermPos;

    /** Used to get the color of a term. The color is determined by the term's significance */
    private HashMap<TermID, Color> termID2Color;

    /** Color for the alpha series */
    private Color alphaColor;

    /** Color for the beta series */
    private Color betaColor;

    /** Has the user changed set set of checked terms manually? */
    private boolean checkedTermsChanged;

    /** The results associated to this composite */
    private EnrichedGOTermsResult result;

    /**
     * Constructor.
     *
     * @param parent
     * @param style
     * @param go
     */
    public EnrichedGOTermsComposite(Composite parent, int style)
    {
        super(parent, style);

        setLayout(new GridLayout());
        setSize(new Point(500, 500));

        createSubtermFilter(this);
        createSashForm();
        this.verticalSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.alphaColor = getDisplay().getSystemColor(SWT.COLOR_RED);
        this.betaColor = getDisplay().getSystemColor(SWT.COLOR_BLUE);

        /* Add the dispose listener */
        addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                disposeSignificanceColors();
            }
        });

    }

    /**
     * Sets the displayed results.
     *
     * @param result
     */
    public void setResult(EnrichedGOTermsResult result)
    {
        super.setResult(result);

        this.result = result;

        if (result instanceof Bayes2GOEnrichedGOTermsResult) {
            Bayes2GOEnrichedGOTermsResult b2gResult = (Bayes2GOEnrichedGOTermsResult) result;
            FixedAlphaBetaScore fixedScore;
            if (b2gResult.getScore() instanceof FixedAlphaBetaScore) {
                fixedScore = (FixedAlphaBetaScore) b2gResult.getScore();
                this.tableFolder.setSingle(false);

                Composite chartComposite = new Composite(this.tableFolder, 0);
                chartComposite.setLayout(new FillLayout());

                Chart chart = new Chart(chartComposite, SWT.NONE);
                chart.getTitle().setVisible(false);
                chart.getAxisSet().getYAxis(0).getTitle().setText("Posterior Probability");
                chart.getAxisSet().getXAxis(0).getTitle().setVisible(false);
                ILineSeries alphaSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "alpha");
                alphaSeries.setXSeries(fixedScore.getAlphaValues());
                alphaSeries.setYSeries(fixedScore.getAlphaDistribution());
                alphaSeries.setAntialias(SWT.ON);
                alphaSeries.setLineColor(this.alphaColor);
                alphaSeries.setLineColor(this.alphaColor);

                ILineSeries betaSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "beta");
                betaSeries.setXSeries(fixedScore.getBetaValues());
                betaSeries.setYSeries(fixedScore.getBetaDistribution());
                betaSeries.setAntialias(SWT.ON);
                betaSeries.setLineColor(this.betaColor);
                betaSeries.setLineColor(this.betaColor);

                CTabItem parameterItem = new CTabItem(this.tableFolder, 0);
                parameterItem.setText("Parameter");
                parameterItem.setControl(chartComposite);

            }

            useMarginal = true;

            /*
             * This hides the given columns. Should perhaps find better variant to hide them (e.g., on creation time)
             */
            this.columns[PVAL].setWidth(0);
            this.columns[PVAL].setResizable(false);
            this.columns[ADJPVAL].setWidth(0);
            this.columns[ADJPVAL].setResizable(false);

            /* Also set a new default significance selection */
            this.significanceSpinner.setSelection(SIGNIFICANCE_RESOLUTION / 2);
            this.significanceLabel.setText("Threshold (higher is more important)");

        } else {
            useMarginal = false;
            this.columns[MARG].setWidth(0);
            this.columns[MARG].setResizable(false);
            this.significanceLabel.setText("Threshold (lower is more important)");
        }
        initializeCheckedTerms();

        /*
         * We sort the array now by adjusted p-values (if no decision could be made, by normal p-values) in order to get
         * the terms' rank
         */
        Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
        {
            @Override
            public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
            {
                if (o1.p_adjusted < o2.p_adjusted) {
                    return -1;
                }
                if (o1.p_adjusted > o2.p_adjusted) {
                    return 1;
                }
                if (o1.p < o2.p) {
                    return -1;
                }
                if (o1.p > o2.p) {
                    return 1;
                }
                return 0;
            }
        });
        this.termID2PValueRank = new HashMap<>();
        for (int rank = 0; rank < this.props.length; rank++) {
            this.termID2PValueRank.put(this.props[rank].goTerm.getID(), rank + 1);
        }

        prepareSignificanceColors();
        buildCheckedTermHashSet();
        populateTable();
        updateSignificanceText();
    }

    /**
     * Populate the table. Respects sorting settings.
     */
    void populateTable()
    {
        int entryNumber;
        final int direction;

        if (this.sortDirection == SWT.UP) {
            direction = 1;
        } else {
            direction = -1;
        }

        switch (this.sortColumn) {
            case ACTIVITY:
                Arrays.sort(this.props, getCheckedComparator(direction));
                break;
            case GOID:
                Arrays.sort(this.props, new GOIDComparator(direction));
                break;
            case NAME:
                Arrays.sort(this.props, new GONameComparator(direction));
                break;

            case RANK:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        int r;

                        r = EnrichedGOTermsComposite.this.termID2PValueRank.get(o1.goTerm.getID())
                            - EnrichedGOTermsComposite.this.termID2PValueRank.get(o2.goTerm.getID());

                        r *= direction;
                        return r;
                    }
                });
                break;

            case ADJPVAL:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        int r;

                        if (o1.p_adjusted < o2.p_adjusted) {
                            r = -1;
                        } else if (o1.p_adjusted > o2.p_adjusted) {
                            r = 1;
                        } else if (o1.p < o2.p) {
                            r = -1;
                        } else if (o1.p > o2.p) {
                            r = 1;
                        } else {
                            r = 0;
                        }

                        r *= direction;
                        return r;
                    }
                });
                break;

            case MARG:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        int r;

                        if (o1 instanceof Bayes2GOGOTermProperties && o2 instanceof Bayes2GOGOTermProperties) {
                            double m1 = ((Bayes2GOGOTermProperties) o1).marg;
                            double m2 = ((Bayes2GOGOTermProperties) o2).marg;

                            if (m1 < m2) {
                                r = -1;
                            } else if (m1 > m2) {
                                r = 1;
                            } else {
                                r = 0;
                            }
                        } else {
                            if (o1.p < o2.p) {
                                r = -1;
                            } else if (o1.p > o2.p) {
                                r = 1;
                            } else {
                                r = 0;
                            }
                        }
                        r *= direction;
                        return r;
                    }
                });
                break;
            case PVAL:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        int r;

                        if (o1.p < o2.p) {
                            r = -1;
                        } else if (o1.p > o2.p) {
                            r = 1;
                        } else {
                            r = 0;
                        }
                        r *= direction;
                        return r;
                    }
                });
                break;

            case POP:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        return (o1.annotatedPopulationGenes - o2.annotatedPopulationGenes) * direction;
                    }
                });
                break;

            case STUDY:
                Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
                {
                    @Override
                    public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                    {
                        return (o1.annotatedStudyGenes - o2.annotatedStudyGenes) * direction;
                    }
                });
                break;
        }

        this.termID2ListLine = new HashMap<>();
        this.line2TermPos = new HashMap<>();

        entryNumber = 0;

        for (int i = 0; i < this.props.length; i++) {
            if (!shouldTermDisplayed(this.props[i].goTerm)) {
                continue;
            }
            this.termID2ListLine.put(this.props[i].goTerm.getID().id, entryNumber);
            this.line2TermPos.put(entryNumber, i);
            entryNumber++;
        }

        this.table.clearAll();
        this.table.setItemCount(entryNumber);
    }

    /**
     * Updates the graph. I.e. selects the node currently selected within the table.
     */
    private void updateGraph()
    {
        int idx = this.table.getSelectionIndex();
        if (idx < 0) {
            return;
        }

        TableItem item = this.table.getItem(idx);
        Term goTerm = (Term) item.getData("term");
        if (goTerm != null) {
            this.graphVisual.selectNode(Integer.toString(goTerm.getID().id));
        }
    }

    /**
     * Update the browser according to the currently selected table entry.
     */
    private void updateBrowser()
    {
        int idx = this.table.getSelectionIndex();
        if (idx < 0) {
            return;
        }

        TableItem item = this.table.getItem(idx);
        StringBuilder str = new StringBuilder();

        if (this.browser != null) {
            str.append("<html><body>");
            if (item != null) {
                str.append(item.getText(NAME));
                str.append(" (");
                str.append(item.getText(GOID));
                str.append(")");
                str.append("<br />");

                Term goTerm = (Term) item.getData("term");
                if (goTerm != null) {
                    Set<TermID> ancestors = this.go.getTermParents(goTerm.getID());
                    if (ancestors != null) {
                        str.append("<br />Parents: ");
                        str.append("<div style=\"margin-left:20px;\">");
                        str.append(createTermString(ancestors));
                        str.append("</div>");
                    }

                    Set<TermID> siblings = this.go.getTermsSiblings(goTerm.getID());
                    if (siblings != null) {
                        str.append("<br />Siblings: ");
                        str.append("<div style=\"margin-left:20px;\">");
                        str.append(createTermString(siblings));
                        str.append("</div>");
                    }

                    Set<TermID> descendants = this.go.getTermChildren(goTerm.getID());
                    if (descendants != null) {
                        str.append("<br />Children: ");
                        str.append("<div style=\"margin-left:20px;\">");
                        str.append(createTermString(descendants));
                        str.append("</div>");
                    }

                    String def = goTerm.getDefinition();
                    if (def == null) {
                        def = "No definition available";
                    }
                    str.append("<br />Definition: ");
                    str.append("<font size=\"-1\">");
                    str.append(def);
                    str.append("</font>");

                    str.append("<br /><br />");

                    str.append("<h3>Annotated Gene Products</h3>");
                    /* Enumerate the genes */
                    GOTermEnumerator enumerator =
                        this.result.getStudySet().enumerateGOTerms(this.go, this.associationContainer);
                    GOTermAnnotatedGenes annotatedGenes = enumerator.getAnnotatedGenes(goTerm.getID());

                    HashSet<String> directGenes = new HashSet<>();
                    for (ByteString gene : annotatedGenes.directAnnotated) {
                        directGenes.add(gene.toString());
                    }

                    int i = 0;
                    int totalGenesCount = annotatedGenes.totalAnnotatedCount();
                    String[] totalGenes = new String[totalGenesCount];

                    for (ByteString gene : annotatedGenes.totalAnnotated) {
                        totalGenes[i++] = gene.toString();
                    }
                    Arrays.sort(totalGenes);

                    for (i = 0; i < totalGenesCount; i++) {
                        if (i != 0) {
                            str.append(", ");
                        }
                        str.append("<A HREF=\"gene:");
                        str.append(totalGenes[i]);
                        str.append("\">");
                        if (directGenes.contains(totalGenes[i])) {
                            str.append("<b>");
                            str.append(totalGenes[i]);
                            str.append("</b>");
                        } else {
                            str.append(totalGenes[i]);
                        }
                        str.append("</A>");
                    }
                }
            }
            str.append("</body></html>");
            this.browser.setText(str.toString());
        } else {
            str.append(item.getText(1));
            str.append(" (");
            str.append(item.getText(0));
            str.append(")");
            str.append("\n\n");
            Term goTerm = (Term) item.getData("term");
            if (goTerm != null) {
                String def = goTerm.getDefinition();
                if (def == null) {
                    def = "No definition available";
                }
                str.append(def);
            }

            this.styledText.setText(str.toString());
        }
    }

    /**
     * Updates the browser for the given gene.
     *
     * @param gene
     */
    private void updateBrowserWithGene(String gene)
    {
        StringBuilder str = new StringBuilder();
        str.append("<html><body>");
        str.append(gene);
        str.append("<br /><br />");

        Gene2Associations assocs = this.associationContainer.get(new ByteString(gene));
        if (assocs != null) {
            str.append("Directly annotated by:");
            str.append("<div style=\"margin-left:20px;\">");
            HashSet<TermID> set = new HashSet<>();
            set.addAll(assocs.getAssociations());
            str.append(createTermString(set));
            str.append("</div>");
        }

        str.append("</body></html>");
        this.browser.setText(str.toString());
    }

    /**
     * Updates the number of significance terms.
     */
    void updateSignificanceText()
    {
        double level = getSignificanceLevel();
        int count = 0;
        int total = this.line2TermPos.size();

        /* count the number of significant entries */
        for (Integer i : this.line2TermPos.keySet()) {
            AbstractGOTermProperties prop = this.props[this.line2TermPos.get(i)];
            if (prop.isSignificant(level)) {
                count++;
            }
        }

        /* display */
        StringBuilder str = new StringBuilder();

        str.append(getNumberOfCheckedTerms());
        str.append(" (");
        str.append("<a href=\"none\">None</a>");
        str.append(") / ");
        str.append("<a href=\"significant\">");
        str.append(count);
        str.append("</a>");
        str.append(" / ");
        str.append("<a href=\"all\">");
        str.append(total);
        str.append("</a>");

        this.significanceLink.setText(str.toString());

        str.setLength(0);
        str.append("The first value shows the number of checked terms.\n");
        str.append("The second value shows the number of terms ");
        if (useMarginal) {
            str.append("above");
        } else {
            str.append("below");
        }
        str.append(" the given threshold.\n");
        str.append("The third value shows the total number of terms that are displayed within the table.");
        this.significanceLink.setToolTipText(str.toString());
    }

    private void buildCheckedTermHashSet()
    {
        initializeCheckedTerms();

        double level = getSignificanceLevel();

        /* count the number of significant entries */
        for (AbstractGOTermProperties prop : this.props) {
            if (prop.isSignificant(level)) {
                addToCheckedTerms(prop.goTerm.getID());
            }
        }
    }

    /**
     * Dispose all colors used for significant terms.
     */
    private void disposeSignificanceColors()
    {
        if (this.termID2Color == null) {
            return;
        }
        for (Color col : this.termID2Color.values()) {
            col.dispose();
        }
        this.termID2Color = null;
    }

    /**
     * Allocate colors used for significant terms.
     */
    private void prepareSignificanceColors()
    {
        if (this.termID2Color != null) {
            disposeSignificanceColors();
        }

        double level = getSignificanceLevel();
        int count = 0;

        /* count the number of significant entries */
        for (AbstractGOTermProperties prop : this.props) {
            if (prop.isSignificant(level)) {
                count++;
            }
        }

        this.termID2Color = new HashMap<>();

        for (AbstractGOTermProperties prop : this.props) {
            if (prop.isSignificant(level)) {
                /* See class StudySetResult */
                float hue, saturation, brightness;

                /*
                 * Use the rank to determine the saturation We want that more significant nodes have more saturation,
                 * but we avoid having significant nodes with too less saturation (at least 0.2)
                 */
                int rank = this.termID2PValueRank.get(prop.goTerm.getID()) - 1;
                assert (rank < count);
                saturation = 1.0f - (((float) rank + 1) / count) * 0.8f;

                /* Always full brightness */
                brightness = 1.0f;

                /* Hue depends on namespace */
                switch (Namespace.getNamespaceEnum(prop.goTerm.getNamespace())) {
                    case BIOLOGICAL_PROCESS:
                        hue = 120.f;
                        break;
                    case MOLECULAR_FUNCTION:
                        hue = 60.f;
                        break;
                    case CELLULAR_COMPONENT:
                        hue = 300.f;
                        break;
                    default:
                        hue = 0.f;
                        saturation = 0.f;
                }

                this.termID2Color.put(prop.goTerm.getID(),
                    new Color(getDisplay(), new RGB(hue, saturation, brightness)));
            }
        }
    }

    /**
     * Returns the proper formatted html string derived from the ids to the given str. Only terms, which actually have
     * annotations are emitted.
     *
     * @param str
     * @param termids
     */
    private String createTermString(final Set<TermID> termids)
    {
        StringBuilder str = new StringBuilder();

        ArrayList<Term> terms = new ArrayList<>();

        for (TermID termid : termids) {
            /* Is the term displayed? */
            Integer lineIdx = this.termID2ListLine.get(termid.id);
            if (lineIdx != null) {
                Term term = this.go.getTerm(termid);
                if (term != null) {
                    terms.add(term);
                }
            }
        }

        Collections.sort(terms, new Comparator<Term>()
        {
            @Override
            public int compare(Term o1, Term o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (Term term : terms) {
            TermID termid = term.getID();
            str.append("<A HREF=\"termid:");
            str.append(termid.id);
            str.append("\">");
            str.append(term.getName());
            str.append("</A>");

            if (term != null) {
                AbstractGOTermProperties prop = this.result.getGOTermProperties(term);
                if (prop.isSignificant(getSignificanceLevel())) {
                    str.append("(*)");
                }
            }

            str.append("<br />");
        }

        return str.toString();
    }

    /**
     * This method initializes sashForm.
     */
    private void createSashForm()
    {
        this.verticalSashForm = new SashForm(this, SWT.VERTICAL);

        createTableComposite();
        createBrowser();

        this.verticalSashForm.setWeights(new int[] { 2, 1 });
    }

    /**
     * This method initializes table.
     */
    private void createTableComposite()
    {
        /* Create the sorting listener used for the table */
        SelectionAdapter sortingListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent ev)
            {
                TableColumn col = (TableColumn) ev.widget;
                if (EnrichedGOTermsComposite.this.table.getSortColumn() == col) {
                    if (EnrichedGOTermsComposite.this.table.getSortDirection() == SWT.UP) {
                        EnrichedGOTermsComposite.this.sortDirection = SWT.DOWN;
                    } else {
                        EnrichedGOTermsComposite.this.sortDirection = SWT.UP;
                    }
                } else {
                    EnrichedGOTermsComposite.this.sortDirection = SWT.UP;
                }

                EnrichedGOTermsComposite.this.sortColumn = (Integer) col.getData("column");

                TableItem selectedItem = null;
                TableItem[] selectedItems = EnrichedGOTermsComposite.this.table.getSelection();
                if (selectedItems != null && selectedItems.length > 0) {
                    selectedItem = selectedItems[0];
                }

                EnrichedGOTermsComposite.this.table.setSortColumn(col);
                EnrichedGOTermsComposite.this.table.setSortDirection(EnrichedGOTermsComposite.this.sortDirection);

                populateTable();

                if (selectedItem != null) {
                    Term selectedTerm = (Term) selectedItem.getData("term");
                    EnrichedGOTermsComposite.this.table
                        .setSelection(EnrichedGOTermsComposite.this.termID2ListLine.get(selectedTerm.getID().id));
                }

            }
        };

        /* Term Overview Sash Form */
        this.termSashForm = new SashForm(this.verticalSashForm, SWT.HORIZONTAL);

        this.tableFolder = new CTabFolder(this.termSashForm, SWT.BORDER);
        this.tableFolder.setSingle(true);
        this.tableFolder.setMaximizeVisible(true);
        this.tableFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        CTabItem tableCTabItem = new CTabItem(this.tableFolder, 0);
        tableCTabItem.setText("Table");
        this.tableFolder.setSelection(0);
        this.tableFolder.addCTabFolder2Listener(new CTabFolder2Adapter()
        {
            @Override
            public void maximize(CTabFolderEvent event)
            {
                EnrichedGOTermsComposite.this.tableFolder.setMaximized(true);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(false);
                EnrichedGOTermsComposite.this.verticalSashForm
                    .setMaximizedControl(EnrichedGOTermsComposite.this.termSashForm);
                EnrichedGOTermsComposite.this.termSashForm
                    .setMaximizedControl(EnrichedGOTermsComposite.this.tableFolder);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(true);
            }

            @Override
            public void restore(CTabFolderEvent event)
            {
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(false);
                EnrichedGOTermsComposite.this.tableFolder.setMaximized(false);
                EnrichedGOTermsComposite.this.verticalSashForm.setMaximizedControl(null);
                EnrichedGOTermsComposite.this.termSashForm.setMaximizedControl(null);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(true);
            }
        });

        this.tableComposite = new Composite(this.tableFolder, SWT.NONE);
        tableCTabItem.setControl(this.tableComposite);
        this.tableComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

        createGraphComposite(this.termSashForm);

        /* Initially, we hide the graph */
        this.termSashForm.setMaximizedControl(this.tableFolder);

        /* Table widget */
        this.table = new Table(this.tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.VIRTUAL);
        GridData tableGridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        this.table.setLayoutData(tableGridData);
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);
        this.table.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (e.detail == SWT.CHECK) {
                    TableItem ti = (TableItem) e.item;
                    Term term = (Term) ti.getData("term");
                    if (ti.getChecked()) {
                        addToCheckedTerms(term.getID());
                    } else {
                        removeFromCheckedTerms(term.getID());
                    }
                    EnrichedGOTermsComposite.this.checkedTermsChanged = true;
                    updateSignificanceText();
                }
                updateBrowser();
                updateGraph();
            }
        });
        this.table.addListener(SWT.SetData, new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {
                TableItem item = (TableItem) e.item;
                Integer index = EnrichedGOTermsComposite.this.line2TermPos.get(e.index);
                if (index != null) {
                    AbstractGOTermProperties prop = EnrichedGOTermsComposite.this.props[index];
                    item.setText(GOID, prop.goTerm.getIDAsString());
                    item.setText(NAME, prop.goTerm.getName());
                    item.setText(NAMESPACE, prop.goTerm.getNamespaceAsAbbrevString());
                    if (useMarginal) {
                        if (prop instanceof Bayes2GOGOTermProperties) {
                            Bayes2GOGOTermProperties b2gp = (Bayes2GOGOTermProperties) prop;
                            item.setText(MARG, String.format("%.3g", b2gp.marg));
                        }

                    } else {
                        item.setText(PVAL, String.format("%.3g", prop.p));
                        item.setText(ADJPVAL, String.format("%.3g", prop.p_adjusted));
                    }
                    item.setText(RANK,
                        EnrichedGOTermsComposite.this.termID2PValueRank.get(prop.goTerm.getID()).toString());
                    item.setText(POP, Integer.toString(prop.annotatedPopulationGenes));
                    item.setText(STUDY, Integer.toString(prop.annotatedStudyGenes));

                    item.setData("term", prop.goTerm);

                    if (isCheckedTerm(prop.goTerm.getID())) {
                        item.setChecked(true);
                    }

                    Color background = EnrichedGOTermsComposite.this.termID2Color.get(prop.goTerm.getID());
                    if (useMarginal) {
                        item.setBackground(MARG, background);
                    } else {
                        item.setBackground(PVAL, background);
                        item.setBackground(ADJPVAL, background);
                    }

                }
            }
        });

        /* Table columns */
        for (int i = 0; i < LAST; i++) {
            this.columns[i] = new TableColumn(this.table, SWT.NONE);
            this.columns[i].addSelectionListener(sortingListener);
            this.columns[i].setData("column", new Integer(i));
        }
        this.columns[ACTIVITY].setText("");
        this.columns[GOID].setText("GO ID");
        this.columns[NAME].setText("Name");
        this.columns[NAMESPACE].setText("NSP");
        this.columns[NAMESPACE].setAlignment(SWT.CENTER);
        this.columns[NAMESPACE].setToolTipText("Namespace or sub ontology");
        this.columns[PVAL].setText("P-Value");
        this.columns[PVAL].setAlignment(SWT.RIGHT);
        this.columns[ADJPVAL].setText("Adj. P-Value");
        this.columns[ADJPVAL].setToolTipText("Adjusted P-Value");
        this.columns[ADJPVAL].setAlignment(SWT.RIGHT);
        this.columns[MARG].setText("Marginal");
        this.columns[MARG].setToolTipText("Marginal probability");
        this.columns[MARG].setAlignment(SWT.RIGHT);
        this.columns[RANK].setText("Rank");
        this.columns[RANK].setToolTipText("The rank of the term in the list");
        this.columns[RANK].setAlignment(SWT.RIGHT);
        this.columns[POP].setText("Pop. Count");
        this.columns[POP].setAlignment(SWT.RIGHT);
        this.columns[POP].setToolTipText("Number of entries within the population set annotated to the term");
        this.columns[STUDY].setText("Study Count");
        this.columns[STUDY].setAlignment(SWT.RIGHT);
        this.columns[STUDY].setToolTipText("Number of entries within the study set annotated to the term");
        for (int i = 0; i < LAST; i++) {
            this.columns[i].pack();
        }
        /* Ensure useful columns sizes */
        if (this.columns[ACTIVITY].getWidth() < 20) {
            this.columns[ACTIVITY].setWidth(20);
        }
        if (this.columns[GOID].getWidth() < 80) {
            this.columns[GOID].setWidth(90);
        }
        if (this.columns[NAME].getWidth() < 250) {
            this.columns[NAME].setWidth(250);
        }

        /* Significance composite */
        this.significanceComposite = new Composite(this.tableComposite, SWT.NONE);
        this.significanceComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
        this.significanceComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.significanceLink = new Link(this.significanceComposite, SWT.READ_ONLY);
        this.significanceLink.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        this.significanceLink.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        this.significanceLink.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (e.text.equals("none")) {
                    /* Clear the selections */
                    initializeCheckedTerms();
                    EnrichedGOTermsComposite.this.checkedTermsChanged = true;
                } else if (e.text.equals("significant")) {
                    /* Adjust the selection to the significant ones */
                    buildCheckedTermHashSet();
                    EnrichedGOTermsComposite.this.checkedTermsChanged = false;
                } else if (e.text.equals("all")) {
                    /* Select all terms */
                    initializeCheckedTerms();
                    for (AbstractGOTermProperties prop : EnrichedGOTermsComposite.this.props) {
                        addToCheckedTerms(prop.goTerm.getID());
                    }
                    EnrichedGOTermsComposite.this.checkedTermsChanged = true;
                }
                EnrichedGOTermsComposite.this.table.clearAll();
                updateSignificanceText();
            }
        });
        this.significanceLabel = new Label(this.significanceComposite, SWT.NONE);
        this.significanceLabel.setText("Threshold");
        this.significanceSpinner = new Spinner(this.significanceComposite, SWT.BORDER);
        this.significanceSpinner.setMaximum(SIGNIFICANCE_RESOLUTION);
        this.significanceSpinner.setDigits(4);
        this.significanceSpinner.setSelection(SIGNIFICANCE_RESOLUTION / 10);
        this.significanceSpinner.setIncrement(10);
        this.significanceSpinner.setPageIncrement(1);
        this.significanceSpinner.setToolTipText(
            "The significance level determines the threshold of terms\nthat are considered as significantly enriched, and thus\nbeing colored.");
        this.significanceSpinner.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateSignificanceText();
                disposeSignificanceColors();
                prepareSignificanceColors();
                if (!EnrichedGOTermsComposite.this.checkedTermsChanged) {
                    buildCheckedTermHashSet();
                }
                EnrichedGOTermsComposite.this.table.clearAll();
            }
        });
    }

    /**
     * Initialized the graph composite.
     *
     * @param parent
     */
    private void createGraphComposite(Composite parent)
    {
        final CTabFolder graphFolder = new CTabFolder(parent, SWT.BORDER);
        graphFolder.setMaximizeVisible(true);
        graphFolder.setSingle(true);
        graphFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        CTabItem graphItem = new CTabItem(graphFolder, 0);
        graphItem.setText("Graph Preview");

        this.graphVisual = new GraphCanvas(graphFolder, SWT.BORDER);
        this.graphVisual.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    int id = Integer.parseInt(e.text);
                    Integer selection = EnrichedGOTermsComposite.this.termID2ListLine.get(id);
                    if (selection != null) {
                        EnrichedGOTermsComposite.this.table.setSelection(selection);
                        updateBrowser();
                    }
                } catch (Exception ex) {
                }
            }
        });
        graphItem.setControl(this.graphVisual);

        /* The context menu */
        Menu contextMenu = this.graphVisual.getMenu();
        new MenuItem(contextMenu, SWT.SEPARATOR);
        final MenuItem childTermsMenuItem = new MenuItem(contextMenu, 0);
        childTermsMenuItem.setText("Enable Child Terms");
        final MenuItem annotateMenuItem = new MenuItem(contextMenu, 0);
        annotateMenuItem.setText("Copy Annotated Genes");
        final MenuItem notAnnotatedMenuItem = new MenuItem(contextMenu, 0);
        notAnnotatedMenuItem.setText("Copy Not Annotated Genes");

        SelectionAdapter menuItemAdapter = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    String stringId = EnrichedGOTermsComposite.this.graphVisual.getNameOfCurrentSelectedNode();
                    int id = Integer.parseInt(stringId);

                    if (e.widget.equals(childTermsMenuItem)) {
                        Set<TermID> termIDs = EnrichedGOTermsComposite.this.go.getTermChildren(new TermID(id));
                        for (TermID termID : termIDs) {
                            Integer selection = EnrichedGOTermsComposite.this.termID2ListLine.get(termID.id);
                            if (selection != null) {
                                addToCheckedTerms(termID);
                                EnrichedGOTermsComposite.this.table.getItem(selection).setChecked(true);
                                EnrichedGOTermsComposite.this.table.clear(selection);
                            }
                        }
                    } else if (e.widget.equals(annotateMenuItem)) {
                        GOTermEnumerator enumerator =
                            EnrichedGOTermsComposite.this.result.getStudySet().enumerateGOTerms(
                                EnrichedGOTermsComposite.this.go, EnrichedGOTermsComposite.this.associationContainer);

                        Clipboard clipboard = new Clipboard(getDisplay());
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);

                        for (ByteString gene : enumerator.getAnnotatedGenes(new TermID(id)).totalAnnotated) {
                            String desc = EnrichedGOTermsComposite.this.result.getStudySet().getGeneDescription(gene);

                            pw.append(gene.toString());
                            pw.append("\t");
                            if (desc != null) {
                                pw.append(desc);
                            }
                            pw.println();
                        }

                        pw.flush();

                        clipboard.setContents(new Object[] { sw.toString() },
                            new Transfer[] { TextTransfer.getInstance() });
                        clipboard.dispose();
                    } else if (e.widget.equals(notAnnotatedMenuItem)) {
                        GOTermEnumerator enumerator =
                            EnrichedGOTermsComposite.this.result.getStudySet().enumerateGOTerms(
                                EnrichedGOTermsComposite.this.go, EnrichedGOTermsComposite.this.associationContainer);

                        /* Build hashset in order to have constant time access */
                        HashSet<ByteString> annotatedGenes = new HashSet<>();
                        annotatedGenes.addAll(enumerator.getAnnotatedGenes(new TermID(id)).totalAnnotated);

                        Clipboard clipboard = new Clipboard(getDisplay());
                        StringBuilder str = new StringBuilder();

                        for (ByteString gene : EnrichedGOTermsComposite.this.result.getStudySet()) {
                            if (!annotatedGenes.contains(gene)) {
                                String desc =
                                    EnrichedGOTermsComposite.this.result.getStudySet().getGeneDescription(gene);

                                str.append(gene.toString());
                                str.append("\t");
                                if (desc != null) {
                                    str.append(
                                        EnrichedGOTermsComposite.this.result.getStudySet().getGeneDescription(gene));
                                }
                                str.append("\n");
                            }
                        }

                        clipboard.setContents(new Object[] { str.toString() },
                            new Transfer[] { TextTransfer.getInstance() });
                        clipboard.dispose();
                    }
                } catch (Exception ex) {
                }
            }
        };

        childTermsMenuItem.addSelectionListener(menuItemAdapter);
        annotateMenuItem.addSelectionListener(menuItemAdapter);
        notAnnotatedMenuItem.addSelectionListener(menuItemAdapter);

        graphItem.setControl(this.graphVisual);
        graphFolder.setSelection(0);
        graphFolder.addCTabFolder2Listener(new CTabFolder2Adapter()
        {
            @Override
            public void maximize(CTabFolderEvent event)
            {
                graphFolder.setMaximized(true);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(false);
                EnrichedGOTermsComposite.this.verticalSashForm
                    .setMaximizedControl(EnrichedGOTermsComposite.this.termSashForm);
                EnrichedGOTermsComposite.this.termSashForm.setMaximizedControl(graphFolder);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(true);
            }

            @Override
            public void restore(CTabFolderEvent event)
            {
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(false);
                graphFolder.setMaximized(false);
                EnrichedGOTermsComposite.this.verticalSashForm.setMaximizedControl(null);
                EnrichedGOTermsComposite.this.termSashForm.setMaximizedControl(null);
                EnrichedGOTermsComposite.this.verticalSashForm.setRedraw(true);
            }

        });
    }

    /**
     * This method initializes browser
     */
    private void createBrowser()
    {
        /*
         * Use an auxiliary composite for the browser, because the browser actually is instanciated even if it fails
         * (issue was reported and will be fixed in post Eclipse 3.2)
         */
        final Composite browserComposite = new Composite(this.verticalSashForm, 0);
        browserComposite.setLayout(new FillLayout());
        try {
            final CTabFolder browserFolder = new CTabFolder(browserComposite, SWT.BORDER);
            browserFolder.setMaximizeVisible(true);
            browserFolder.setSingle(true);
            browserFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

            CTabItem browserTabItem = new CTabItem(browserFolder, 0);
            browserTabItem.setText("Browser");
            this.browser = new Browser(browserFolder, SWT.BORDER);
            this.browser.addLocationListener(new LocationAdapter()
            {
                @Override
                public void changing(LocationEvent event)
                {
                    if (event.location.startsWith("termid:")) {
                        /* TODO handle the root term in a better way */
                        try {
                            int id = Integer.parseInt(event.location.substring(7));
                            Integer selection = EnrichedGOTermsComposite.this.termID2ListLine.get(id);
                            if (selection != null) {
                                EnrichedGOTermsComposite.this.table.setSelection(selection);
                                updateBrowser();
                                updateGraph();
                            }
                        } catch (Exception e) {
                        }

                        /*
                         * We return such that we won't accept the url change. This disables the usage of the back
                         * button which is of course not intuitive. But I found no way how to make the back button
                         * usable as setting the text of the browser is handled like a new url
                         */
                        event.doit = false;
                    } else {
                        if (event.location.startsWith("gene:")) {
                            updateBrowserWithGene(event.location.substring(5));
                            event.doit = false;
                        }
                    }
                }
            });
            browserTabItem.setControl(this.browser);
            browserFolder.setSelection(0);

            browserFolder.addCTabFolder2Listener(new CTabFolder2Adapter()
            {
                @Override
                public void maximize(CTabFolderEvent event)
                {
                    browserFolder.setMaximized(true);
                    EnrichedGOTermsComposite.this.verticalSashForm.setMaximizedControl(browserComposite);
                }

                @Override
                public void restore(CTabFolderEvent event)
                {
                    browserFolder.setMaximized(false);
                    EnrichedGOTermsComposite.this.verticalSashForm.setMaximizedControl(null);
                }

            });
        } catch (SWTError e) {
            browserComposite.dispose();
            this.browser = null;

            /* Create the fall back environment */
            Composite styledTextComposite = new Composite(this.verticalSashForm, 0);
            styledTextComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

            Label label = new Label(styledTextComposite, 0);
            label.setText("No browser available!");

            String error = e.getLocalizedMessage();
            if (error != null) {
                label.setToolTipText(NOBROWSER_TOOLTIP + "\n\nReason for failing: " + error);
            } else {
                label.setToolTipText(NOBROWSER_TOOLTIP);
            }

            this.styledText = new StyledText(styledTextComposite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
            this.styledText.setEditable(false);
            this.styledText
                .setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        }
    }

    @Override
    protected void newEmanatingTermSelected(Term term)
    {
        super.newEmanatingTermSelected(term);

        populateTable();
        updateSignificanceText();
    }

    /**
     * Returns the currently chosen significance level.
     *
     * @return the significance level (between 0 and 1)
     */
    public double getSignificanceLevel()
    {
        double level = this.significanceSpinner.getSelection() / (double) SIGNIFICANCE_RESOLUTION;
        return level;
    }

    /**
     * Helper function to create a new graph generation thread.
     *
     * @param finished
     * @return
     */
    private EnrichedGraphGenerationThread createGraphGenerationThread(IGraphGenerationFinished finished)
    {
        EnrichedGraphGenerationThread ggt =
            new EnrichedGraphGenerationThread(getDisplay(), GlobalPreferences.getDOTPath(), finished);
        ggt.go = this.go;
        ggt.emanatingTerm = getEmanatingTerm();
        ggt.significanceLevel = getSignificanceLevel();
        ggt.leafTerms.addAll(getCheckedTermsCollection());
        ggt.result = this.result;
        return ggt;
    }

    @Override
    public void setScaleToFit(boolean fit)
    {
        this.graphVisual.setScaleToFit(fit);
    }

    @Override
    public void zoomIn()
    {
        this.graphVisual.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        this.graphVisual.zoomOut();
    }

    @Override
    public void resetZoom()
    {
        this.graphVisual.zoomReset();
    }

    /**
     * Preview the graph.
     */
    @Override
    public void updateDisplayedGraph()
    {
        EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished()
        {
            @Override
            public void finished(boolean success, String message, File pngFile, File dotFile)
            {
                if (success) {
                    logger.info("Layouted graph successful (located in \"" + dotFile.toString() + "\").");

                    /* make the graph display visible */
                    EnrichedGOTermsComposite.this.termSashForm.setMaximizedControl(null);

                    try {
                        EnrichedGOTermsComposite.this.graphVisual.setLayoutedDotFile(dotFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.warning("Layouting graph failed.");

                    MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mbox.setMessage(
                        "Unable to execute the 'dot' tool!\nPlease check the preferences, and ensure that GraphViz (available from http://www.graphviz.org/) is installed properly\n\n"
                            + message);
                    mbox.setText("Ontologizer - Error");
                    mbox.open();
                }
            }
        });
        logger.info("Layouting graph.");
        ggt.start();
    }

    /**
     * Stores the result as graph.
     */
    @Override
    public void saveGraph(String path)
    {
        EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished()
        {
            @Override
            public void finished(boolean success, String message, File pngFile, File dotFile)
            {
                if (!success && !isDisposed()) {
                    MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
                    mbox.setText("Ontologizer - Error");
                    mbox.open();
                }
            }
        });

        ggt.setGfxOutFilename(path);
        ggt.start();
    }

    /**
     * Store the result as ascii table.
     *
     * @param path defines the path where the file should be written to.
     */
    @Override
    public void tableSave(String path)
    {
        File tableFile = new File(path);
        this.result.writeTable(tableFile);
    }

    /**
     * Store the result as a latex file.
     */
    @Override
    public void latexSave(String path)
    {
        EnrichedGOTermsResultLatexWriter.write(this.result, new File(path), getCheckedTermsCollection());
    }

    /**
     * Store the result as html site.
     *
     * @param path defines the path where the files should be written to.
     */
    @Override
    public void htmlSave(String path)
    {
        final File htmlFile = new File(path);
        EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished()
        {
            @Override
            public void finished(boolean success, String message, File pngFile, File dotFile)
            {
                if (!isDisposed()) {
                    if (success) {
                        logger.info("HTML preparation finished with success!");
                        Ontologizer.showWaitPointer();
                        EnrichedGOTermsResultHTMLWriter.write(EnrichedGOTermsComposite.this.result, htmlFile, dotFile);
                        Ontologizer.hideWaitPointer();
                    } else {
                        logger.warning("HTML preparation finished with failure!");
                        MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                        mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
                        mbox.setText("Ontologizer - Error");
                        mbox.open();
                    }
                } else {
                    logger.info("HTML preparation finished with success = " + success);
                }
            }
        });

        logger.info("Preparing to store html file \"" + path + "\".");
        ggt.start();
    }

    /**
     * Store the annotated results as ascii text.
     *
     * @param path defines the path where the file should be written to.
     */
    @Override
    public void tableAnnotatedSetSave(String path)
    {
        File tableFile = new File(path);
        this.result.getStudySet().writeSetWithAnnotations(this.result.getGO(), this.associationContainer, tableFile);
    }

    public EnrichedGOTermsResult getResult()
    {
        return this.result;
    }

    @Override
    public String getTitle()
    {
        String correctionName = getResult().getCorrectionName();
        String calculationName = getResult().getCalculationName();

        if (correctionName != null) {
            return getResult().getStudySet().getName() + " (" + calculationName + "/" + correctionName + ")";
        } else {
            return getResult().getStudySet().getName() + " (" + calculationName + ")";
        }
    }

}
