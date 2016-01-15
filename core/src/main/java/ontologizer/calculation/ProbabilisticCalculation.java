package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.types.ByteString;

/**
 * This calculation implements the approach described in "A probabilistic generative model for GO enrichment analysis"
 * by Lu et al. TODO: The current implementation is neither elegant nor clean.
 *
 * @author Sebastian Bauer
 */
public class ProbabilisticCalculation implements ICalculation
{
    /** Run data */
    private static class Data
    {
        public double p = 0.5;

        public double q = 0.5;

        public double alpha = 3;

        public int sg;

        public int sn;

        public int st; /* total */

        public int ag;

        public int an;

        public int nsg;

        public Set<TermID> activeTerms = new LinkedHashSet<TermID>();

        /** Active gene nodes connected to at least one active term */
        private HashMap<ByteString, Integer> Ag = new HashMap<ByteString, Integer>();

        /* Fixed (initialized from outside) */
        private GOTermEnumerator popEnumerator;

        private HashSet<ByteString> activeGenes; /* Study genes */

        private HashSet<ByteString> allGenes;

        private List<TermID> allTerms;

        /**
         * Switch the given term (i.e., make it active if not active, make it inactive if active)
         *
         * @param t
         */
        public void switchTerm(TermID t)
        {
            if (this.activeTerms.contains(t)) {
                /* Term is going to be deactivated */
                this.activeTerms.remove(t);

                for (ByteString g : this.popEnumerator.getAnnotatedGenes(t).totalAnnotated) {
                    if (this.activeGenes.contains(g)) {
                        Integer cnt = this.Ag.get(g);
                        if (cnt == 1) {
                            this.Ag.remove(g);
                        } else {
                            this.Ag.put(g, cnt - 1);
                        }
                    } else {
                        /* Gene is inactive but term active */
                        this.nsg--;
                    }
                }
            } else {
                /* Term is going to be activated */
                this.activeTerms.add(t);

                for (ByteString g : this.popEnumerator.getAnnotatedGenes(t).totalAnnotated) {
                    if (this.activeGenes.contains(g)) {
                        Integer cnt = this.Ag.get(g);
                        if (cnt == null) {
                            this.Ag.put(g, 1);
                        } else {
                            this.Ag.put(g, cnt + 1);
                        }
                    } else {
                        /* Gene is inactive but term active */
                        this.nsg++;
                    }
                }
            }
        }

        /**
         * The function which should be optimized.
         *
         * @param data
         * @return
         */
        public double objective()
        {
            calculateParamters();

            double obj;

            obj =
                this.ag * Math.log(this.p) + this.an * Math.log(this.q) + this.sg * Math.log(1 - this.p) + this.sn
                    * Math.log(1 - this.q) - this.alpha
                        * this.activeTerms.size();

            return obj;
        }

        /**
         * Recalculate all parameters based upon the active terms from scratch.
         */
        public void calculateParamtersOld()
        {
            /* Active gene nodes connected to at least one active term */
            // Set<ByteString> Ag = new HashSet<ByteString>();
            HashMap<ByteString, Integer> Ag = new HashMap<ByteString, Integer>();

            /* I inactive gene nodes */
            /* Number of edges connecting nodes in I with active term nodes */
            this.sg = 0;

            /* Number of edges connecting nodes in I with inactive term nodes */
            this.sn = 0;

            Ag.clear();

            for (TermID t : this.activeTerms) {
                for (ByteString g : this.popEnumerator.getAnnotatedGenes(t).totalAnnotated) {
                    if (this.activeGenes.contains(g)) {
                        // Ag.add(g);
                        Integer cnt = Ag.get(g);
                        if (cnt == null) {
                            Ag.put(g, 1);
                        } else {
                            Ag.put(g, cnt + 1);
                        }
                    } else {
                        /* Gene is inactive but term active */
                        this.sg++;
                    }
                }
            }

            // for (TermID t : allTerms)
            // {
            // if (activeTerms.contains(t))
            // continue;
            //
            // /* Inactive terms */
            // for (ByteString g : popEnumerator.getAnnotatedGenes(t).totalAnnotated)
            // {
            // if (!activeGenes.contains(g))
            // {
            // /* Gene is inactive as terms are inactive */
            // sn++;
            // }
            // }
            // }

            /* Active gene nodes connected to at least one active term */
            this.ag = Ag.size();

            /* Active gene nodes not connected to any active term */
            this.an = this.activeGenes.size() - this.ag;

            this.sn = this.st - this.sg;
        }

        /**
         * Recalculate all parameters based upon the active terms from scratch.
         */
        public void calculateParamters()
        {
            /* I inactive gene nodes */
            /* Number of edges connecting nodes in I with active term nodes */
            this.sg = this.nsg;

            /* Number of edges connecting nodes in I with inactive term nodes */
            this.sn = this.st - this.sg;

            /* Active gene nodes connected to at least one active term */
            this.ag = this.Ag.size();

            /* Active gene nodes not connected to any active term */
            this.an = this.activeGenes.size() - this.ag;
        }

        /**
         * Optimize the objective the for active terms.
         *
         * @param graph
         * @return
         */
        private double optimizeForTerms(Ontology graph)
        {
            /* No active terms in the init phase. */
            this.activeTerms.clear();
            this.Ag.clear();
            this.sg = 0;
            this.nsg = 0;

            double obj = objective();

            do {
                double best = Double.NEGATIVE_INFINITY;
                TermID bestTerm = null;

                // System.out.println(obj + " " + best + " " + activeTerms.size());

                for (TermID t : this.allTerms) {
                    switchTerm(t);

                    double o = objective();
                    if (o > best) {
                        best = o;
                        bestTerm = t;
                    }

                    switchTerm(t);
                }

                if (bestTerm != null && best > obj) {
                    switchTerm(bestTerm);
                    obj = objective();
                } else {
                    break;
                }
            } while (true);

            return obj;
        }

    }

    private double defaultP = Double.NaN;

    private double defaultQ = Double.NaN;

    public ProbabilisticCalculation()
    {
    }

    public ProbabilisticCalculation(ProbabilisticCalculation calc)
    {
        this.defaultP = calc.defaultP;
        this.defaultQ = calc.defaultQ;
    }

    public void setDefaultP(double defaultP)
    {
        this.defaultP = defaultP;
    }

    public void setDefaultQ(double defaultQ)
    {
        this.defaultQ = defaultQ;
    }

    @Override
    public EnrichedGOTermsResult calculateStudySet(Ontology graph,
        AssociationContainer goAssociations, PopulationSet populationSet,
        StudySet studySet, AbstractTestCorrection testCorrection)
    {
        Data data = new Data();
        data.popEnumerator = populationSet.enumerateGOTerms(graph, goAssociations);
        data.allTerms = data.popEnumerator.getAllAnnotatedTermsAsList();
        data.allGenes = populationSet.getAllGeneNames();
        data.activeGenes = studySet.getAllGeneNames();

        int total = 0;
        for (TermID t : data.allTerms) {
            /* Inactive terms */
            for (ByteString g : data.popEnumerator.getAnnotatedGenes(t).totalAnnotated) {
                if (!data.activeGenes.contains(g)) {
                    total++;
                }
            }
        }
        data.st = total;

        if (Double.isNaN(this.defaultP)) {
            data.p = 0.5;
        } else {
            data.p = this.defaultP;
        }

        if (Double.isNaN(this.defaultQ)) {
            data.q = ((double) data.activeGenes.size()) / data.allGenes.size();
        } else {
            data.q = this.defaultQ;
        }

        double eps = 0.0001;

        data.calculateParamters();

        while (true) {
            data.optimizeForTerms(graph);

            data.calculateParamters();
            double pNext = (double) (data.ag) / (data.ag + data.sg);
            double qNext = (double) (data.an) / (data.an + data.sn);

            if (!Double.isNaN(this.defaultP) && !Double.isNaN(this.defaultQ)) {
                break;
            }

            if (Double.isNaN(pNext) || Double.isNaN(qNext)) {
                System.err.println("Breaked: #terms=" + data.activeTerms.size() + " ag=" + data.ag + " sg=" + data.sg);
                break;
            }

            // System.out.println("p=" + data.p + " q=" + data.q + " pNext="+pNext + " qNext="+qNext);

            if (Math.abs(qNext - data.q) < eps) {
                break;
            }
            if (Math.abs(pNext - data.p) < eps) {
                break;
            }

            data.p = pNext;
            data.q = qNext;
        }

        /* Now do the term for term calculation */
        /* TODO: Fix MTC issue */
        TermForTermCalculation tftc = new TermForTermCalculation();
        EnrichedGOTermsResult results =
            tftc.calculateStudySet(graph, goAssociations, populationSet, studySet, testCorrection);
        results.setCalculationName("GenGO");

        /* Merge results and flag all inactive terms as insignificant */

        System.out.println("Found " + data.activeTerms.size() + " terms");

        for (AbstractGOTermProperties prop : results) {
            if (!data.activeTerms.contains(prop.goTerm.getID())) {
                prop.p = prop.p_adjusted = 1;
                prop.ignoreAtMTC = true;
            }
            // System.out.println(prop.goTerm.toString() + " " + prop.p_adjusted);
        }

        return results;
    }

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName()
    {
        return "GenGO";
    }

    /**
     * Used for the companion method.
     */
    @Override
    public boolean supportsTestCorrection()
    {
        return true;
    }

}
