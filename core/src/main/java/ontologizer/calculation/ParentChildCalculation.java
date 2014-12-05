package ontologizer.calculation;

import java.util.HashSet;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;

public class ParentChildCalculation extends
AbstractHypergeometricCalculation
{

    @Override
    public String getName()
    {
        return "Parent-Child-Union";
    }

    @Override
    public String getDescription()
    {
        return "We calculate p-values measuring over-representation" +
            "of GO term annotated genes in a study set by comparing" +
            "a term's annotation to the annotation of its parent terms.";
    }

    @Override
    public EnrichedGOTermsResult calculateStudySet(
        Ontology graph,
        AssociationContainer goAssociations,
        PopulationSet popSet,
        StudySet studySet,
        AbstractTestCorrection testCorrection)
    {

        EnrichedGOTermsResult studySetResult =
            new EnrichedGOTermsResult(graph, goAssociations, studySet, popSet.getGeneCount());
        studySetResult.setCalculationName(this.getName());
        studySetResult.setCorrectionName(testCorrection.getName());

        /**
         * This class hides all the details about how the p values are calculated from the multiple test correction.
         *
         * @author Sebastian Bauer
         */
        class ParentChildPValuesCalculation implements IPValueCalculation
        {
            /*
             * We basically have the arguments of calculateStudy as fields
             */
            public Ontology graph;

            public AssociationContainer goAssociations;

            public PopulationSet populationSet;

            public GOTermEnumerator popTermEnumerator;

            public StudySet observedStudySet;

            private PValue[] calculatePValues(StudySet studySet)
            {
                /* We need this to get genes annotated in the study set */
                GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(this.graph,
                    this.goAssociations);

                // PValue p [] = new PValue[populationTermCounter.getTotalNumberOfAnnotatedTerms()];
                PValue p[] = new PValue[this.popTermEnumerator.getTotalNumberOfAnnotatedTerms()];
                int i = 0;

                /* For every term within the goTermCounter */
                for (TermID term : this.popTermEnumerator)
                {
                    // calculating properties of term
                    ParentChildGOTermProperties termProp = calculateTerm(term, this.graph,
                        this.popTermEnumerator, studyTermEnumerator);

                    // adding properties to p Vector
                    p[i++] = termProp;
                }

                return p;
            }

            @Override
            public int currentStudySetSize()
            {
                return this.observedStudySet.getGeneCount();
            }

            @Override
            public PValue[] calculateRawPValues()
            {
                return calculatePValues(this.observedStudySet);
            }

            @Override
            public PValue[] calculateRandomPValues()
            {
                return calculatePValues(this.populationSet.generateRandomStudySet(this.observedStudySet.getGeneCount()));
            }

            private ParentChildGOTermProperties calculateTerm(
                TermID term,
                Ontology graph,
                GOTermEnumerator popTermEnumerator,
                GOTermEnumerator studyTermEnumerator)
            {
                // counts annotated to term
                int studyTermCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
                int popTermCount = popTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

                // this is what we give back
                ParentChildGOTermProperties prop = new ParentChildGOTermProperties();
                prop.goTerm = graph.getTerm(term);
                prop.annotatedPopulationGenes = popTermCount;
                prop.annotatedStudyGenes = studyTermCount;

                if (graph.isRootTerm(term)) {
                    prop.nparents = 0;
                    prop.ignoreAtMTC = true;
                    prop.p = 1.0;
                    prop.p_adjusted = 1.0;
                    prop.p_min = 1.0;
                } else {
                    // getting parents
                    Set<TermID> parents = graph.getTermParents(term);

                    // These will hold the names of all genes directly annotated to parents
                    HashSet<ByteString> popParentAllGenes = new HashSet<ByteString>();
                    HashSet<ByteString> studyParentAllGenes = new HashSet<ByteString>();

                    // looping over all parents to get the genes and adding all annotated genes to HashSets
                    for (TermID parent : parents)
                    {
                        popParentAllGenes.addAll(
                            popTermEnumerator.getAnnotatedGenes(parent).totalAnnotated
                            );
                        studyParentAllGenes.addAll(
                            studyTermEnumerator.getAnnotatedGenes(parent).totalAnnotated
                            );
                    }

                    // number of genes annotated to family (term and parents)
                    int popFamilyCount = popParentAllGenes.size();
                    int studyFamilyCount = studyParentAllGenes.size();

                    prop.popFamilyGenes = popFamilyCount;
                    prop.studyFamilyGenes = studyFamilyCount;
                    prop.nparents = parents.size();

                    if (studyTermCount != 0) {
                        if (popFamilyCount == popTermCount) {
                            prop.ignoreAtMTC = true;
                            prop.p = 1.0;
                            prop.p_adjusted = 1.0;
                            prop.p_min = 1.0;
                        } else {
                            double p = ParentChildCalculation.this.hyperg.phypergeometric(
                                popFamilyCount,
                                (double) popTermCount / (double) popFamilyCount,
                                studyFamilyCount,
                                studyTermCount);

                            prop.ignoreAtMTC = false;
                            prop.p = p;
                            prop.p_min = ParentChildCalculation.this.hyperg.dhyper(
                                popTermCount,
                                popFamilyCount,
                                popTermCount,
                                popTermCount);
                        }
                    } else {
                        prop.ignoreAtMTC = true;
                        prop.p = 1.0;
                        prop.p_adjusted = 1.0;
                        prop.p_min = 1.0;
                    }
                }

                return prop;
            }
        }
        ;

        ParentChildPValuesCalculation pValueCalculation = new ParentChildPValuesCalculation();
        pValueCalculation.goAssociations = goAssociations;
        pValueCalculation.graph = graph;
        pValueCalculation.populationSet = popSet;
        pValueCalculation.popTermEnumerator = popSet.enumerateGOTerms(graph, goAssociations);
        pValueCalculation.observedStudySet = studySet;
        PValue p[] = testCorrection.adjustPValues(pValueCalculation);

        /*
         * Add the results to the result list and filter out terms with no annotated genes.
         */
        for (PValue element : p) {
            /* Entries are SingleGOTermProperties */
            ParentChildGOTermProperties prop = (ParentChildGOTermProperties) element;

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
