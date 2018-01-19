package ontologizer.gui.swt.support;

/*************************************************************
 $Id$

 This class is copyright 2006 by Sebastian Bauer and released
 under the terms of the CPL.
 See http://www.opensource.org/licenses/cpl.php for more
 information.
*************************************************************/

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TypedListener;

import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import att.grappa.Parser;

/**
 * A canvas displaying a graph.
 *
 * @author Sebastian Bauer <mail@sebastianbauer.info>
 */
public class GraphCanvas extends Canvas implements IGraphCanvas
{
    private Graph g;

    private GraphPaint gp;

    private boolean scaleToFit;

    private float scale;

    private float xoffset;

    private float yoffset;

    private MenuItem zoomInItem;

    private MenuItem zoomOutItem;

    private MenuItem zoomReset;

    private MenuItem scaleToFitItem;

    private ScrollBar horizontalScrollBar;

    private ScrollBar verticalScrollBar;

    private GraphCanvas thisCanvas;

    private boolean mouseDown;

    private int mouseCenterX;

    private int mouseCenterY;

    private float oldXOffset;

    private float oldYOffset;

    private float oldScale;

    private boolean zoomMove;

    /**
     * Constructs a new graph canvas.
     *
     * @param parent
     * @param style
     */
    public GraphCanvas(Composite parent, int style)
    {
        super(parent, style | SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED | SWT.HORIZONTAL | SWT.VERTICAL);

        this.thisCanvas = this;

        this.scaleToFit = true;
        this.horizontalScrollBar = getHorizontalBar();
        this.verticalScrollBar = getVerticalBar();

        this.g = new Graph("empty");
        prepareGraph();

        /* Hide the scrollbars */
        this.horizontalScrollBar.setVisible(false);
        this.verticalScrollBar.setVisible(false);

        /* Add our control listener */
        addControlListener(new ControlAdapter()
        {
            @Override
            public void controlResized(ControlEvent e)
            {
                if (GraphCanvas.this.scaleToFit) {
                    updateTransformation();
                }
                updateScrollers();
            }
        });

        /* Mouse tracker */
        addMouseTrackListener(new MouseTrackAdapter()
        {
            @Override
            public void mouseHover(MouseEvent e)
            {
                /* Build the inverted transformation */
                Transform transform = buildTransform();
                transform.invert();

                float[] points = new float[2];
                points[0] = e.x;
                points[1] = e.y;
                transform.transform(points);

                Edge edge = GraphCanvas.this.gp.findEdgeByCoord(points[0], points[1]);
                if (edge != null) {
                    String tip = (String) edge.getAttributeValue("tooltip");
                    setToolTipText(tip);
                } else {
                    setToolTipText(null);
                }
            }
        });

        /* Add our mouse listener */
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDown(MouseEvent e)
            {
                /* Build the inverted transformation */
                Transform transform = buildTransform();
                transform.invert();

                float[] points = new float[2];
                points[0] = e.x;
                points[1] = e.y;
                transform.transform(points);

                Node n = GraphCanvas.this.gp.findNodeByCoord(points[0], points[1]);
                if (n != null) {
                    GraphCanvas.this.gp.setSelectedNode(n);
                    redraw();

                    /* emit event */
                    Event ev = new Event();
                    ev.widget = GraphCanvas.this.thisCanvas;
                    ev.type = SWT.Selection;
                    ev.text = getNameOfCurrentSelectedNode();
                    notifyListeners(SWT.Selection, ev);
                } else {
                    if (e.button == 1 || e.button == 2) {
                        GraphCanvas.this.mouseDown = true;
                        GraphCanvas.this.mouseCenterX = e.x;
                        GraphCanvas.this.mouseCenterY = e.y;
                        GraphCanvas.this.oldXOffset = GraphCanvas.this.xoffset;
                        GraphCanvas.this.oldYOffset = GraphCanvas.this.yoffset;
                        GraphCanvas.this.oldScale = GraphCanvas.this.scale;

                        GraphCanvas.this.zoomMove = e.button == 2;
                    }
                }
            }

            @Override
            public void mouseUp(MouseEvent e)
            {
                GraphCanvas.this.mouseDown = false;
            }
        });

        /* Add mouse move listener */
        addMouseMoveListener(new MouseMoveListener()
        {
            @Override
            public void mouseMove(MouseEvent e)
            {
                if (GraphCanvas.this.mouseDown) {
                    if (GraphCanvas.this.zoomMove) {
                        setScale(GraphCanvas.this.oldScale * ((float) GraphCanvas.this.mouseCenterY / (float) e.y));
                    } else {
                        GraphCanvas.this.xoffset = GraphCanvas.this.oldXOffset + (GraphCanvas.this.mouseCenterX - e.x);
                        GraphCanvas.this.yoffset = GraphCanvas.this.oldYOffset + (GraphCanvas.this.mouseCenterY - e.y);
                        updateScrollers();
                        redraw();
                    }
                }
            }
        });

        /* Add our paint listener */
        addPaintListener(new PaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                GC gc = e.gc;

                gc.setAntialias(SWT.ON);
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
                gc.fillRectangle(getClientArea());

                Transform transform = buildTransform();
                gc.setTransform(transform);
                GraphCanvas.this.gp.drawGraph(gc);
                gc.setTransform(null);
                transform.dispose();
            }
        });

        /* Add the dispose listener */
        addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                cleanupGraph();
            }
        });

        Menu menu = new Menu(this);
        this.zoomInItem = new MenuItem(menu, 0);
        this.zoomInItem.setText("Zoom In");

        this.zoomOutItem = new MenuItem(menu, 0);
        this.zoomOutItem.setText("Zoom Out");

        this.zoomReset = new MenuItem(menu, 0);
        this.zoomReset.setText("Reset Zoom");

        this.scaleToFitItem = new MenuItem(menu, SWT.CHECK | SWT.TOGGLE);
        this.scaleToFitItem.setText("Scale To Fit");
        this.scaleToFitItem.setSelection(this.scaleToFit);

        setMenu(menu);

        /* Add selection listener for the menu */
        SelectionListener selListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (e.widget.equals(GraphCanvas.this.zoomInItem)) {
                    zoomIn();
                } else if (e.widget.equals(GraphCanvas.this.zoomOutItem)) {
                    zoomOut();
                } else if (e.widget.equals(GraphCanvas.this.scaleToFitItem)) {
                    setScaleToFit(GraphCanvas.this.scaleToFitItem.getSelection());
                } else if (e.widget.equals(GraphCanvas.this.zoomReset)) {
                    zoomReset();
                }
            }
        };
        this.zoomInItem.addSelectionListener(selListener);
        this.zoomOutItem.addSelectionListener(selListener);
        this.zoomReset.addSelectionListener(selListener);
        this.scaleToFitItem.addSelectionListener(selListener);

        /* Add selection listener for the bar */
        SelectionListener barListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (e.widget.equals(GraphCanvas.this.horizontalScrollBar)) {
                    GraphCanvas.this.xoffset = getHorizontalBar().getSelection();
                } else if (e.widget.equals(GraphCanvas.this.verticalScrollBar)) {
                    GraphCanvas.this.yoffset = getVerticalBar().getSelection();
                }

                redraw();
            }
        };

        this.horizontalScrollBar.addSelectionListener(barListener);
        this.verticalScrollBar.addSelectionListener(barListener);
    }

    /**
     * Cleanup the graph.
     */
    private void cleanupGraph()
    {
        if (this.gp != null) {
            this.gp.dispose();
        }
        this.gp = null;
    }

    /**
     * Prepare for the current graph.
     */
    private void prepareGraph()
    {
        cleanupGraph();
        this.gp = new GraphPaint(getDisplay(), this.g);
    }

    /**
     * Short cut for getting the width of the displayed graph.
     *
     * @return
     */
    private float getGraphWidth()
    {
        return (float) (this.g.getBoundingBox().getMaxX() - this.g.getBoundingBox().getMinX() + 1);
    }

    /**
     * Short cut for getting the height of the displayed graph.
     *
     * @return
     */
    private float getGraphHeight()
    {
        return (float) (this.g.getBoundingBox().getMaxY() - this.g.getBoundingBox().getMinY() + 1);
    }

    /**
     * Updates transformation for scale to fit.
     */
    private void updateTransformation()
    {
        /* Real graph Dimensions */
        float graphWidth = getGraphWidth();
        float graphHeight = getGraphHeight();

        /* Zoom factor */
        this.scale = Math.min((getClientArea().width) / graphWidth, (getClientArea().height) / graphHeight)
            / 1.005f;
        this.xoffset = 0;
        this.yoffset = 0;
    }

    /**
     * Updates the scroller (i.e. if they are visible and their values)
     */
    private void updateScrollers()
    {
        getHorizontalBar().setValues((int) this.xoffset, 0, (int) (getGraphWidth() * this.scale), getClientArea().width,
            1,
            getClientArea().width - 1);
        getVerticalBar().setValues((int) this.yoffset, 0, (int) (getGraphHeight() * this.scale), getClientArea().height,
            1,
            getClientArea().height - 1);

        if (getGraphWidth() * this.scale > getClientArea().width) {
            getHorizontalBar().setVisible(true);
        } else {
            getHorizontalBar().setVisible(false);
        }

        if (getGraphHeight() * this.scale > getClientArea().height) {
            getVerticalBar().setVisible(true);
        } else {
            getVerticalBar().setVisible(false);
        }
    }

    /**
     * Returns the name of the currently selected Node.
     *
     * @return
     */
    public String getNameOfCurrentSelectedNode()
    {
        if (this.gp.getSelectedNode() != null) {
            return this.gp.getSelectedNode().getName();
        }
        return null;
    }

    /**
     * Selectes the node with the given name. Other nodes are deselected.
     *
     * @param name
     */
    public void selectNode(String name)
    {
        Node node = this.g.findNodeByName(name);
        if (node != this.gp.getSelectedNode()) {

            this.gp.setSelectedNode(node);
            redraw();
        }
    }

    @Override
    public void setLayoutedDotFile(File file) throws Exception
    {
        Parser parser = new Parser(new FileInputStream(file), System.err);
        parser.parse();

        this.gp.setSelectedNode(null);
        cleanupGraph();

        this.g = parser.getGraph();
        this.g.setEditable(false);

        prepareGraph();

        updateTransformation();
        redraw();
    }

    class ZoomRunnable implements Runnable
    {
        private float oldScale;

        private float newScale;

        private long startTime;

        private long duration;

        private long endTime;

        private float centerX;

        private float centerY;

        public void initialize(float newScale)
        {
            this.startTime = System.currentTimeMillis();
            this.duration = 250;
            this.endTime = this.startTime + this.duration;
            this.oldScale = GraphCanvas.this.scale;

            this.centerX = (Math.min(getClientArea().width, getGraphWidth() * GraphCanvas.this.scale) / 2f
                + GraphCanvas.this.xoffset) / GraphCanvas.this.scale;
            this.centerY = (Math.min(getClientArea().height, getGraphHeight() * GraphCanvas.this.scale) / 2f
                + GraphCanvas.this.yoffset) / GraphCanvas.this.scale;
            this.newScale = newScale;
        }

        @Override
        public void run()
        {
            if (isDisposed() || GraphCanvas.this.scaleToFit) {
                return;
            }

            long newTime = System.currentTimeMillis();
            if (newTime < this.endTime) {
                getDisplay().timerExec(33, this);
            } else {
                newTime = this.endTime;
            }

            long diffTime = newTime - this.startTime;

            GraphCanvas.this.scale =
                this.oldScale - (this.oldScale - this.newScale) * ((float) diffTime / (float) this.duration);

            /*
             * Update the xoffset and yoffsets as follow: Determine the current center and adapt the offsets after
             * scaling to match the center.
             */
            GraphCanvas.this.xoffset = this.centerX * GraphCanvas.this.scale
                - Math.min(getClientArea().width, getGraphWidth() * GraphCanvas.this.scale) / 2f;
            GraphCanvas.this.yoffset = this.centerY * GraphCanvas.this.scale
                - Math.min(getClientArea().height, getGraphHeight() * GraphCanvas.this.scale) / 2f;

            updateScrollers();
            redraw();
            update();
        }
    };

    private ZoomRunnable zoomRunnable = new ZoomRunnable();

    /**
     * Sets a new magnification. Ensures that the center point stays the same.
     *
     * @param newScale
     */
    private void setScale(float newScale)
    {
        this.scaleToFit = false;
        this.scaleToFitItem.setSelection(false);

        this.zoomRunnable.initialize(newScale);
        getDisplay().timerExec(10, this.zoomRunnable);
    }

    /**
     * Zoom out.
     */
    @Override
    public void zoomOut()
    {
        setScale(this.scale / 1.5f);
    }

    /**
     * Zoom in.
     */
    @Override
    public void zoomIn()
    {
        setScale(this.scale * 1.5f);
    }

    /**
     * Reset the zoom.
     */
    @Override
    public void zoomReset()
    {
        setScale(1.0f);
    }

    /**
     * Set whether the magnification should be chosen such that, the complete graph is visible (also after resizing).
     *
     * @param selection
     */
    @Override
    public void setScaleToFit(boolean selection)
    {
        this.scaleToFit = selection;
        if (selection) {
            updateTransformation();
            updateScrollers();
            redraw();
        }
    }

    /**
     * Add a new selection listener. The text field is used for the node's name.
     *
     * @param sel
     */
    public void addSelectionListener(SelectionListener sel)
    {
        TypedListener listener = new TypedListener(sel);
        addListener(SWT.Selection, listener);
    }

    @Override
    public void addMouseListener(MouseListener mouse)
    {
        TypedListener typedListener = new TypedListener(mouse);
        addListener(SWT.MouseDown, typedListener);
        addListener(SWT.MouseUp, typedListener);
        addListener(SWT.MouseDoubleClick, typedListener);
    }

    /**
     * Builds an transformation from the graph coordinate system to the display coordinate system.
     *
     * @return
     */
    private Transform buildTransform()
    {
        return buildTransform(this.xoffset, this.yoffset, this.scale);
    }

    /**
     * Build an transformation from the graph coordinate system to the display coordinate system.
     *
     * @param xoffset
     * @param yoffset
     * @param scale
     * @return
     */
    private Transform buildTransform(float xoffset, float yoffset, float scale)
    {
        Transform transform = new Transform(getDisplay());

        float alignedXOffset = -xoffset;
        float alignedYOffset = getClientArea().height - 2 - yoffset;

        if (getGraphWidth() * scale < getClientArea().width) {
            alignedXOffset += (getClientArea().width - getGraphWidth() * scale) / 2;
        }

        if (getGraphHeight() * scale < getClientArea().height) {
            alignedYOffset -= (getClientArea().height - getGraphHeight() * scale) / 2;
        } else {
            alignedYOffset += getGraphHeight() * scale - getClientArea().height;
        }

        transform.translate(alignedXOffset, alignedYOffset);
        transform.scale(scale, scale);
        return transform;
    }
}
