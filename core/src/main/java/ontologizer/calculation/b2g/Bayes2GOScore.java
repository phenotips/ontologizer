package ontologizer.calculation.b2g;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * A basic container representing a set of genes
 *
 * @author Sebastian Bauer
 */
class GeneIDs
{
    public int[] gid;

    public GeneIDs(int size)
    {
        this.gid = new int[size];
    }
}

/**
 * The base class of bayes2go Score. For efficiency reasons terms and genes are represented by own ids.
 *
 * @author Sebastian Bauer
 */
abstract public class Bayes2GOScore
{
    /** Source of randomness */
    protected Random rnd;

    protected GOTermEnumerator populationEnumerator;

    protected Set<ByteString> population;

    /** Array of terms */
    protected TermID[] termsArray;

    /** Indicates the activation state of a term */
    protected boolean[] isActive;

    /**
     * Contains indices to terms of termsArray.
     */
    protected int[] termPartition;

    /**
     * The current number of inactive terms. Represents the first part of the partition.
     */
    protected int numInactiveTerms;

    /**
     * Contains the position/index of the terms in the partition (i.e., termPartition[positionOfTermInPartition[i]] = i
     * must hold)
     */
    protected int[] positionOfTermInPartition;

    /** Array indicating the genes that have been observed */
    protected boolean[] observedGenes;

    /** Array that indicate the activation counts of the genes */
    protected int[] activeHiddenGenes;

    /** Maps genes to an unique gene index */
    protected HashMap<ByteString, Integer> gene2GenesIdx = new HashMap<ByteString, Integer>();

    protected ByteString[] genes;

    /** Maps the term to the index in allTermsArray */
    protected HashMap<TermID, Integer> term2TermsIdx = new HashMap<TermID, Integer>();

    /** Maps a term id to the ids of the genes to that the term is annotated */
    protected GeneIDs[] termLinks;

    protected int numRecords;

    protected int[] termActivationCounts;

    protected boolean usePrior = true;

    protected double p = Double.NaN;

    /**
     * Constructs a class for calculating the Bayes2GO/MGSA score suitable for an MCMC algorithm.
     *
     * @param termList list of terms that can possibly be selected.
     * @param populationEnumerator terms to genes.
     * @param observedActiveGenes defines the set of genes that are observed as active.
     */
    public Bayes2GOScore(List<TermID> termList, GOTermEnumerator populationEnumerator,
        Set<ByteString> observedActiveGenes)
    {
        this(null, termList, populationEnumerator, observedActiveGenes);
    }

    /**
     * Constructs a class for calculating the Bayes2GO score suitable for an MCMC algorithm.
     *
     * @param rnd Random source for proposing states.
     * @param termList list of terms that can possibly be selected.
     * @param populationEnumerator terms to genes.
     * @param observedActiveGenes defines the set of genes that are observed as active.
     */
    public Bayes2GOScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator,
        Set<ByteString> observedActiveGenes)
    {
        int i;

        this.rnd = rnd;

        /* Initialize basics of genes */
        this.population = populationEnumerator.getGenes();
        this.genes = new ByteString[this.population.size()];
        this.observedGenes = new boolean[this.genes.length];
        i = 0;
        for (ByteString g : this.population) {
            this.gene2GenesIdx.put(g, i);
            this.genes[i] = g;
            this.observedGenes[i] = observedActiveGenes.contains(g);
            i++;
        }
        this.activeHiddenGenes = new int[this.population.size()];

        /* Initialize basics of terms */
        this.isActive = new boolean[termList.size()];
        this.termsArray = new TermID[termList.size()];
        this.termPartition = new int[termList.size()];
        this.positionOfTermInPartition = new int[termList.size()];
        this.numInactiveTerms = termList.size();
        this.termActivationCounts = new int[termList.size()];
        this.termLinks = new GeneIDs[termList.size()];

        i = 0;
        for (TermID tid : termList) {
            this.term2TermsIdx.put(tid, i);
            this.termsArray[i] = tid;
            this.termPartition[i] = i;
            this.positionOfTermInPartition[i] = i;

            /* Fill in the links */
            this.termLinks[i] = new GeneIDs(populationEnumerator.getAnnotatedGenes(tid).totalAnnotated.size());
            int j = 0;
            for (ByteString gene : populationEnumerator.getAnnotatedGenes(tid).totalAnnotated) {
                this.termLinks[i].gid[j] = this.gene2GenesIdx.get(gene);
                j++;
            }

            i++;
        }

        this.populationEnumerator = populationEnumerator;
    }

    public void setUsePrior(boolean usePrior)
    {
        this.usePrior = usePrior;
    }

    public boolean getUsePrior()
    {
        return this.usePrior;
    }

    public void setExpectedNumberOfTerms(double terms)
    {
        this.p = terms / this.termsArray.length;
    }

    /**
     * Returns the score of the setting if the given terms are active and all others are inactive.
     *
     * @param activeTerms
     * @return
     */
    public double score(Collection<TermID> activeTerms)
    {
        int[] oldTerms = new int[this.termsArray.length - this.numInactiveTerms];
        for (int i = this.numInactiveTerms, j = 0; i < this.termsArray.length; i++, j++) {
            oldTerms[j] = this.termPartition[i];
        }

        /* Deactivate old terms */
        for (int oldTerm : oldTerms) {
            switchState(oldTerm);
        }

        /* Enable new terms */
        for (TermID tid : activeTerms) {
            Integer idx = this.term2TermsIdx.get(tid);
            if (idx != null) {
                switchState(idx);
            }
        }

        double score = getScore();

        /* Disable new terms */
        for (TermID tid : activeTerms) {
            Integer idx = this.term2TermsIdx.get(tid);
            if (idx != null) {
                switchState(idx);
            }
        }

        /* Enable old terms again */
        for (int oldTerm : oldTerms) {
            switchState(oldTerm);
        }

        return score;
    }

    /**
     * Returns the score of the current state.
     *
     * @return
     */
    public abstract double getScore();

    public abstract void proposeNewState(long rand);

    public void proposeNewState()
    {
        proposeNewState(this.rnd.nextLong());
    }

    public abstract void hiddenGeneActivated(int gid);

    public abstract void hiddenGeneDeactivated(int gid);

    // public long currentTime;

    public void switchState(int toSwitch)
    {
        // long enterTime = System.nanoTime();

        int[] geneIDs = this.termLinks[toSwitch].gid;

        this.isActive[toSwitch] = !this.isActive[toSwitch];
        if (this.isActive[toSwitch]) {
            /* A term was added, activate/deactivate genes */
            for (int gid : geneIDs) {
                if (this.activeHiddenGenes[gid] == 0) {
                    this.activeHiddenGenes[gid] = 1;
                    hiddenGeneActivated(gid);
                } else {
                    this.activeHiddenGenes[gid]++;
                }
            }

            /*
             * Move the added set from the 0 partition to the 1 partition (it essentially becomes the new first element
             * of the 1 element, while the last 0 element gets the original position of the added set)
             */
            this.numInactiveTerms--;
            if (this.numInactiveTerms != 0) {
                int pos = this.positionOfTermInPartition[toSwitch];
                int e0 = this.termPartition[this.numInactiveTerms];

                /* Move last element in the partition to left */
                this.termPartition[pos] = e0;
                this.positionOfTermInPartition[e0] = pos;
                /* Let be the newly added term the first in the partition */
                this.termPartition[this.numInactiveTerms] = toSwitch;
                this.positionOfTermInPartition[toSwitch] = this.numInactiveTerms;
            }
        } else {
            /* Update hiddenActiveGenes */
            for (int gid : geneIDs) {
                if (this.activeHiddenGenes[gid] == 1) {
                    this.activeHiddenGenes[gid] = 0;
                    hiddenGeneDeactivated(gid);
                } else {
                    this.activeHiddenGenes[gid]--;
                }
            }

            /*
             * Converse of above. Here the removed set, which belonged to the 1 partition, is moved at the end of the 0
             * partition while the element at that place is pushed to the original position of the removed element.
             */
            if (this.numInactiveTerms != (this.termsArray.length - 1)) {
                int pos = this.positionOfTermInPartition[toSwitch];
                int b1 = this.termPartition[this.numInactiveTerms];
                this.termPartition[pos] = b1;
                this.positionOfTermInPartition[b1] = pos;
                this.termPartition[this.numInactiveTerms] = toSwitch;
                this.positionOfTermInPartition[toSwitch] = this.numInactiveTerms;
            }
            this.numInactiveTerms++;

        }

        // {
        // long ds = currentTime / 100000000;
        // currentTime += System.nanoTime() - enterTime;
        // if (currentTime / 100000000 != ds)
        // System.out.println(currentTime / 1000000);
        // }
    }

    public void exchange(TermID t1, TermID t2)
    {
        switchState(this.term2TermsIdx.get(t1));
        switchState(this.term2TermsIdx.get(t2));
    }

    public abstract void undoProposal();

    public abstract long getNeighborhoodSize();

    /**
     * Records the current settings.
     */
    public void record()
    {
        for (int i = this.numInactiveTerms; i < this.termsArray.length; i++) {
            this.termActivationCounts[this.termPartition[i]]++;
        }

        this.numRecords++;
    }

    public ArrayList<TermID> getActiveTerms()
    {
        ArrayList<TermID> list = new ArrayList<TermID>(this.termsArray.length - this.numInactiveTerms);
        for (int i = this.numInactiveTerms; i < this.termsArray.length; i++) {
            list.add(this.termsArray[this.termPartition[i]]);
        }
        return list;
    }
}
