package ontologizer.gui.swt.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.IAssociationParserProgress;
import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.ICalculationProgress;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.filter.GeneFilter;
import ontologizer.go.IOBOParserProgress;
import ontologizer.go.OBOParser;
import ontologizer.go.Ontology;
import ontologizer.go.TermContainer;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.ResultWindow;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetList;
import ontologizer.statistics.AbstractResamplingTestCorrection;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.IResamplingProgress;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.types.ByteString;

public class AnalyseThread extends AbstractOntologizerThread
{
    private static Logger logger = Logger.getLogger(AnalyseThread.class.getName());

    private String definitionFile;

    private String associationsFile;

    private String mappingFile;

    private String methodName;

    private String mtcName;

    private String subsetName;

    private String subontologyName;

    private PopulationSet populationSet;

    private StudySetList studySetList;

    private int numberOfPermutations;

    private Collection<String> checkedEvidences;

    private double alpha, upperAlpha, beta, upperBeta;

    private int expectedNumber;

    private int numberOfMCMCSteps;

    public AnalyseThread(Display display, Runnable calledWhenFinished, ResultWindow result,
        String definitionFile, String associationsFile, String mappingFile, PopulationSet populationSet,
        StudySetList studySetList,
        String methodName, String mtcName, String subsetName, String subontologyName,
        Collection<String> checkedEvidences,
        int noP, double alpha, double upperAlpha, double beta, double upperBeta, int expectedNumber,
        int numberOfMCMCSteps)
    {
        super("Analyze Thread", calledWhenFinished, display, result);

        this.definitionFile = definitionFile;
        this.associationsFile = associationsFile;
        this.mappingFile = mappingFile;
        this.populationSet = populationSet;
        this.studySetList = studySetList;
        this.methodName = methodName;
        this.mtcName = mtcName;
        this.subsetName = subsetName;
        this.subontologyName = subontologyName;
        this.checkedEvidences = checkedEvidences;

        this.numberOfPermutations = noP;
        this.alpha = alpha;
        this.upperAlpha = upperAlpha;
        this.beta = beta;
        this.upperBeta = upperBeta;
        this.expectedNumber = expectedNumber;
        this.numberOfMCMCSteps = numberOfMCMCSteps;

        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void perform()
    {
        try {
            if (this.checkedEvidences != null && this.checkedEvidences.size() == 0) {
                logger.info("No evidence filter specified. We assume that evidence code shall be ignored.");
                this.checkedEvidences = null;
            }

            /**
             * Runnable to be used for adding a new result into the result window
             *
             * @author Sebastian Bauer
             */
            class AddResultRunnable implements Runnable
            {
                private AbstractGOTermsResult theResult;

                public AddResultRunnable(AbstractGOTermsResult result)
                {
                    this.theResult = result;
                }

                @Override
                public void run()
                {
                    if (!AnalyseThread.this.result.isDisposed()) {
                        AnalyseThread.this.result.addResults(this.theResult);
                        AnalyseThread.this.result.updateProgress(0);
                    }
                }
            }
            ;

            this.definitionFile = downloadFile(this.definitionFile, "Download OBO file");
            this.associationsFile = downloadFile(this.associationsFile, "Download association file");

            /* Initial progress stuff */
            this.display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    // main.disableAnalyseButton();
                    /* TODO: Add this to the proper location */
                    if (!AnalyseThread.this.result.isDisposed()) {
                        AnalyseThread.this.result.appendLog("Parse OBO File");
                        AnalyseThread.this.result.updateProgress(0);
                        AnalyseThread.this.result.showProgressBar();
                    }
                };
            });

            /* TODO: Merge or change to use OntologizerCore */

            ICalculation calculation = CalculationRegistry.getCalculationByName(this.methodName);
            if (calculation == null) {
                calculation = CalculationRegistry.getDefault();
            }

            if (calculation instanceof Bayes2GOCalculation) {
                Bayes2GOCalculation b2g = (Bayes2GOCalculation) calculation;

                if (!Double.isNaN(this.alpha)) {
                    b2g.setAlpha(this.alpha);
                } else {
                    b2g.setAlpha(B2GParam.Type.MCMC);
                    b2g.setAlphaBounds(0, this.upperAlpha);
                }
                if (!Double.isNaN(this.beta)) {
                    b2g.setBeta(this.beta);
                } else {
                    b2g.setBeta(B2GParam.Type.MCMC);
                    b2g.setBetaBounds(0, this.upperBeta);
                }
                if (this.expectedNumber != -1) {
                    b2g.setExpectedNumber(this.expectedNumber);
                } else {
                    b2g.setExpectedNumber(B2GParam.Type.MCMC);
                }

                b2g.setMcmcSteps(this.numberOfMCMCSteps);

                b2g.setProgress(new ICalculationProgress()
                {
                    @Override
                    public void init(final int max)
                    {
                        AnalyseThread.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!AnalyseThread.this.result.isDisposed()) {
                                    AnalyseThread.this.result.initProgress(max);
                                }
                            }
                        });
                    }

                    @Override
                    public void update(final int current)
                    {
                        /* Abort condition */
                        if (isInterrupted()) {
                            throw new AbortCalculationException();
                        }

                        AnalyseThread.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!AnalyseThread.this.result.isDisposed()) {
                                    AnalyseThread.this.result.updateProgress(current);
                                }
                            }
                        });
                    }
                });
            }

            /* Set the desired test correction or set the default */
            AbstractTestCorrection testCorrection = TestCorrectionRegistry.getCorrectionByName(this.mtcName);
            if (testCorrection == null) {
                testCorrection = TestCorrectionRegistry.getDefault();
            }

            if (testCorrection instanceof IResampling) {
                IResampling resampling = (IResampling) testCorrection;

                /* TODO: Probably, invalidating the cache doesn't make much sense here */

                resampling.resetCache();
                if (this.numberOfPermutations > 0) {
                    resampling.setNumberOfResamplingSteps(this.numberOfPermutations);
                }
            }

            /* OBO */
            OBOParser oboParser = new OBOParser(this.definitionFile, OBOParser.PARSE_DEFINITIONS);
            String diag = oboParser.doParse(new IOBOParserProgress()
            {
                @Override
                public void init(final int max)
                {
                    AnalyseThread.this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!AnalyseThread.this.result.isDisposed()) {
                                AnalyseThread.this.result.initProgress(max);
                            }
                        }
                    });
                }

                @Override
                public void update(final int current, final int terms)
                {
                    /* Abort condition */
                    if (isInterrupted()) {
                        throw new AbortCalculationException();
                    }

                    AnalyseThread.this.display.asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!AnalyseThread.this.result.isDisposed()) {
                                AnalyseThread.this.result.updateProgress(current);
                                AnalyseThread.this.result.appendLog("Parse OBO file (" + terms + " terms)");
                            }
                        }
                    });
                }
            });
            this.display.asyncExec(new ResultAppendLogRunnable(diag));
            this.display.asyncExec(new ResultAppendLogRunnable("Building GO graph"));
            TermContainer goTerms =
                new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
            Ontology goGraph = new Ontology(goTerms);
            if (this.subsetName != null) {
                goGraph.setRelevantSubset(this.subsetName);
            }
            if (this.subontologyName != null) {
                goGraph.setRelevantSubontology(this.subontologyName);
            }

            if (this.mappingFile != null) {
                GeneFilter filter = new GeneFilter(new File(this.mappingFile));

                this.populationSet.applyFilter(filter);

                for (StudySet studySet : this.studySetList) {
                    studySet.applyFilter(filter);
                }
            }

            boolean popWasEmpty = this.populationSet.getGeneCount() == 0;

            /*
             * Check now if all study genes are included within the population, if a particular gene is not contained,
             * add it
             */
            for (ByteString geneName : this.studySetList.getGeneSet()) {
                if (!this.populationSet.contains(geneName)) {
                    this.populationSet.addGene(geneName, "");
                }
            }

            /*
             * Parse the GO association file containing GO annotations for genes or gene products. Results are placed in
             * association parser.
             */
            this.display.asyncExec(new ResultAppendLogRunnable("Parse associations"));
            AssociationParser ap =
                new AssociationParser(this.associationsFile, goTerms, this.populationSet.getAllGeneNames(),
                    this.checkedEvidences, new IAssociationParserProgress()
                    {
                        @Override
                        public void init(final int max)
                        {
                            AnalyseThread.this.display.asyncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (!AnalyseThread.this.result.isDisposed()) {
                                        AnalyseThread.this.result.initProgress(max);
                                    }
                                }
                            });
                        }

                        @Override
                        public void update(final int current)
                        {
                            /* Abort condition */
                            if (isInterrupted()) {
                                throw new AbortCalculationException();
                            }

                            AnalyseThread.this.display.asyncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (!AnalyseThread.this.result.isDisposed()) {
                                        AnalyseThread.this.result.updateProgress(current);
                                    }
                                }
                            });
                        }
                    });
            AssociationContainer goAssociations =
                new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

            if (popWasEmpty) {
                /*
                 * If population set was empty we add all genes whose associations are know to the population set.
                 */
                List<ByteString> l = ap.getListOfObjectSymbols();
                for (ByteString bs : l) {
                    this.populationSet.addGene(bs, "");
                }
            }

            /*
             * Filter out duplicate genes (i.e. different gene names refering to the same gene)
             */
            this.display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    AnalyseThread.this.result.appendLog("Filter out duplicate genes");
                }
            });
            this.populationSet.filterOutDuplicateGenes(goAssociations);
            for (StudySet study : this.studySetList) {
                study.filterOutDuplicateGenes(goAssociations);
            }

            /* Filter out genes within the study without any annotations */
            this.display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    AnalyseThread.this.result.appendLog("Removing unannotated genes");
                }
            });
            for (StudySet study : this.studySetList) {
                study.filterOutAssociationlessGenes(goAssociations);
            }
            /* Filter out genes within the population which doesn't have an annotation */
            this.populationSet.filterOutAssociationlessGenes(goAssociations);

            /* Reset progress bar */
            this.display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    AnalyseThread.this.result.updateProgress(0);
                }
            });

            /* Perform calculation */
            int studyNum = 0;
            ArrayList<EnrichedGOTermsResult> studySetResultList = new ArrayList<>();

            for (StudySet studySet : this.studySetList) {
                /* Abort condition */
                if (isInterrupted()) {
                    throw new AbortCalculationException();
                }

                studyNum++;

                /*
                 * If procedure is a resampling test TODO: Enclose testCorrection by synchronize statement
                 */
                if (testCorrection instanceof AbstractResamplingTestCorrection) {
                    AbstractResamplingTestCorrection rtc = (AbstractResamplingTestCorrection) testCorrection;
                    final AnalyseThread t = this;

                    rtc.setProgressUpdate(new IResamplingProgress()
                    {
                        @Override
                        public void init(final int max)
                        {
                            AnalyseThread.this.display.asyncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (!AnalyseThread.this.result.isDisposed()) {
                                        AnalyseThread.this.result.initProgress(max);
                                    }
                                }
                            });
                        }

                        @Override
                        public void update(final int current)
                        {
                            /* If thread is interrupted throw a Runtime Exception */
                            if (t.isInterrupted()) {
                                throw new AbortCalculationException();
                            }

                            AnalyseThread.this.display.asyncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (!AnalyseThread.this.result.isDisposed()) {
                                        AnalyseThread.this.result.updateProgress(current);
                                    }
                                }
                            });
                        }
                    });
                }

                this.display.asyncExec(new ResultAppendLogRunnable(
                    "Perform analysis on study set " + studyNum + " (out of " + this.studySetList.size() + ")"));

                EnrichedGOTermsResult studySetResult = calculation.calculateStudySet(
                    goGraph, goAssociations, this.populationSet, studySet,
                    testCorrection);

                /*
                 * Reset the counter and enumerator items here. It is not necessarily nice to place it here, but for the
                 * moment it's the easiest way
                 */
                studySet.resetCounterAndEnumerator();

                if (testCorrection instanceof AbstractResamplingTestCorrection) {
                    AbstractResamplingTestCorrection rtc = (AbstractResamplingTestCorrection) testCorrection;
                    rtc.setProgressUpdate(null);
                }

                this.display.asyncExec(new AddResultRunnable(studySetResult));

                studySetResultList.add(studySetResult);
            }

            /* Eigen stuff */
            /*
             * try { SVDResult svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, false, false);
             * display.asyncExec(new AddResultRunnable(svdResult)); svdResult = SVD.doSVD(goTerms, goGraph,
             * studySetResultList, populationSet, true, false); display.asyncExec(new AddResultRunnable(svdResult));
             * svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, false, true);
             * display.asyncExec(new AddResultRunnable(svdResult)); svdResult = SVD.doSVD(goTerms, goGraph,
             * studySetResultList, populationSet, true, true); display.asyncExec(new AddResultRunnable(svdResult)); }
             * catch (final Exception e) { display.syncExec(new Runnable() { public void run() {
             * SWTUtil.displayException(main.getShell(), e, "Error while performing SVD. No results are displayed.\n");
             * } }); }
             */
            this.display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    if (!AnalyseThread.this.result.isDisposed()) {
                        AnalyseThread.this.result.setBusyPointer(false);
                        AnalyseThread.this.result.appendLog("Calculation finished");
                        AnalyseThread.this.result.clearProgressText();
                        AnalyseThread.this.result.hideProgressBar();
                    }
                };
            });

        } catch (AbortCalculationException e) {
            /* Do nothing */
        } catch (InterruptedException e) {
            /* Do nothing */
        } catch (final Exception e) {
            if (!interrupted()) {
                this.display.syncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AnalyseThread.this.result.dispose();
                        Ontologizer.logException(e);
                    }
                });
            }
        }
    }
};
