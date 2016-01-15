package ontologizer.calculation.b2g;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * Score of a setting in which alpha and beta are known.
 *
 * @author Sebastian Bauer
 */
class VariableAlphaBetaScore extends Bayes2GOScore
{
    private HashMap<ByteString, Double> llr = new HashMap<ByteString, Double>();

    private double alpha;

    private double beta;

    private double score;

    public VariableAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator,
        Set<ByteString> observedActiveGenes, double alpha, double beta)
    {
        super(rnd, termList, populationEnumerator, observedActiveGenes);

        this.alpha = alpha;
        this.beta = beta;

        calcLLR();
    }

    public void calcLLR()
    {
        for (ByteString g : this.population) {
            int gid = this.gene2GenesIdx.get(g);
            if (this.observedGenes[gid]) {
                this.llr.put(g, Math.log(1 - this.beta) - Math.log(this.alpha)); // P(oi=1|h=1) / P(oi=1|h=0)
            } else {
                this.llr.put(g, Math.log(this.beta) - Math.log(1 - this.alpha)); // P(oi=0|h=1) / P(oi=0|h=0)
            }
        }

    }

    private int proposalSwitch;

    private TermID proposalT1;

    private TermID proposalT2;

    @Override
    public void hiddenGeneActivated(int gid)
    {
        ByteString gene = this.genes[gid];
        this.score += this.llr.get(gene);
    }

    @Override
    public void hiddenGeneDeactivated(int gid)
    {
        ByteString gene = this.genes[gid];
        this.score -= this.llr.get(gene);
    }

    @Override
    public void proposeNewState(long rand)
    {
        long oldPossibilities = getNeighborhoodSize();

        this.proposalSwitch = -1;
        this.proposalT1 = null;
        this.proposalT2 = null;

        long choose = Math.abs(rand) % oldPossibilities;

        if (choose < this.termsArray.length) {
            /* on/off */
            this.proposalSwitch = (int) choose;
            switchState(this.proposalSwitch);
        } else {
            long base = choose - this.termsArray.length;

            int activeTermPos = (int) (base / this.numInactiveTerms);
            int inactiveTermPos = (int) (base % this.numInactiveTerms);

            this.proposalT1 = this.termsArray[this.termPartition[activeTermPos + this.numInactiveTerms]];
            this.proposalT2 = this.termsArray[this.termPartition[inactiveTermPos]];

            exchange(this.proposalT1, this.proposalT2);
        }
    }

    @Override
    public double getScore()
    {
        return this.score + (this.termsArray.length - this.numInactiveTerms) * Math.log(this.p / (1.0 - this.p));
    }

    @Override
    public void undoProposal()
    {
        if (this.proposalSwitch != -1) {
            switchState(this.proposalSwitch);
        } else {
            exchange(this.proposalT2, this.proposalT1);
        }
    }

    @Override
    public long getNeighborhoodSize()
    {
        return this.termsArray.length + (this.termsArray.length - this.numInactiveTerms) * this.numInactiveTerms;
    }

}
