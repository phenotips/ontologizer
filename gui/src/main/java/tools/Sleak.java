/*******************************************************************************
* Copyright (c) 2004 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Instructions on how to use the Sleak tool with a standlaone SWT example: Modify the main method below to launch your
 * application. Run Sleak.
 */
public class Sleak
{
    Display display;

    public Shell shell;

    List list;

    Canvas canvas;

    Button start, stop, check;

    Text text;

    Label label;

    Object[] oldObjects = new Object[0];

    Error[] oldErrors = new Error[0];

    Object[] objects = new Object[0];

    Error[] errors = new Error[0];

    /*
     * public static void main (String [] args) { DeviceData data = new DeviceData(); data.tracking = true; Display
     * display = new Display (data); Sleak sleak = new Sleak (); sleak.open (); // Launch your application here while
     * (!sleak.shell.isDisposed ()) { if (!display.readAndDispatch ()) display.sleep (); } display.dispose (); }
     */

    public void open()
    {
        this.display = Display.getCurrent();
        this.shell = new Shell(this.display);
        this.shell.setText("S-Leak");
        this.list = new List(this.shell, SWT.BORDER | SWT.V_SCROLL);
        this.list.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                refreshObject();
            }
        });
        this.text = new Text(this.shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        this.canvas = new Canvas(this.shell, SWT.BORDER);
        this.canvas.addListener(SWT.Paint, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                paintCanvas(event);
            }
        });
        this.check = new Button(this.shell, SWT.CHECK);
        this.check.setText("Stack");
        this.check.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {
                toggleStackTrace();
            }
        });
        this.start = new Button(this.shell, SWT.PUSH);
        this.start.setText("Snap");
        this.start.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                refreshAll();
            }
        });
        this.stop = new Button(this.shell, SWT.PUSH);
        this.stop.setText("Diff");
        this.stop.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                refreshDifference();
            }
        });
        this.label = new Label(this.shell, SWT.BORDER);
        this.label.setText("0 object(s)");
        this.shell.addListener(SWT.Resize, new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {
                layout();
            }
        });
        this.check.setSelection(false);
        this.text.setVisible(false);
        Point size = this.shell.getSize();
        this.shell.setSize(size.x / 2, size.y / 2);
        this.shell.open();
    }

    void refreshLabel()
    {
        int colors = 0, cursors = 0, fonts = 0, gcs = 0, images = 0, regions = 0;
        for (Object object : this.objects) {
            if (object instanceof Color) {
                colors++;
            }
            if (object instanceof Cursor) {
                cursors++;
            }
            if (object instanceof Font) {
                fonts++;
            }
            if (object instanceof GC) {
                gcs++;
            }
            if (object instanceof Image) {
                images++;
            }
            if (object instanceof Region) {
                regions++;
            }
        }
        String string = "";
        if (colors != 0) {
            string += colors + " Color(s)\n";
        }
        if (cursors != 0) {
            string += cursors + " Cursor(s)\n";
        }
        if (fonts != 0) {
            string += fonts + " Font(s)\n";
        }
        if (gcs != 0) {
            string += gcs + " GC(s)\n";
        }
        if (images != 0) {
            string += images + " Image(s)\n";
        }
        if (regions != 0) {
            string += regions + " Region(s)\n";
        }
        if (string.length() != 0) {
            string = string.substring(0, string.length() - 1);
        }
        this.label.setText(string);
    }

    void refreshDifference()
    {
        DeviceData info = this.display.getDeviceData();
        if (!info.tracking) {
            MessageBox dialog = new MessageBox(this.shell, SWT.ICON_WARNING | SWT.OK);
            dialog.setText(this.shell.getText());
            dialog.setMessage("Warning: Device is not tracking resource allocation");
            dialog.open();
        }
        Object[] newObjects = info.objects;
        Error[] newErrors = info.errors;
        Object[] diffObjects = new Object[newObjects.length];
        Error[] diffErrors = new Error[newErrors.length];
        int count = 0;
        for (int i = 0; i < newObjects.length; i++) {
            int index = 0;
            while (index < this.oldObjects.length) {
                if (newObjects[i] == this.oldObjects[index]) {
                    break;
                }
                index++;
            }
            if (index == this.oldObjects.length) {
                diffObjects[count] = newObjects[i];
                diffErrors[count] = newErrors[i];
                count++;
            }
        }
        this.objects = new Object[count];
        this.errors = new Error[count];
        System.arraycopy(diffObjects, 0, this.objects, 0, count);
        System.arraycopy(diffErrors, 0, this.errors, 0, count);
        this.list.removeAll();
        this.text.setText("");
        this.canvas.redraw();
        for (Object object : this.objects) {
            this.list.add(objectName(object));
        }
        refreshLabel();
        layout();
    }

    String objectName(Object object)
    {
        String string = object.toString();
        int index = string.lastIndexOf('.');
        if (index == -1) {
            return string;
        }
        return string.substring(index + 1, string.length());
    }

    void toggleStackTrace()
    {
        refreshObject();
        layout();
    }

    void paintCanvas(Event event)
    {
        this.canvas.setCursor(null);
        int index = this.list.getSelectionIndex();
        if (index == -1) {
            return;
        }
        GC gc = event.gc;
        Object object = this.objects[index];
        if (object instanceof Color) {
            if (((Color) object).isDisposed()) {
                return;
            }
            gc.setBackground((Color) object);
            gc.fillRectangle(this.canvas.getClientArea());
            return;
        }
        if (object instanceof Cursor) {
            if (((Cursor) object).isDisposed()) {
                return;
            }
            this.canvas.setCursor((Cursor) object);
            return;
        }
        if (object instanceof Font) {
            if (((Font) object).isDisposed()) {
                return;
            }
            gc.setFont((Font) object);
            FontData[] array = gc.getFont().getFontData();
            String string = "";
            String lf = this.text.getLineDelimiter();
            for (FontData data : array) {
                String style = "NORMAL";
                int bits = data.getStyle();
                if (bits != 0) {
                    if ((bits & SWT.BOLD) != 0) {
                        style = "BOLD ";
                    }
                    if ((bits & SWT.ITALIC) != 0) {
                        style += "ITALIC";
                    }
                }
                string += data.getName() + " " + data.getHeight() + " " + style + lf;
            }
            gc.drawString(string, 0, 0);
            return;
        }
        // NOTHING TO DRAW FOR GC
        // if (object instanceof GC) {
        // return;
        // }
        if (object instanceof Image) {
            if (((Image) object).isDisposed()) {
                return;
            }
            gc.drawImage((Image) object, 0, 0);
            return;
        }
        if (object instanceof Region) {
            if (((Region) object).isDisposed()) {
                return;
            }
            String string = ((Region) object).getBounds().toString();
            gc.drawString(string, 0, 0);
            return;
        }
    }

    void refreshObject()
    {
        int index = this.list.getSelectionIndex();
        if (index == -1) {
            return;
        }
        if (this.check.getSelection()) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PrintStream s = new PrintStream(stream);
            this.errors[index].printStackTrace(s);
            this.text.setText(stream.toString());
            this.text.setVisible(true);
            this.canvas.setVisible(false);
        } else {
            this.canvas.setVisible(true);
            this.text.setVisible(false);
            this.canvas.redraw();
        }
    }

    void refreshAll()
    {
        this.oldObjects = new Object[0];
        this.oldErrors = new Error[0];
        refreshDifference();
        this.oldObjects = this.objects;
        this.oldErrors = this.errors;
    }

    void layout()
    {
        Rectangle rect = this.shell.getClientArea();
        int width = 0;
        String[] items = this.list.getItems();
        GC gc = new GC(this.list);
        for (int i = 0; i < this.objects.length; i++) {
            width = Math.max(width, gc.stringExtent(items[i]).x);
        }
        gc.dispose();
        Point size1 = this.start.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point size2 = this.stop.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point size3 = this.check.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point size4 = this.label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        width = Math.max(size1.x, Math.max(size2.x, Math.max(size3.x, width)));
        width = Math.max(64, Math.max(size4.x, this.list.computeSize(width, SWT.DEFAULT).x));
        this.start.setBounds(0, 0, width, size1.y);
        this.stop.setBounds(0, size1.y, width, size2.y);
        this.check.setBounds(0, size1.y + size2.y, width, size3.y);
        this.label.setBounds(0, rect.height - size4.y, width, size4.y);
        int height = size1.y + size2.y + size3.y;
        this.list.setBounds(0, height, width, rect.height - height - size4.y);
        this.text.setBounds(width, 0, rect.width - width, rect.height);
        this.canvas.setBounds(width, 0, rect.width - width, rect.height);
    }

}
