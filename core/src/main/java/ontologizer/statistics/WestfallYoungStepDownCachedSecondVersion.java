package ontologizer.statistics;

import java.util.Arrays;
import java.util.HashMap;

public class WestfallYoungStepDownCachedSecondVersion extends AbstractTestCorrection
    implements IResampling
{
    /** Specifies the number of resampling steps */
    private int numberOfResamplingSteps = 1000;

    private HashMap<Integer, PvalueSetStoreSecondVersion> sampledPValuesPerSize =
        new HashMap<Integer, PvalueSetStoreSecondVersion>();

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName()
    {
        return "Westfall-Young-Step-Down-Cached-Second-Version";
    }

    /**
     * @author Sebastian Bauer Class models a double value entry and its index of a source array.
     */
    private class Entry implements Comparable<Entry>
    {
        public double value;

        public int index;

        @Override
        public int compareTo(Entry o)
        {
            if (this.value < o.value) {
                return -1;
            }
            if (this.value == o.value) {
                return 0;
            }
            return 1;
        }
    };

    @Override
    public PValue[] adjustPValues(IPValueCalculation pvalueCalc)
    {
        int i;

        /* Calculate raw P-values */
        PValue[] rawP = pvalueCalc.calculateRawPValues();

        double[] q = new double[rawP.length];
        int[] count = new int[rawP.length];

        /* Sort the raw P-values and remember their original index */
        int m = rawP.length;
        int r[] = new int[m];
        Entry[] sortedRawPValues = new Entry[m];

        for (i = 0; i < m; i++) {
            sortedRawPValues[i] = new Entry();
            sortedRawPValues[i].value = rawP[i].p;
            sortedRawPValues[i].index = i;
        }
        Arrays.sort(sortedRawPValues);

        /* build up r, this info is redundant but using r is more convenient */
        for (i = 0; i < m; i++) {
            r[i] = sortedRawPValues[i].index;
        }

        int studySetSize = pvalueCalc.currentStudySetSize();

        /* holds the sampled random p values for the current study set size */
        PvalueSetStoreSecondVersion randomSampledPValues;

        if (this.sampledPValuesPerSize.containsKey(studySetSize)) {
            System.out.println("Using available samples for study set size " + studySetSize);
            randomSampledPValues = this.sampledPValuesPerSize.get(studySetSize);
        } else {
            System.out.println("Sampling for study set size " + studySetSize + "\nThis may take a while...");
            randomSampledPValues = new PvalueSetStoreSecondVersion(this.numberOfResamplingSteps, m);
            for (int b = 0; b < this.numberOfResamplingSteps; b++) {
                /* Compute raw p values of "permuted" data */
                randomSampledPValues.add(pvalueCalc.calculateRandomPValues());

                System.out.print("created " + b + " samples out of " + this.numberOfResamplingSteps + "\r");
            }
            this.sampledPValuesPerSize.put(studySetSize, randomSampledPValues);
        }

        /* Now "permute" */
        for (PValue[] randomRawP : randomSampledPValues) {
            /* Compute raw p values of "permuted" data */
            // PValue [] randomRawP = randomSampledPValues[b];

            /* Compute the successive minima of raw p values */
            q[m - 1] = randomRawP[r[m - 1]].p;
            for (i = m - 2; i >= 0; i--) {
                q[i] = Math.min(q[i + 1], randomRawP[r[i]].p);
            }

            /* Count up */
            for (i = 0; i < m; i++) {
                if (q[i] <= rawP[r[i]].p) {
                    count[i]++;
                }
            }
        }

        /* Enforce monotony contraints */
        int c = count[0];
        for (i = 1; i < m; i++) {
            c = count[i] = Math.max(1, Math.max(c, count[i]));
        }

        /* Calculate the adjusted p values */
        for (i = 0; i < m; i++) {
            rawP[r[i]].p_adjusted = ((double) count[i]) / this.numberOfResamplingSteps;
        }
        return rawP;
    }

    @Override
    public void setNumberOfResamplingSteps(int n)
    {
        if (n != this.numberOfResamplingSteps) {
            this.numberOfResamplingSteps = n;

            /* Clear the cache */
            this.sampledPValuesPerSize = new HashMap<Integer, PvalueSetStoreSecondVersion>();
        }
    }

    @Override
    public int getNumberOfResamplingSteps()
    {
        return this.numberOfResamplingSteps;
    }

    @Override
    public void resetCache()
    {
        this.sampledPValuesPerSize = new HashMap<Integer, PvalueSetStoreSecondVersion>();
    }

    @Override
    public int getSizeTolerance()
    {
        return 0;
    }

    @Override
    public void setSizeTolerance(int t)
    {

    }
}
