package ontologizer.gui.swt.threads;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import ontologizer.FileCache;
import ontologizer.FileCache.FileDownload;
import ontologizer.OntologizerThreadGroups;
import ontologizer.gui.swt.ResultWindow;

/**
 * This is the parent class of all Ontologizer analyse threads.
 *
 * @author Sebastian Bauer
 */
public abstract class AbstractOntologizerThread extends Thread
{
    protected Display display;

    protected ResultWindow result;

    private Runnable calledWhenFinished;

    public AbstractOntologizerThread(String threadName, Runnable calledWhenFinnished, Display d, ResultWindow r)
    {
        super(OntologizerThreadGroups.workerThreadGroup, threadName);

        this.display = d;
        this.result = r;
        this.calledWhenFinished = calledWhenFinnished;
    }

    /**
     * Basic runnable which appends a given text to the result window.
     *
     * @author Sebastian Bauer
     */
    class ResultAppendLogRunnable implements Runnable
    {
        String log;

        ResultAppendLogRunnable(String log)
        {
            this.log = log;
        }

        @Override
        public void run()
        {
            AbstractOntologizerThread.this.result.appendLog(this.log);
        }
    }

    @Override
    final public void run()
    {
        perform();
        this.calledWhenFinished.run();
    }

    /**
     * Method to be implemented by subclasses.
     */
    public abstract void perform();

    /**
     * Downloads a file in a synchronous manner.
     *
     * @param filename
     * @param message defines the message sent to the result window.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    protected String downloadFile(String filename, final String message)
        throws IOException, InterruptedException
    {
        String newPath = FileCache.getCachedFileNameBlocking(filename,
            new FileDownload()
            {
                private boolean messageSeen;

                @Override
                public void initProgress(final int max)
                {
                    if (!this.messageSeen) {
                        AbstractOntologizerThread.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!AbstractOntologizerThread.this.result.isDisposed()) {
                                    AbstractOntologizerThread.this.result.appendLog(message);
                                    AbstractOntologizerThread.this.result.updateProgress(0);
                                    AbstractOntologizerThread.this.result.showProgressBar();
                                }
                            }
                        });
                        this.messageSeen = true;
                    }

                    if (max == -1) {
                        return;
                    }

                    if (!AbstractOntologizerThread.this.result.isDisposed()) {
                        AbstractOntologizerThread.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!AbstractOntologizerThread.this.result.isDisposed()) {
                                    AbstractOntologizerThread.this.result.initProgress(max);
                                }
                            }
                        });
                    }
                }

                @Override
                public void progress(final int current)
                {
                    if (!AbstractOntologizerThread.this.result.isDisposed()) {
                        AbstractOntologizerThread.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!AbstractOntologizerThread.this.result.isDisposed()) {
                                    AbstractOntologizerThread.this.result.updateProgress(current);
                                }
                            }
                        });

                    }

                }

                @Override
                public void ready(Exception ex, String name)
                {
                }
            });
        return newPath;
    }

}
