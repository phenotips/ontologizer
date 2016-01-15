package ontologizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.IAssociationParserProgress;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.filter.GeneFilter;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.TermContainer;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetFactory;
import ontologizer.set.StudySetList;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.types.ByteString;

/**
 * The OntologizerCore class controls parsing and output of Gene Ontology cluster information. It forms a common
 * interface between the main methods in the class OntologizerCMD (Command line) and OntoFrame (GUI version) to the rest
 * of the program logic of the Ontologizer.
 *
 * @author Peter Robinson, Sebastian Bauer
 */
public class OntologizerCore
{
    /**
     * @author Sebastian Bauer This class is indented to pass arguments to the OntologizerCore constructor.
     */
    public static class Arguments
    {
        /** gene_ontology.obo file (and path) */
        public String goTermsOBOFile;

        /** gene_association.* file (and path) */
        public String associationFile;

        /** Name of the study file (or directory) */
        public String studySet;

        /** Only study input files with this suffix are considered */
        public String suffix;

        /** Name of the main output File */
        public String mainOutputName;

        /** Name of the calculation (e.g. singlestep) */
        public String calculationName;

        /** Name of the multiple test correction procedure */
        public String correctionName;

        /**
         * Number of resampling steps in case of a resampling based mtc procedure
         */
        public int resamplingSteps;

        /**
         * Tolerance in percent for Westfall-Young-Approximate
         */
        public int sizeTolerance;

        /** Minium number of association to be displayed */
        public int minAssociationCount;

        /** Specifies the population file containing the whole gene population, might be null */
        public String populationFile;

        /** Should genes be filtered out, from which there exists no annotation? */
        public boolean filterOutUnannotatedGenes;

        /** Specifies the filter file */
        public String filterFile;
    };

    /** Contains all avaiable GOTerms */
    private TermContainer goTerms;

    /** The graph to the gene ontolgy */
    private Ontology goGraph;

    /** List of all studies being analyzed */
    private StudySetList studySetList;

    /** Results of the analized studies */
    private StudySetResultList studySetResultList;

    /** Collection of all valid annotations for current dataset */
    private AssociationContainer goAssociations;

    /** The kind of calculation which should be performed */
    private ICalculation calculation;

    /** The correction which should be used */
    private AbstractTestCorrection testCorrection;

    /**
     * The population object. If this is set we do the hypergeometric analysis basded on the scheme in GeneMerge.
     */
    private PopulationSet populationSet;

    /**
     * Construct the object.
     *
     * @param args specifies the arguments
     * @throws IOException
     * @throws FileNotFoundException
     * @throws myException
     */
    public OntologizerCore(Arguments args) throws FileNotFoundException, IOException, OBOParserException
    {
        /* Set the desired calculation method or the default */
        this.calculation = CalculationRegistry.getCalculationByName(args.calculationName);
        if (this.calculation == null) {
            this.calculation = CalculationRegistry.getDefault();
        }
        if (this.calculation instanceof Bayes2GOCalculation) {
            Bayes2GOCalculation b2g = (Bayes2GOCalculation) this.calculation;
            b2g.setAlpha(B2GParam.Type.MCMC);
            b2g.setBeta(B2GParam.Type.MCMC);
            b2g.setExpectedNumber(B2GParam.Type.MCMC);
            b2g.setMcmcSteps(1000000);
        }

        /* Set the desired test correction or set the default */
        this.testCorrection = TestCorrectionRegistry.getCorrectionByName(args.correctionName);
        if (this.testCorrection == null) {
            this.testCorrection = TestCorrectionRegistry.getDefault();
        }
        /* Empty cache for resampling based MTCs and set number of sampling steps */
        if (this.testCorrection instanceof IResampling) {
            IResampling resampling = (IResampling) this.testCorrection;
            resampling.resetCache();
            if (args.resamplingSteps > 0) {
                resampling.setNumberOfResamplingSteps(args.resamplingSteps);

            }
            if (args.sizeTolerance > 0) {
                resampling.setSizeTolerance(args.sizeTolerance);
            }
        }

        /*
         * Parse the gene_ontology.obo file to get information about all terms. Transfer the information to a
         * TermContainer object.
         */
        System.err.println("Parse obo file \"" + args.goTermsOBOFile + "\"");

        OBOParser oboParser = new OBOParser(args.goTermsOBOFile);
        System.err.println(oboParser.doParse());
        this.goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
        System.err.println("Building graph");
        this.goGraph = new Ontology(this.goTerms);

        /* create the study list. A directory or a single file might be given */
        File studyFile = new File(args.studySet);
        if (studyFile.isDirectory()) {
            this.studySetList = new StudySetList(args.studySet, args.suffix);
        } else {
            /* Create a study list with a dummy name and add the study manually */
            this.studySetList = new StudySetList("study");
            this.studySetList.addStudySet(StudySetFactory.createFromFile(studyFile, false));
        }

        /* create the population set TODO: Get rid of the casting */
        this.populationSet = (PopulationSet) StudySetFactory.createFromFile(new File(args.populationFile), true);

        /* Apply the optional gene name mapping given by the supplied filter file */
        if (args.filterFile != null) {
            System.err.println("Parsing filter \"" + args.filterFile + "\"");
            GeneFilter filter = new GeneFilter(new File(args.filterFile));

            System.err.println("Appling filter");
            this.populationSet.applyFilter(filter);

            for (StudySet studySet : this.studySetList) {
                studySet.applyFilter(filter);
            }
        }

        /*
         * Check now if all study genes are included within the population, if a partiulcar gene is not contained, add
         * it
         */
        for (ByteString geneName : this.studySetList.getGeneSet()) {
            if (!this.populationSet.contains(geneName)) {
                this.populationSet.addGene(geneName, "");
            }
        }

        /*
         * Parse the GO association file containing GO annotations for genes or gene products. Results are placed in
         * associationparser.
         */
        AssociationParser ap =
            new AssociationParser(args.associationFile, this.goTerms, this.populationSet.getAllGeneNames(),
                new IAssociationParserProgress()
                {
                    private int max;

                    private long startTime;

                    @Override
                    public void init(int max)
                    {
                        this.max = max;
                        this.startTime = System.currentTimeMillis();
                    }

                    @Override
                    public void update(int current)
                    {
                        long currentTime = System.currentTimeMillis();

                        if (currentTime - this.startTime > 20000) {
                            /* Show progress */
                            System.err.print("\033[1A\033[K");
                            System.err.println("Reading annotation file: "
                                + String.format("%.1f%%", current / (double) this.max * 100));
                        }
                    }

                });
        this.goAssociations =
            new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

        /*
         * Filter out duplicate genes (i.e. different gene names refering to the same gene)
         */
        this.populationSet.filterOutDuplicateGenes(this.goAssociations);
        for (StudySet study : this.studySetList) {
            study.filterOutDuplicateGenes(this.goAssociations);
        }

        if (args.filterOutUnannotatedGenes) {
            /* Filter out genes within the study without any annotations */
            for (StudySet study : this.studySetList) {
                study.filterOutAssociationlessGenes(this.goAssociations);
            }

            /* Filter out genes within the population which doesn't have an annotation */
            this.populationSet.filterOutAssociationlessGenes(this.goAssociations);
        }
    }

    /**
     * Returns the iterator over all results.
     *
     * @return the iterator.
     */
    public Iterator<EnrichedGOTermsResult> studySetResultIterator()
    {
        /* Create a dummy list in case no results were available */
        if (this.studySetResultList == null) {
            this.studySetResultList = new StudySetResultList();
        }

        return this.studySetResultList.iterator();
    }

    /**
     * Perform the statistical calculation using the given calculation procedure accumulation the results into the
     * studySetResultList
     */
    public void calculate()
    {
        assert(this.populationSet != null);
        this.studySetResultList = new StudySetResultList();

        for (StudySet studySet : this.studySetList) {
            this.studySetResultList.addStudySetResult(
                this.calculation.calculateStudySet(this.goGraph, this.goAssociations, this.populationSet, studySet,
                    this.testCorrection));

            /*
             * Reset the counter and enumerator items here. It is not necessarily nice to place it here, but for the
             * moment it's the easiest way
             */
            studySet.resetCounterAndEnumerator();
        }
    }

    private Iterator<StudySet> studySetIter;

    /**
     * Perform the statistical calculation of the next study. When called first, the first study is considered as the
     * next study.
     *
     * @return the result of the calculation or null, if no more studies are available.
     */
    public EnrichedGOTermsResult calculateNextStudy()
    {
        assert(this.populationSet != null);
        if (this.studySetIter == null) {
            this.studySetIter = this.studySetList.iterator();
        }
        if (!this.studySetIter.hasNext()) {
            return null;
        }

        StudySet studySet = this.studySetIter.next();
        EnrichedGOTermsResult studySetResult =
            this.calculation.calculateStudySet(this.goGraph, this.goAssociations, this.populationSet, studySet,
                this.testCorrection);

        /*
         * Reset the counter and enumerator items here. It is not necessarily nice to place it here, but for the moment
         * it's the easiest way
         */
        studySet.resetCounterAndEnumerator();
        return studySetResult;
    }

    public AssociationContainer getGoAssociations()
    {
        return this.goAssociations;
    }

    public Ontology getGoGraph()
    {
        return this.goGraph;
    }

    public TermContainer getGoTerms()
    {
        return this.goTerms;
    }

    public PopulationSet getPopulationSet()
    {
        return this.populationSet;
    }

    public String getCalculationName()
    {
        return this.calculation.getName();
    }

    public String getTestCorrectionName()
    {
        return this.testCorrection.getName();
    }
}
