package ontologizer.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A class providing memory efficient storage of PValue arrays. The idea is to deflate the arrays by storing only the
 * pvalues which are not marked by "ignoreAtMTC". The PValue sets to be stored are assumed to all have the same size
 * which has to be set at creation. Furthermore, apart from the "ignoreAtMTC" attributes and the "p" value itself,
 * nothing else gets stored. Especially, the "p_min" and "p_adjusted" entries get lost.
 *
 * @author grossman
 */
public class PvalueSetStore implements Iterable<PValue[]>
{

    /**
     * The iterator to conveniently hide the inflation process.
     *
     * @author grossman
     */
    public class PValueSetStoreIterator implements Iterator<PValue[]>
    {
        private Iterator<HashMap<Integer, Double>> reducedIterator = PvalueSetStore.this.reducedStoredSets.iterator();

        @Override
        public boolean hasNext()
        {
            return this.reducedIterator.hasNext();
        }

        @Override
        public PValue[] next()
        {
            return inflate_pvals(this.reducedIterator.next());
        }

        @Override
        public void remove()
        {
            this.reducedIterator.remove();
        }

    }

    private ArrayList<HashMap<Integer, Double>> reducedStoredSets;

    private int setSize;

    /**
     * @param numberOfResamplingSteps
     * @param setSize
     */
    public PvalueSetStore(int numberOfResamplingSteps, int setSize)
    {
        this.reducedStoredSets = new ArrayList<HashMap<Integer, Double>>(numberOfResamplingSteps);
        this.setSize = setSize;
    }

    public void add(PValue[] values)
    {
        this.reducedStoredSets.add(deflate_pvals(values));
    }

    private HashMap<Integer, Double> deflate_pvals(PValue[] values)
    {
        HashMap<Integer, Double> reducedSet = new HashMap<Integer, Double>();
        for (int i = 0; i < values.length; i++)
        {
            if (!values[i].ignoreAtMTC)
            {
                reducedSet.put(i, values[i].p);
            }
        }
        return reducedSet;
    }

    private PValue[] inflate_pvals(HashMap<Integer, Double> reducedSet)
    {
        PValue[] pvals = new PValue[this.setSize];

        // initialize array
        for (int i = 0; i < this.setSize; i++)
        {
            pvals[i] = new PValue();
            pvals[i].ignoreAtMTC = true;
            pvals[i].p = 1.0;
        }

        // set old values
        for (int k : reducedSet.keySet())
        {
            pvals[k].ignoreAtMTC = false;
            pvals[k].p = reducedSet.get(k);
        }

        return pvals;
    }

    @Override
    public Iterator<PValue[]> iterator()
    {
        return new PValueSetStoreIterator();
    }

}
