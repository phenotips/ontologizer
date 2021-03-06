package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;

/**
 * This is not a significance test, but a dummy class which independently samples uniform p-values for each term. I
 * think I can use it to write a test for the Westfall-Young MTC.
 *
 * @author Steffen Grossmann
 */
public class IndependentUniformCalculation implements ICalculation
{
    @Override
    public String getName()
    {
        return "Independent-Uniform";
    }

    @Override
    public String getDescription()
    {
        return "No description yet";
    }

    /**
     * Start calculation based on fisher exact test of the given study.
     *
     * @param graph
     * @param goAssociations
     * @param populationSet
     * @param studySet
     * @return
     */
    @Override
    public EnrichedGOTermsResult calculateStudySet(Ontology graph,
        AssociationContainer goAssociations, PopulationSet populationSet,
        StudySet studySet, AbstractTestCorrection testCorrection)
    {
        EnrichedGOTermsResult studySetResult =
            new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
        studySetResult.setCalculationName(this.getName());
        studySetResult.setCorrectionName(testCorrection.getName());

        /**
         * This class hides all the details about how the p values are calculated from the multiple test correction.
         *
         * @author Sebastian Bauer
         */
        class SinglePValuesCalculation implements IPValueCalculation
        {
            public PopulationSet populationSet;

            public StudySet observedStudySet;

            public Ontology graph;

            @Override
            public int currentStudySetSize()
            {
                return this.observedStudySet.getGeneCount();
            }

            private PValue[] calculatePValues(StudySet studySet)
            {
                int i = 0;

                PValue p[] = new PValue[this.graph.getNumberOfTerms()];
                TermForTermGOTermProperties myP;

                for (Term goterm : this.graph) {
                    String term = goterm.getIDAsString();

                    int goidAnnotatedPopGeneCount = this.populationSet.getGeneCount();
                    int goidAnnotatedStudyGeneCount = this.observedStudySet.getGeneCount();

                    myP = new TermForTermGOTermProperties();
                    myP.goTerm = this.graph.getTerm(term);
                    myP.annotatedStudyGenes = goidAnnotatedStudyGeneCount;
                    myP.annotatedPopulationGenes = goidAnnotatedPopGeneCount;

                    myP.p = Math.random();
                    myP.p_min = 0.0;

                    p[i++] = myP;
                }
                return p;
            }

            @Override
            public PValue[] calculateRawPValues()
            {
                return calculatePValues(this.observedStudySet);
            }

            @Override
            public PValue[] calculateRandomPValues()
            {
                return calculatePValues(
                    this.populationSet.generateRandomStudySet(this.observedStudySet.getGeneCount()));
            }
        }

        SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation();
        pValueCalculation.graph = graph;
        pValueCalculation.populationSet = populationSet;
        pValueCalculation.observedStudySet = studySet;
        PValue p[] = testCorrection.adjustPValues(pValueCalculation);

        /*
         * Add the results to the result list and filter out terms with no annotated genes.
         */
        for (PValue element : p) {
            /* Entries are SingleGOTermProperties */
            TermForTermGOTermProperties prop = (TermForTermGOTermProperties) element;

            /* Within the result ignore terms without any annotation */
            if (prop.annotatedStudyGenes == 0) {
                continue;
            }

            studySetResult.addGOTermProperties(prop);
        }

        return studySetResult;
    }

    @Override
    public boolean supportsTestCorrection()
    {
        return true;
    }

}
