package ontologizer.gui.swt.result;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import cern.colt.matrix.DoubleMatrix2D;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.svd.SVDGOTermProperties;
import ontologizer.calculation.svd.SVDResult;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.Chart;
import ontologizer.gui.swt.support.Chart.XYSeries;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.gui.swt.support.ScatterPlot;

/**
 * A class responsible for displaying SVD results.
 *
 * @author Sebastian Bauer
 */
public class SVDGOTermsComposite extends AbstractResultComposite
{
    private SVDResult result;

    /** Size of the svd (i.e. number of study sets being used) */
    private int size;

    /* Table stuff */
    private Table table;

    private TableColumn[] columns;

    private int sortColumn = -1;

    private int sortDirection = SWT.UP;

    /** Used to find the position of a table line given the term id */
    private HashMap<Integer, Integer> termID2ListLine;

    /** Used to find the proper term position given the table position */
    private HashMap<Integer, Integer> line2TermPos;

    /** Used to get the color of a term. The color is determined by the term's significance */
    private HashMap<TermID, Color> termID2Color;

    private static final int GOID = 0;

    private static final int NAME = 1;

    private static final int NAMESPACE = 2;

    private static final int LAST = 3;

    private Chart screeChart;

    private Spinner termSpinner;

    private Spinner percentageSpinner;

    private Combo eigenCombo;

    private ScatterPlot scatterPlot;

    /** Has the user changed set set of checked terms manually? */
    private boolean checkedTermsChanged;

    /**
     * Returns the number of terms which should be marked.
     *
     * @return
     */
    private int getNumberOfTermsToBeMarked()
    {
        return this.termSpinner.getSelection();
    }

    /**
     * Returns the number of the eigenterm against which the number of terms to be highlighted should be considered.
     *
     * @return
     */
    private int getRespectiveNumber()
    {
        return this.eigenCombo.getSelectionIndex();
    }

    /**
     * Comparator class for sorting go term properties by selected weights.
     *
     * @author Sebastian Bauer
     */
    private class WeightComparator implements Comparator<AbstractGOTermProperties>
    {
        private int direction;

        private int index;

        public WeightComparator(int direction, int index)
        {
            this.direction = direction;
            this.index = index;
        }

        @Override
        public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
        {
            SVDGOTermProperties so1 = (SVDGOTermProperties) o1;
            SVDGOTermProperties so2 = (SVDGOTermProperties) o2;
            int r;

            if (Math.abs(so1.weights[this.index]) < Math.abs(so2.weights[this.index])) {
                r = -1;
            } else if (Math.abs(so1.weights[this.index]) > Math.abs(so2.weights[this.index])) {
                r = 1;
            } else {
                r = 0;
            }
            r *= this.direction;
            return r;
        }
    }

    /**
     * The listener for the table columns.
     */
    private SelectionListener sortingListener = new SelectionAdapter()
    {
        @Override
        public void widgetSelected(SelectionEvent ev)
        {
            TableColumn col = (TableColumn) ev.widget;
            if (SVDGOTermsComposite.this.table.getSortColumn() == col) {
                if (SVDGOTermsComposite.this.table.getSortDirection() == SWT.UP) {
                    SVDGOTermsComposite.this.sortDirection = SWT.DOWN;
                } else {
                    SVDGOTermsComposite.this.sortDirection = SWT.UP;
                }
            } else {
                SVDGOTermsComposite.this.sortDirection = SWT.UP;
            }

            SVDGOTermsComposite.this.sortColumn = (Integer) col.getData("column");

            SVDGOTermsComposite.this.table.setSortColumn(col);
            SVDGOTermsComposite.this.table.setSortDirection(SVDGOTermsComposite.this.sortDirection);

            populateTable();
        }
    };

    /**
     * @param parent
     * @param style
     */
    public SVDGOTermsComposite(Composite parent, int style)
    {
        super(parent, style);

        setLayout(new GridLayout());

        /* We like to have a subterm filter */
        createSubtermFilter(this);

        SashForm verticalSash = new SashForm(this, SWT.VERTICAL);
        verticalSash
            .setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        /* Table */
        SelectionListener highlightChangingListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent se)
            {
                disposeSignificanceColors();
                prepareSignificanceColors();
                SVDGOTermsComposite.this.table.clearAll();
                updatePercentageSpinner();
            }
        };

        SashForm horizSash = new SashForm(verticalSash, SWT.HORIZONTAL);

        Composite tableComposite = new Composite(horizSash, 0);
        tableComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

        createTableComposite(tableComposite);
        this.table.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        Composite selectComposite = new Composite(tableComposite, 0);
        selectComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        selectComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(6));
        Label label = new Label(selectComposite, 0);
        label.setText("Terms to be highlighted");
        label.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
        this.termSpinner = new Spinner(selectComposite, SWT.BORDER);
        this.termSpinner.addSelectionListener(highlightChangingListener);
        label = new Label(selectComposite, 0);
        label.setText("Or percentage");
        this.percentageSpinner = new Spinner(selectComposite, SWT.BORDER);
        this.percentageSpinner.setMaximum(1000);
        this.percentageSpinner.setDigits(1);
        this.percentageSpinner.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateTermSpinner();
                disposeSignificanceColors();
                prepareSignificanceColors();
                SVDGOTermsComposite.this.table.clearAll();
            }
        });
        label = new Label(selectComposite, 0);
        label.setText("respective component");
        this.eigenCombo = new Combo(selectComposite, SWT.READ_ONLY);
        this.eigenCombo.addSelectionListener(highlightChangingListener);

        this.graphVisual = new GraphCanvas(horizSash, SWT.BORDER);
        this.graphVisual
            .setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        horizSash = new SashForm(verticalSash, SWT.HORIZONTAL);
        createScreeChart(horizSash);
        this.scatterPlot = new ScatterPlot(horizSash, SWT.BORDER);
        this.scatterPlot.setXAxisTitle("PCA 1");
        this.scatterPlot.setYAxisTitle("PCA 2");

        addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent arg0)
            {
                disposeSignificanceColors();
            }
        });
    }

    /**
     * Overwriteable method to return the string of data which was used as input source for the svd. Defaults to the
     * counts.
     *
     * @param prop
     * @param i
     * @return
     */
    protected String getOrginalDataString(SVDGOTermProperties prop, int i)
    {
        return Integer.toString(prop.counts[i]);
    }

    public void createTableComposite(Composite parent)
    {
        this.table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.VIRTUAL);
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);

        this.table.addListener(SWT.SetData, new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {
                TableItem item = (TableItem) e.item;
                Integer index = SVDGOTermsComposite.this.line2TermPos.get(e.index);
                if (index != null) {
                    SVDGOTermProperties prop = (SVDGOTermProperties) SVDGOTermsComposite.this.props[index];
                    item.setText(GOID, prop.goTerm.getIDAsString());
                    item.setText(NAME, prop.goTerm.getName());
                    item.setText(NAMESPACE, prop.goTerm.getNamespaceAsString());

                    for (int i = 0; i < SVDGOTermsComposite.this.size; i++) {
                        item.setText(LAST + i, getOrginalDataString(prop, i));
                    }

                    for (int i = 0; i < SVDGOTermsComposite.this.size; i++) {
                        item.setText(LAST + i + SVDGOTermsComposite.this.size, String.format("%.3g", prop.weights[i]));
                    }

                    if (isCheckedTerm(prop.goTerm.getID())) {
                        item.setChecked(true);
                    }

                    Color background = SVDGOTermsComposite.this.termID2Color.get(prop.goTerm.getID());
                    if (background != null) {
                        item.setBackground(NAMESPACE, background);
                    }
                }
            }
        });
    }

    public void createScreeChart(Composite parent)
    {
        this.screeChart = new Chart(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gd.heightHint = 50;
        gd.widthHint = 100;
        this.screeChart.setLayoutData(gd);
        this.screeChart.setXAxisTitle("Index of Eigenvalue");
    }

    public void setResult(SVDResult newResult)
    {
        super.setResult(newResult);

        this.result = newResult;

        this.size = this.result.getSigma().length;
        if (this.size <= 0) {
            return;
        }

        /* Graph stuff */
        setDotNodeAttributesProvider(new AbstractDotAttributesProvider()
        {
            @Override
            public String getDotNodeAttributes(TermID id)
            {
                StringBuilder attrString = new StringBuilder();

                attrString.append("label=\"");

                if (SVDGOTermsComposite.this.go.isRootTerm(id)) {
                    attrString.append("Gene Ontology");
                } else {
                    SVDGOTermProperties prop =
                        (SVDGOTermProperties) SVDGOTermsComposite.this.result.getGOTermProperties(id);
                    attrString.append(id.toString());
                    attrString.append("\\n");
                    attrString.append(SVDGOTermsComposite.this.go.getTerm(id).getName());
                }
                attrString.append("\"");

                Color col = SVDGOTermsComposite.this.termID2Color.get(id);
                if (col != null) {
                    attrString.append(",fillcolor=\"");
                    float[] hsb = col.getRGB().getHSB();
                    attrString.append(String.format(Locale.US, "%f,%f,%f", hsb[0] / 360.f, hsb[1], hsb[2]));
                    attrString.append("\"");
                }

                return attrString.toString();
            }
        });

        /* Set eigen stuff */
        String items[] = new String[this.size];
        for (int i = 0; i < this.size; i++) {
            items[i] = Integer.toString(i);
        }
        this.eigenCombo.setItems(items);
        this.eigenCombo.select(0);

        /* Table stuff */
        this.columns = new TableColumn[LAST + 2 * this.size];
        for (int i = 0; i < this.columns.length; i++) {
            this.columns[i] = new TableColumn(this.table, SWT.NONE);
            this.columns[i].addSelectionListener(this.sortingListener);
            this.columns[i].setData("column", new Integer(i));
        }
        this.columns[GOID].setText("GO ID");
        this.columns[NAME].setText("Name");
        this.columns[NAMESPACE].setText("NSP");
        this.columns[NAMESPACE].setAlignment(SWT.CENTER);
        this.columns[NAMESPACE].setToolTipText("Namespace or sub ontology");

        for (int i = LAST; i < LAST + this.result.getSigma().length; i++) {
            this.columns[i].setText("Cnt");
            this.columns[i].setAlignment(SWT.RIGHT);
        }

        for (int i = LAST + this.result.getSigma().length; i < this.columns.length; i++) {
            this.columns[i].setText("Weight");
            this.columns[i].setAlignment(SWT.RIGHT);
        }

        for (TableColumn column : this.columns) {
            column.pack();
        }

        /* Ensure useful columns sizes */
        if (this.columns[GOID].getWidth() < 70) {
            this.columns[GOID].setWidth(100);
        }
        if (this.columns[NAME].getWidth() < 250) {
            this.columns[NAME].setWidth(250);
        }

        prepareSignificanceColors();
        populateTable();
        prepareScreeChart();
        prepareCorrelationChart();

        this.termSpinner.setMaximum(this.props.length);
    }

    /**
     * Updates the percentage spinner according to the term spinner.
     */
    private void updatePercentageSpinner()
    {
        double totalWeight = 0.0;

        int column = getRespectiveNumber();
        if (column == -1) {
            column = 0;
        }

        /* Sort the properties by the given columns */
        AbstractGOTermProperties[] sortedProps = new AbstractGOTermProperties[this.props.length];
        for (int i = 0; i < this.props.length; i++) {
            sortedProps[i] = this.props[i];
            totalWeight += Math.abs(((SVDGOTermProperties) this.props[i]).weights[column]);
        }

        Arrays.sort(sortedProps, new WeightComparator(-1, column));

        int terms = getNumberOfTermsToBeMarked();
        double weight = 0.0;
        for (int i = 0; i < Math.min(this.props.length, terms); i++) {
            weight += Math.abs(((SVDGOTermProperties) this.props[i]).weights[column]);
        }

        this.percentageSpinner.setSelection((int) (weight / totalWeight * 1000));
    }

    /**
     * Updates the term spinner according to the percentage spinner.
     */
    private void updateTermSpinner()
    {
        double totalWeight = 0.0;

        int column = getRespectiveNumber();
        if (column == -1) {
            column = 0;
        }

        /* Sort the properties by the given columns */
        AbstractGOTermProperties[] sortedProps = new AbstractGOTermProperties[this.props.length];
        for (int i = 0; i < this.props.length; i++) {
            sortedProps[i] = this.props[i];
            totalWeight += Math.abs(((SVDGOTermProperties) this.props[i]).weights[column]);
        }

        Arrays.sort(sortedProps, new WeightComparator(-1, column));

        double weightRatio = (this.percentageSpinner.getSelection()) / 1000.0;
        double maxWeight = weightRatio * totalWeight;
        double weight = 0.0;

        for (int i = 0; i < this.props.length; i++) {
            weight += Math.abs(((SVDGOTermProperties) this.props[i]).weights[column]);
            if (weight > maxWeight) {
                this.termSpinner.setSelection(i);
                break;
            }
        }
    }

    private void prepareScreeChart()
    {
        XYSeries variancesSeries = new XYSeries();
        variancesSeries.setName("Variance");
        variancesSeries.setY(this.result.getVariances());

        XYSeries cumSeries = new XYSeries();
        cumSeries.setY(this.result.getCumSumOfVariances());
        cumSeries.setName("Cumsum");

        this.screeChart.removeAllXYSeries();
        this.screeChart.addXYSeries(variancesSeries);
        this.screeChart.addXYSeries(cumSeries);
    }

    private void prepareCorrelationChart()
    {
        ScatterPlot.XYSeries correlationSeries = new ScatterPlot.XYSeries();
        DoubleMatrix2D dm2d = this.result.getCorrelation(0, 1);

        double x[] = new double[dm2d.rows()];
        double y[] = new double[dm2d.rows()];

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;

        for (int i = 0; i < dm2d.rows(); i++) {
            double x1 = dm2d.get(i, 0);
            double y1 = dm2d.get(i, 1);

            if (x1 < minX) {
                minX = x1;
            }
            if (y1 < minY) {
                minY = y1;
            }
        }

        for (int i = 0; i < dm2d.rows(); i++) {
            x[i] = dm2d.get(i, 0) - minX;
            y[i] = dm2d.get(i, 1) - minY;

            System.out.println(y[i]);
        }

        correlationSeries.setX(x);
        correlationSeries.setY(y);
        correlationSeries.setLabels(this.result.getCorrelationLabels());

        this.scatterPlot.removeAllXYSeries();
        this.scatterPlot.addXYSeries(correlationSeries);
    }

    /**
     * Populate the table. Respects sorting settings.
     */
    private void populateTable()
    {
        int entryNumber;
        final int direction;

        if (this.sortDirection == SWT.UP) {
            direction = 1;
        } else {
            direction = -1;
        }

        switch (this.sortColumn) {
            case GOID:
                Arrays.sort(this.props, new GOIDComparator(direction));
                break;
            case NAME:
                Arrays.sort(this.props, new GONameComparator(direction));
                break;
        }

        if (this.sortColumn >= LAST && this.sortColumn < LAST + this.size) {
            Arrays.sort(this.props, new Comparator<AbstractGOTermProperties>()
            {
                @Override
                public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
                {
                    SVDGOTermProperties so1 = (SVDGOTermProperties) o1;
                    SVDGOTermProperties so2 = (SVDGOTermProperties) o2;
                    int r;

                    if (so1.counts[SVDGOTermsComposite.this.sortColumn
                        - LAST] < so2.counts[SVDGOTermsComposite.this.sortColumn - LAST]) {
                        r = -1;
                    } else if (so1.counts[SVDGOTermsComposite.this.sortColumn
                        - LAST] > so2.counts[SVDGOTermsComposite.this.sortColumn - LAST]) {
                        r = 1;
                    } else {
                        r = 0;
                    }
                    r *= direction;
                    return r;
                }
            });
        }

        if (this.sortColumn >= LAST + this.size && this.sortColumn < LAST + 2 * this.size) {
            Arrays.sort(this.props, new WeightComparator(direction, this.sortColumn - LAST - this.size));
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

        int count = getNumberOfTermsToBeMarked();
        int column = getRespectiveNumber();
        if (column == -1) {
            column = 0;
        }

        this.termID2Color = new HashMap<>();

        /* Sort the properties by the given columns */
        AbstractGOTermProperties[] sortedProps = new AbstractGOTermProperties[this.props.length];
        for (int i = 0; i < this.props.length; i++) {
            sortedProps[i] = this.props[i];
        }

        Arrays.sort(sortedProps, new WeightComparator(-1, column));

        if (!this.checkedTermsChanged) {
            initializeCheckedTerms();
        }

        for (int rank = 0; rank < count; rank++) {
            /* See class StudySetResult */
            float hue, saturation, brightness;

            /*
             * Use the rank to determine the saturation We want that more significant nodes have more saturation, but we
             * avoid having significant nodes with too less saturation (at least 0.2)
             */
            saturation = 1.0f - (((float) rank + 1) / count) * 0.8f;

            /* Always full brightness */
            brightness = 1.0f;

            /* Hue depends on namespace */
            // switch (sortedProps[rank].goTerm.getNamespace())
            // {
            // case BIOLOGICAL_PROCESS: hue = 120.f;break;
            // case MOLECULAR_FUNCTION: hue = 60.f;break;
            // case CELLULAR_COMPONENT: hue = 300.f;break;
            // default: hue = 0.f; saturation = 0.f;
            // }
            SVDGOTermProperties svdProp = (SVDGOTermProperties) sortedProps[rank];

            if (svdProp.weights[column] > 0) {
                hue = 120f;
            } else {
                hue = 0f;
            }

            this.termID2Color.put(sortedProps[rank].goTerm.getID(),
                new Color(getDisplay(), new RGB(hue, saturation, brightness)));
            if (!this.checkedTermsChanged) {
                addToCheckedTerms(sortedProps[rank].goTerm.getID());
            }
        }
    }

    @Override
    public String getTitle()
    {
        return "PCA";
    }
}
