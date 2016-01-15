package ontologizer.calculation.b2g;

import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.calculation.util.Gamma;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * Score of a setting in which alpha and beta are not known.
 *
 * @author Sebastian Bauer
 */
public class FixedAlphaBetaScore extends Bayes2GOScore
{
    private boolean integrateParams = false;

    private int proposalSwitch;

    private TermID proposalT1;

    private TermID proposalT2;

    protected double[] ALPHA = new double[] { 0.0000001, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55,
    0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95 };

    private int alphaIdx = 0;

    private int oldAlphaIdx;

    protected int[] totalAlpha = new int[this.ALPHA.length];

    private boolean doAlphaMCMC = true;

    protected double[] BETA = this.ALPHA;

    private int betaIdx = 0;

    private int oldBetaIdx;

    protected int totalBeta[] = new int[this.BETA.length];

    private boolean doBetaMCMC = true;

    protected final int[] EXPECTED_NUMBER_OF_TERMS = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
    17, 18, 19, 20 };

    private int expIdx = 0;

    private int oldExpIdx;

    protected int totalExp[] = new int[this.EXPECTED_NUMBER_OF_TERMS.length];

    private boolean doExpMCMC = true;

    protected double alpha = Double.NaN;

    protected double beta = Double.NaN;

    private int n00;

    private int n01;

    private int n10;

    private int n11;

    private long totalN00;

    private long totalN01;

    private long totalN10;

    private long totalN11;

    private long totalT;

    public void setAlpha(double alpha)
    {
        this.alpha = alpha;
        this.doAlphaMCMC = Double.isNaN(alpha);
    }

    public void setBeta(double beta)
    {
        this.beta = beta;
        this.doBetaMCMC = Double.isNaN(beta);
    }

    @Override
    public void setExpectedNumberOfTerms(double terms)
    {
        super.setExpectedNumberOfTerms(terms);
        this.doExpMCMC = Double.isNaN(terms);
    }

    public void setMaxAlpha(double maxAlpha)
    {
        int span;

        if (Double.isNaN(maxAlpha)) {
            maxAlpha = 1;
        }

        if (maxAlpha < 0.01) {
            maxAlpha = 0.01;
        }
        if (maxAlpha > 0.99999999) {
            span = 20;
        } else {
            span = 19;
        }

        this.ALPHA = new double[20];
        this.totalAlpha = new int[20];

        this.ALPHA[0] = 0.0000001;
        for (int i = 1; i < 20; i++) {
            this.ALPHA[i] = i * maxAlpha / span;
        }
    }

    public void setMaxBeta(double maxBeta)
    {
        int span;

        if (Double.isNaN(maxBeta)) {
            maxBeta = 1;
        }

        if (maxBeta < 0.01) {
            maxBeta = 0.01;
        }
        if (maxBeta > 0.99999999) {
            span = 20;
        } else {
            span = 19;
        }

        this.BETA = new double[20];
        this.totalBeta = new int[20];

        this.BETA[0] = 0.0000001;
        for (int i = 1; i < 20; i++) {
            this.BETA[i] = i * maxBeta / span;
        }

    }

    public void setIntegrateParams(boolean integrateParams)
    {
        this.integrateParams = integrateParams;
    }

    public FixedAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator,
        Set<ByteString> observedActiveGenes)
    {
        super(rnd, termList, populationEnumerator, observedActiveGenes);

        setMaxAlpha(1.);
        setMaxBeta(1.);

        this.n10 = observedActiveGenes.size();
        this.n00 = this.population.size() - this.n10;
    }

    @Override
    public void hiddenGeneActivated(int gid)
    {
        if (this.observedGenes[gid]) {
            this.n11++;
            this.n10--;
        } else {
            this.n01++;
            this.n00--;
        }
    }

    @Override
    public void hiddenGeneDeactivated(int gid)
    {
        if (this.observedGenes[gid]) {
            this.n11--;
            this.n10++;
        } else {
            this.n01--;
            this.n00++;
        }
    }

    @Override
    public void proposeNewState(long rand)
    {
        long oldPossibilities = getNeighborhoodSize();

        this.proposalSwitch = -1;
        this.proposalT1 = null;
        this.proposalT2 = null;
        this.oldAlphaIdx = -1;
        this.oldBetaIdx = -1;
        this.oldExpIdx = -1;

        if ((!this.doAlphaMCMC && !this.doBetaMCMC && !this.doExpMCMC) || this.rnd.nextBoolean()) {
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
        } else {
            int max = 0;

            if (this.doAlphaMCMC) {
                max += this.ALPHA.length;
            }
            if (this.doBetaMCMC) {
                max += this.BETA.length;
            }
            if (this.doExpMCMC) {
                max += this.EXPECTED_NUMBER_OF_TERMS.length;
            }

            int choose = Math.abs((int) rand) % max;

            if (this.doAlphaMCMC) {
                if (choose < this.ALPHA.length) {
                    this.oldAlphaIdx = this.alphaIdx;
                    this.alphaIdx = choose;
                    return;
                }
                choose -= this.ALPHA.length;
            }

            if (this.doBetaMCMC) {
                if (choose < this.BETA.length) {
                    this.oldBetaIdx = this.betaIdx;
                    this.betaIdx = choose;
                    return;
                }
                choose -= this.BETA.length;
            }

            if (!this.doExpMCMC) {
                throw new RuntimeException("MCMC requested proposal but no proposal is possible");
            }

            this.oldExpIdx = this.expIdx;
            this.expIdx = choose;
        }
    }

    public final double getAlpha()
    {
        double alpha;

        if (Double.isNaN(this.alpha)) {
            alpha = this.ALPHA[this.alphaIdx];
        } else {
            alpha = this.alpha;
        }

        return alpha;
    }

    public final double getBeta()
    {
        double beta;

        if (Double.isNaN(this.beta)) {
            beta = this.BETA[this.betaIdx];
        } else {
            beta = this.beta;
        }

        return beta;
    }

    public final double getP()
    {
        double p;
        if (Double.isNaN(this.p)) {
            p = (double) this.EXPECTED_NUMBER_OF_TERMS[this.expIdx] / this.termsArray.length;
        } else {
            p = this.p;
        }

        return p;
    }

    private final double[] lGamma = new double[20000];

    private double logGamma(int a)
    {
        if (a == 1 || a == 2) {
            return 0.0;
        }

        if (a < this.lGamma.length) {
            double lg = this.lGamma[a];
            if (lg == 0.0) {
                lg = Gamma.lgamma(a);
                this.lGamma[a] = lg;
            }
            return lg;
        }
        return Gamma.lgamma(a);
    }

    private double logBeta(int a, int b)
    {
        return logGamma(a) + logGamma(b) - logGamma(a + b);
    }

    @Override
    public double getScore()
    {
        double newScore2;

        if (!this.integrateParams) {
            double alpha;
            double beta;
            double p;

            alpha = getAlpha();
            beta = getBeta();
            p = getP();

            newScore2 =
                Math.log(alpha) * this.n10 + Math.log(1 - alpha) * this.n00 + Math.log(1 - beta) * this.n11
                    + Math.log(beta) * this.n01;

            if (this.usePrior) {
                newScore2 +=
                    Math.log(p) * (this.termsArray.length - this.numInactiveTerms) + Math.log(1 - p)
                        * this.numInactiveTerms;
            }
        } else {
            /* Prior */
            int alpha1 = 1; /* Psedocounts, false positive */
            int alpha2 = 1; /* Psedocounts, true negative */
            int beta1 = 1; /* Pseudocounts, false negative */
            int beta2 = 1; /* Pseudocounts, true positives */
            double pl = 0;
            double pu = 0.5;
            int p1 = 1; /* Pseudocounts, on */
            int p2 = 1; /* Pseudocounts, off */

            int m1 = this.termsArray.length - this.numInactiveTerms;
            int m0 = this.numInactiveTerms;

            double s1 = logBeta(alpha1 + this.n10, alpha2 + this.n00);
            double s2 = logBeta(beta1 + this.n01, beta2 + this.n11);
            double s3 = logBeta(p1 + m1, p2 + m0);
            newScore2 = s1 + s2 + s3;
        }

        return newScore2;
    }

    @Override
    public void undoProposal()
    {
        if (this.proposalSwitch != -1) {
            switchState(this.proposalSwitch);
        } else if (this.proposalT1 != null) {
            exchange(this.proposalT2, this.proposalT1);
        } else if (this.oldAlphaIdx != -1) {
            this.alphaIdx = this.oldAlphaIdx;
        } else if (this.oldBetaIdx != -1) {
            this.betaIdx = this.oldBetaIdx;
        } else if (this.oldExpIdx != -1) {
            this.expIdx = this.oldExpIdx;
        } else {
            throw new RuntimeException("Wanted to undo a proposal that wasn't proposed");
        }
    }

    @Override
    public long getNeighborhoodSize()
    {
        long size = this.termsArray.length + (this.termsArray.length - this.numInactiveTerms) * this.numInactiveTerms;
        return size;
    }

    @Override
    public void record()
    {
        super.record();

        this.totalN00 += this.n00;
        this.totalN01 += this.n01;
        this.totalN10 += this.n10;
        this.totalN11 += this.n11;

        this.totalAlpha[this.alphaIdx]++;
        this.totalBeta[this.betaIdx]++;
        this.totalExp[this.expIdx]++;
        this.totalT += (this.termsArray.length - this.numInactiveTerms);
    }

    public double getAvgN00()
    {
        return (double) this.totalN00 / this.numRecords;
    }

    public double getAvgN01()
    {
        return (double) this.totalN01 / this.numRecords;
    }

    public double getAvgN10()
    {
        return (double) this.totalN10 / this.numRecords;
    }

    public double getAvgN11()
    {
        return (double) this.totalN11 / this.numRecords;
    }

    public double getAvgT()
    {
        return (double) this.totalT / this.numRecords;
    }

    /**
     * Returns possible alpha values.
     *
     * @return
     */
    public double[] getAlphaValues()
    {
        return this.ALPHA;
    }

    /**
     * Returns the distribution of the given counts.
     *
     * @param counts
     * @return
     */
    private double[] getDistribution(int[] counts)
    {
        double[] dist = new double[counts.length];
        int total = 0;
        for (int a : counts) {
            total += a;
        }
        for (int i = 0; i < counts.length; i++) {
            dist[i] = counts[i] / (double) total;
        }
        return dist;
    }

    /**
     * Returns the inferred alpha distribution.
     *
     * @return
     */
    public double[] getAlphaDistribution()
    {
        return getDistribution(this.totalAlpha);
    }

    /**
     * Returns possible alpha values.
     *
     * @return
     */
    public double[] getBetaValues()
    {
        return this.BETA;
    }

    /**
     * Returns the inferred alpha distribution.
     *
     * @return
     */
    public double[] getBetaDistribution()
    {
        return getDistribution(this.totalBeta);
    }

}
