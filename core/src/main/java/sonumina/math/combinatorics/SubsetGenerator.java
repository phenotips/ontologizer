package sonumina.math.combinatorics;

/**
 * A class to generate stepwise subsets with cardinality not greater than m of the set {0,1,...,n-1}. Note that an empty
 * subset is generated as well.
 *
 * @author sba
 */
public class SubsetGenerator
{
    static public class Subset
    {
        /** Subset */
        public int[] j;

        /** Size of the subset */
        public int r;
    }

    private Subset subset;

    private int n;

    private int m;

    /** Indicates whether first subset has already been generated */
    private boolean firstSubset;

    /**
     * Constructor.
     *
     * @param n defines size of the set
     * @param m defines the maximum cardinality of the generated subsets.
     */
    public SubsetGenerator(int n, int m)
    {
        this.n = n;
        this.m = m;
        this.firstSubset = true;
        this.subset = new Subset();
    }

    /**
     * Returns the next subset or null if all subsets have already been created. Note that the returned array is read
     * only!
     *
     * @return
     */
    public Subset next()
    {
        if (this.subset.r == 0)
        {
            if (this.firstSubset)
            {
                this.firstSubset = false;
                return this.subset;
            }

            /* Special case when subset of an empty set should be generated */
            if (this.n == 0)
            {
                this.firstSubset = true;
                return null;
            }

            /* First call of next inside a subset generating phase */
            this.subset.j = new int[this.m];
            this.subset.r = 1;
            return this.subset;
        }

        int[] j = this.subset.j;
        int r = this.subset.r;

        if (j[r - 1] < this.n - 1 && r < this.m)
        {
            /* extend */
            j[r] = j[r - 1] + 1;
            r++;
        } else
        {
            /* modified reduce */
            if (j[r - 1] >= this.n - 1) {
                r--;
            }

            if (r == 0)
            {
                this.subset.r = 0;
                this.firstSubset = true;
                return null;
            }
            j[r - 1] = j[r - 1] + 1;
        }

        this.subset.r = r;
        return this.subset;
    }
}
