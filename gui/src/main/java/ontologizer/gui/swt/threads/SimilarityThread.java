package ontologizer.gui.swt.threads;

import org.eclipse.swt.widgets.Display;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.SemanticCalculation;
import ontologizer.calculation.SemanticResult;
import ontologizer.go.Ontology;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.ResultWindow;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetList;
import ontologizer.worksets.IWorkSetProgress;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

public class SimilarityThread extends AbstractOntologizerThread
{
    private StudySetList studySetList;

    private WorkSet workSet;

    public SimilarityThread(Display display, Runnable calledWhenFinished, ResultWindow result,
        StudySetList studySetList, WorkSet workSet)
    {
        super("Similarity Thread", calledWhenFinished, display, result);

        this.studySetList = studySetList;
        this.workSet = workSet;
    }

    @Override
    public void perform()
    {
        final Object lock = new Object();

        synchronized (lock) {
            WorkSetLoadThread.obtainDatafiles(this.workSet, new IWorkSetProgress()
            {
                @Override
                public void message(final String message)
                {
                    SimilarityThread.this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!SimilarityThread.this.result.isDisposed()) {
                                SimilarityThread.this.result.appendLog(message);
                            }
                        }
                    });
                }

                @Override
                public void initGauge(final int maxWork)
                {
                    SimilarityThread.this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!SimilarityThread.this.result.isDisposed()) {
                                SimilarityThread.this.result.updateProgress(0);

                                if (maxWork > 0) {
                                    SimilarityThread.this.result.initProgress(maxWork);
                                    SimilarityThread.this.result.showProgressBar();
                                } else {
                                    SimilarityThread.this.result.hideProgressBar();
                                }
                            }
                        }
                    });
                }

                @Override
                public void updateGauge(final int currentWork)
                {
                    SimilarityThread.this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!SimilarityThread.this.result.isDisposed()) {
                                SimilarityThread.this.result.updateProgress(currentWork);
                            }
                        }
                    });

                }
            }, new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });

            try {
                lock.wait();

                /* Stuff should have been loaded at this point */

                Ontology graph = WorkSetLoadThread.getGraph(this.workSet.getOboPath());
                AssociationContainer assoc = WorkSetLoadThread.getAssociations(this.workSet.getAssociationPath());

                if (graph == null) {
                    throw new RuntimeException("Error in loading the ontology graph!");
                }
                if (assoc == null) {
                    throw new RuntimeException("Error in loading the associations!");
                }

                this.display.asyncExec(new ResultAppendLogRunnable("Preparing semantic calculation"));

                SemanticCalculation s = new SemanticCalculation(graph, assoc);

                for (StudySet studySet : this.studySetList) {
                    this.display
                        .asyncExec(new ResultAppendLogRunnable("Analyzing study set \"" + studySet.getName() + "\""));

                    final SemanticResult sr =
                        s.calculate(studySet, new SemanticCalculation.ISemanticCalculationProgress()
                        {
                            @Override
                            public void init(final int max)
                            {
                                SimilarityThread.this.display.asyncExec(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        SimilarityThread.this.result.showProgressBar();
                                        SimilarityThread.this.result.initProgress(max);
                                    }
                                });
                            }

                            @Override
                            public void update(final int update)
                            {
                                SimilarityThread.this.display.asyncExec(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        SimilarityThread.this.result.updateProgress(update);
                                    }
                                });
                            }
                        });

                    this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            SimilarityThread.this.result.addResults(sr);
                        }
                    });

                }

                this.display.asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (!SimilarityThread.this.result.isDisposed()) {
                            SimilarityThread.this.result.setBusyPointer(false);
                            SimilarityThread.this.result.appendLog("Calculation finished");
                            SimilarityThread.this.result.clearProgressText();
                            SimilarityThread.this.result.hideProgressBar();
                        }
                    };
                });

                WorkSetLoadThread.releaseDatafiles(this.workSet);
            } catch (InterruptedException e) {

            } catch (RuntimeException re) {
                Ontologizer.logException(re);
                this.display.asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (!SimilarityThread.this.result.isDisposed()) {
                            SimilarityThread.this.result.dispose();
                        }
                    };
                });
            }

        }
    }
}
