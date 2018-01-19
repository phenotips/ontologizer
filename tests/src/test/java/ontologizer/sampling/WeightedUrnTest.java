package ontologizer.sampling;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class WeightedUrnTest
{
    @Test
    public void testWeightedUrn()
    {
        HashSet<String> enumeratorSet = new HashSet<>();
        String[] enumeratorObjs = { "a", "b", "c" };
        for (String entry : enumeratorObjs) {
            enumeratorSet.add(entry);
        }

        HashSet<String> denominatorSet = new HashSet<>();
        String[] denominatorObjs = { "1", "2", "3", "4", "5" };

        for (String entry : denominatorObjs) {
            denominatorSet.add(entry);
        }

        double[] ratiosToTest = { 1.0, 2.0, 4.0, 10.0 };
        for (double ratio : ratiosToTest) {
            WeightedUrn<String> testUrn = new WeightedUrn<>(enumeratorSet, denominatorSet, ratio);
            HashSet<String> sampledSet;

            sampledSet = testUrn.sample(3);
            Assert.assertTrue(sampledSet.size() == 3);

            sampledSet = testUrn.sample(8);
            // Assert.assertTrue(sampledSet.size() == 8);
            // Assert.assertTrue(sampledSet.containsAll(enumeratorSet));
            // Assert.assertTrue(sampledSet.containsAll(denominatorSet));
        }
    }

    @Test
    public void testWeightedUrnMini()
    {
        HashSet<String> enumeratorSet = new HashSet<>();
        String[] enumeratorObjs = { "a" };
        for (String entry : enumeratorObjs) {
            enumeratorSet.add(entry);
        }

        HashSet<String> denominatorSet = new HashSet<>();

        double[] ratiosToTest = { 1.0, 2.0, 4.0, 10.0 };

        for (double ratio : ratiosToTest) {
            WeightedUrn<String> testUrn = new WeightedUrn<>(enumeratorSet, denominatorSet, ratio);
            HashSet<String> sampledSet = new HashSet<>();

            sampledSet = testUrn.sample(1);
            Assert.assertTrue(sampledSet.size() == 1);

            sampledSet = testUrn.sample(1);
            Assert.assertTrue(sampledSet.size() == 1);
            Assert.assertTrue(sampledSet.containsAll(enumeratorSet));
            Assert.assertTrue(sampledSet.containsAll(denominatorSet));
        }
    }

}
