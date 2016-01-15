package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;

/**
 * Significant test using a simple independent Fisher Exact calculation for every single term.
 *
 * @author Sebastian Bauer
 */
public class TermForTermCalculation extends AbstractHypergeometricCalculation
{
    @Override
    public String getName()
    {
        return "Term-For-Term";
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
    public EnrichedGOTermsResult calculateStudySet(
        Ontology graph,
        AssociationContainer goAssociations,
        PopulationSet populationSet,
        StudySet studySet,
        AbstractTestCorrection testCorrection)
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

            public AssociationContainer goAssociations;

            public Ontology graph;

            private PValue[] calculatePValues(StudySet studySet)
            {
                GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(this.graph, this.goAssociations);
                GOTermEnumerator populationTermEnumerator =
                    this.populationSet.enumerateGOTerms(this.graph, this.goAssociations);

                int i = 0;

                PValue p[] = new PValue[populationTermEnumerator.getTotalNumberOfAnnotatedTerms()];

                TermForTermGOTermProperties myP;

                for (TermID term : populationTermEnumerator) {
                    int goidAnnotatedPopGeneCount =
                        populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
                    int popGeneCount = this.populationSet.getGeneCount();
                    int studyGeneCount = studySet.getGeneCount();
                    int goidAnnotatedStudyGeneCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

                    myP = new TermForTermGOTermProperties();
                    myP.goTerm = this.graph.getTerm(term);
                    myP.annotatedStudyGenes = goidAnnotatedStudyGeneCount;
                    myP.annotatedPopulationGenes = goidAnnotatedPopGeneCount;

                    if (goidAnnotatedStudyGeneCount != 0) {
                        /*
                         * Imagine the following... In an urn you put popGeneCount number of balls where a color of a
                         * ball can be white or black. The number of balls having white color is
                         * goidAnnontatedPopGeneCount (all genes of the population which are annotated by the current
                         * GOID). You choose to draw studyGeneCount number of balls without replacement. How big is the
                         * probability, that you got goidAnnotatedStudyGeneCount white balls after the whole drawing
                         * process?
                         */

                        myP.p =
                            TermForTermCalculation.this.hyperg.phypergeometric(popGeneCount,
                                (double) goidAnnotatedPopGeneCount
                                    / (double) popGeneCount,
                                studyGeneCount, goidAnnotatedStudyGeneCount);
                        myP.p_min = TermForTermCalculation.this.hyperg.dhyper(
                            goidAnnotatedPopGeneCount,
                            popGeneCount,
                            goidAnnotatedPopGeneCount,
                            goidAnnotatedPopGeneCount);
                    } else {
                        /* Mark this p value as irrelevant so it isn't considered in a mtc */
                        myP.p = 1.0;
                        myP.ignoreAtMTC = true;
                        myP.p_min = 1.0;
                    }

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
            public int currentStudySetSize()
            {
                return this.observedStudySet.getGeneCount();
            }

            @Override
            public PValue[] calculateRandomPValues()
            {
                return calculatePValues(
                    this.populationSet.generateRandomStudySet(this.observedStudySet.getGeneCount()));
            }
        }
        ;

        SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation();
        pValueCalculation.goAssociations = goAssociations;
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
