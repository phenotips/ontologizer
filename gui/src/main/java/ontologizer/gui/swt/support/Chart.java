/*
 * Created on 09.06.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.support;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A widget to draw a basic xy chart.
 *
 * @author Sebastian Bauer
 */
public class Chart extends Canvas
{
    /* Context menu support */
    private Menu contextMenu;

    private ArrayList<MenuItem> contextMenuItemList;

    /**
     * Class for the chart model.
     *
     * @author Sebastian Bauer
     */
    public static class XYSeries
    {
        private double[] x;

        private double[] y;

        private double maxX;

        private double maxY;

        private String name;

        private boolean showMe = true;

        public void setX(double[] x)
        {
            this.x = x;
            this.maxX = Double.MIN_VALUE;
            for (double element : x) {
                if (element > this.maxX) {
                    this.maxX = element;
                }
            }
        }

        public void setX(float[] x)
        {
            this.x = new double[x.length];
            this.maxX = Double.MIN_VALUE;
            for (int i = 0; i < x.length; i++) {
                this.x[i] = x[i];
                if (x[i] > this.maxX) {
                    this.maxX = x[i];
                }
            }
        }

        public void setY(double[] y)
        {
            this.y = y;
            this.maxY = Double.MIN_VALUE;
            for (double element : y) {
                if (element > this.maxY) {
                    this.maxY = element;
                }
            }

            if (this.x == null) {
                this.x = new double[y.length];
                for (int i = 0; i < y.length; i++) {
                    this.x[i] = i;
                }
                this.maxX = y.length;
            }
        }

        public void setY(float[] y)
        {
            this.y = new double[y.length];

            this.maxY = Double.MIN_VALUE;
            for (int i = 0; i < y.length; i++) {
                this.y[i] = y[i];
                if (y[i] > this.maxY) {
                    this.maxY = y[i];
                }
            }

            if (this.x == null) {
                this.x = new double[y.length];
                for (int i = 0; i < y.length; i++) {
                    this.x[i] = i;
                }
                this.maxX = y.length;
            }
        }

        public void setY(int[] y)
        {
            this.y = new double[y.length];

            this.maxY = Double.MIN_VALUE;
            for (int i = 0; i < y.length; i++) {
                this.y[i] = y[i];
                if (y[i] > this.maxY) {
                    this.maxY = y[i];
                }
            }

            if (this.x == null) {
                this.x = new double[y.length];
                for (int i = 0; i < y.length; i++) {
                    this.x[i] = i;
                }
                this.maxX = y.length;
            }
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public void store(FileOutputStream fs)
        {
            PrintStream ps = new PrintStream(fs);
            for (int i = 0; i < this.x.length; i++) {
                ps.print(this.x[i] + "\t" + this.y[i] + "\n");
            }
            ps.flush();
            ps.close();
        }

        public boolean hasShowMeFlag()
        {
            return this.showMe;
        }

        public void setShowMeFlag(boolean disp)
        {
            this.showMe = disp;
        }
    };

    private LinkedList<XYSeries> seriesList = new LinkedList<>();

    private double maxX = Double.MIN_VALUE;

    private double maxY = Double.MIN_VALUE;

    private String xAxisTitle;

    private String yAxisTitle;

    public Chart(Composite parent, int style)
    {
        super(parent, style | SWT.NO_BACKGROUND);

        /* context menu */
        this.contextMenuItemList = new ArrayList<>();
        this.contextMenu = new Menu(this);
        setMenu(this.contextMenu);
        this.contextMenu.addMenuListener(new MenuListener()
        {
            private void disposeItems()
            {
                for (MenuItem item : Chart.this.contextMenuItemList) {
                    item.dispose();
                }
                Chart.this.contextMenuItemList.clear();
            }

            @Override
            public void menuHidden(MenuEvent e)
            {
            }

            @Override
            public void menuShown(MenuEvent e)
            {
                disposeItems();
                for (XYSeries xySeries : Chart.this.seriesList) {
                    MenuItem mi = new MenuItem(Chart.this.contextMenu, SWT.CHECK | SWT.TOGGLE);
                    mi.setText("Show " + xySeries.getName());
                    mi.setSelection(xySeries.hasShowMeFlag());
                    mi.setData("xySeries", xySeries);
                    mi.addSelectionListener(new SelectionAdapter()
                    {
                        @Override
                        public void widgetSelected(SelectionEvent e)
                        {
                            XYSeries xySeries = (XYSeries) e.widget.getData("xySeries");
                            xySeries.setShowMeFlag(((MenuItem) e.widget).getSelection());
                            calculateDomain();
                            redraw();
                        }
                    });
                    Chart.this.contextMenuItemList.add(mi);
                }
            }
        });

        /* The paint listener */
        addPaintListener(new PaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                Rectangle clientArea = getClientArea();
                e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
                e.gc.fillRectangle(clientArea);

                int axesLeft = clientArea.x + 2;
                int axesTop = clientArea.y + 2;
                int axesRight = clientArea.x + clientArea.width - 4;
                int axesBottom = clientArea.y + clientArea.height - 4;

                Point xAxisTitleDimension;
                if (Chart.this.xAxisTitle != null) {
                    xAxisTitleDimension = e.gc.textExtent(Chart.this.xAxisTitle);
                } else {
                    xAxisTitleDimension = new Point(0, 0);
                }

                Point yAxisTitleDimension;
                if (Chart.this.yAxisTitle != null) {
                    yAxisTitleDimension = e.gc.textExtent(Chart.this.yAxisTitle);
                } else {
                    yAxisTitleDimension = new Point(0, 0);
                }

                /* Draw title of y-axis */
                if (Chart.this.yAxisTitle != null) {
                    Transform yAxisTransform = new Transform(getDisplay());
                    yAxisTransform.rotate(90);
                    yAxisTransform.translate((axesBottom - axesTop + 1 - yAxisTitleDimension.x) / 2,
                        -yAxisTitleDimension.y);
                    e.gc.setTransform(yAxisTransform);
                    e.gc.drawText(Chart.this.yAxisTitle, 0, 0);
                    e.gc.setTransform(null);
                    yAxisTransform.dispose();
                }

                /* Draw title of x-axis */
                if (Chart.this.xAxisTitle != null) {
                    e.gc.drawText(Chart.this.xAxisTitle, (axesRight - axesLeft + 1 - xAxisTitleDimension.x) / 2,
                        axesBottom - xAxisTitleDimension.y + 1);
                }

                /* Adjust axes */
                axesLeft += yAxisTitleDimension.y;
                axesBottom -= xAxisTitleDimension.y;

                Iterator<XYSeries> iter = Chart.this.seriesList.iterator();

                Color[] colors = new Color[5];
                colors[0] = getDisplay().getSystemColor(SWT.COLOR_BLACK);
                colors[1] = getDisplay().getSystemColor(SWT.COLOR_BLUE);
                colors[2] = getDisplay().getSystemColor(SWT.COLOR_GREEN);
                colors[3] = getDisplay().getSystemColor(SWT.COLOR_RED);
                colors[4] = getDisplay().getSystemColor(SWT.COLOR_GRAY);

                int currentColor = 0;

                boolean drawYTicks = true;

                int areaHeight = axesBottom - axesTop + 1;

                /* Draw ticks of y-axis */
                if (drawYTicks && Chart.this.maxY != Double.MIN_VALUE) {
                    int fontHeight = e.gc.getFontMetrics().getHeight();

                    /* Number of ticks */
                    int nTicks = (axesBottom - axesTop) / (fontHeight + 12);

                    /* Spaces between the ticks insise the coordinate domain */
                    double space = Chart.this.maxY / nTicks;
                    double roundedSpace = Math.pow(10, Math.ceil(Math.log10(space)));
                    int newNTicks = (int) (Chart.this.maxY / roundedSpace);

                    if (newNTicks < 2 && nTicks > 5) {
                        roundedSpace /= 2;
                        newNTicks = (int) (Chart.this.maxY / roundedSpace);
                    }

                    /* Space of a tick inside pixel domain */
                    double areaSpace = areaHeight * roundedSpace / Chart.this.maxY;

                    int labelWidth = 0;

                    final String fmtString = "%.2g";

                    for (int i = 1; i <= newNTicks; i++) {
                        int nlw = e.gc.textExtent(String.format(fmtString, roundedSpace * i)).x;
                        if (nlw > labelWidth) {
                            labelWidth = nlw;
                        }
                    }

                    axesLeft += labelWidth + 2;

                    for (int i = 1; i <= newNTicks; i++) {
                        e.gc.drawLine(axesLeft - 1, (int) (axesBottom - i * areaSpace), axesLeft + 1,
                            (int) (axesBottom - i * areaSpace));

                        int w = e.gc.textExtent(String.format(fmtString, roundedSpace * i)).x;
                        e.gc.drawText(String.format(fmtString, roundedSpace * i), axesLeft - w - 2,
                            (int) (axesBottom - i * areaSpace) - fontHeight / 2);
                    }
                }

                int areaLeft = axesLeft;
                int areaTop = axesTop;
                int areaWidth = axesRight - axesLeft + 1;

                /* Draw axes */
                e.gc.drawLine(axesLeft, axesTop, axesLeft, axesBottom);
                e.gc.drawLine(axesLeft, axesBottom, axesRight, axesBottom);

                while (iter.hasNext()) {
                    XYSeries series = iter.next();

                    if (!series.hasShowMeFlag()) {
                        if (++currentColor == colors.length) {
                            currentColor = 0;
                        }
                        continue;
                    }

                    int lastX = (int) (series.x[0] * areaWidth / Chart.this.maxX);
                    int lastY = (int) (series.y[0] * areaHeight / Chart.this.maxY);

                    int y_accu = lastY;
                    int y_num = 1;

                    e.gc.setForeground(colors[currentColor++]);
                    if (currentColor == colors.length) {
                        currentColor = 0;
                    }

                    for (int i = 1; i < series.x.length; i++) {
                        int x = (int) (series.x[i] * areaWidth / Chart.this.maxX);
                        int y = (int) (series.y[i] * areaHeight / Chart.this.maxY);

                        if (x != lastX) {
                            e.gc.drawLine(
                                areaLeft + lastX, areaTop + areaHeight - lastY - 1,
                                areaLeft + x, areaTop + areaHeight - y_accu / y_num - 1);

                            lastY = y_accu / y_num;

                            y_accu = 0;
                            y_num = 0;
                        }

                        y_accu += y;
                        y_num++;

                        lastX = x;
                    }
                }

                /* Determine the dimension of the legend */
                Point legendDims = new Point(0, 0);
                iter = Chart.this.seriesList.iterator();
                while (iter.hasNext()) {
                    XYSeries series = iter.next();
                    String name = series.getName();
                    if (name != null && name.length() > 0) {
                        Point p = e.gc.textExtent(name);
                        if (p.x > legendDims.x) {
                            legendDims.x = p.x;
                        }

                        legendDims.y += p.y;
                    }
                }

                if (legendDims.x > 0) {
                    legendDims.x += 20;
                    legendDims.y += (Chart.this.seriesList.size() - 1) * 2;

                    int legendX = axesRight - legendDims.x - 2;
                    int legendY = axesBottom - legendDims.y - 2;

                    currentColor = 0;
                    iter = Chart.this.seriesList.iterator();
                    while (iter.hasNext()) {
                        XYSeries series = iter.next();
                        String name = series.getName();

                        if (name != null) {
                            Point dim = e.gc.textExtent(name);
                            e.gc.setForeground(colors[currentColor]);
                            e.gc.drawLine(legendX + 2, legendY + dim.y / 2, legendX + 14, legendY + dim.y / 2);
                            e.gc.setForeground(colors[0]);
                            e.gc.drawText(name, legendX + 18, legendY);
                            legendY += dim.y + 2;
                        }
                        if (++currentColor == colors.length) {
                            currentColor = 0;
                        }
                    }
                }
            }
        });
    }

    private void calculateDomain()
    {
        this.maxX = Double.MIN_VALUE;
        this.maxY = Double.MIN_VALUE;

        for (XYSeries series : this.seriesList) {
            if (series.hasShowMeFlag()) {
                if (series.maxX > this.maxX) {
                    this.maxX = series.maxX;
                }
                if (series.maxY > this.maxY) {
                    this.maxY = series.maxY;
                }
            }
        }
    }

    public void removeAllXYSeries()
    {
        this.maxX = Double.MIN_VALUE;
        this.maxY = Double.MIN_VALUE;

        this.seriesList.clear();
        redraw();
    }

    /**
     * Add a new series to the chart.
     *
     * @param series
     */
    public void addXYSeries(XYSeries series)
    {
        this.seriesList.add(series);
        if (series.hasShowMeFlag()) {
            if (series.maxX > this.maxX) {
                this.maxX = series.maxX;
            }
            if (series.maxY > this.maxY) {
                this.maxY = series.maxY;
            }
        }
        redraw();
    }

    /**
     * Set the title for the x-axis.
     *
     * @param title
     */
    public void setXAxisTitle(String title)
    {
        this.xAxisTitle = title;
    }

    /**
     * Set the title for the y-axis.
     *
     * @param title
     */
    public void setYAxisTitle(String title)
    {
        this.yAxisTitle = title;
    }
}
