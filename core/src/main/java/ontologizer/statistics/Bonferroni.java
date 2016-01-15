/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

/**
 * This class implements the Bonferroni multiple test correction which is the most conservative approach.
 *
 * @author Sebastian Bauer
 */
public class Bonferroni extends AbstractTestCorrection
{
    /** The name of the correction method */
    private static final String NAME = "Bonferroni";

    @Override
    public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
    {
        PValue[] p = pValueCalculation.calculateRawPValues();
        int pvalsCount = countRelevantPValues(p);

        /* Adjust the values */
        for (int i = 0; i < p.length; i++) {
            if (!p[i].ignoreAtMTC) {
                p[i].p_adjusted = Math.min(1.0, p[i].p * pvalsCount);
            }
        }
        return p;
    }

    @Override
    public String getDescription()
    {
        return "The Bonferroni correction is the most conservative approach.";
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
