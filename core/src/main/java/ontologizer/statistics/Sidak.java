package ontologizer.statistics;

/**
 * @author Sebastian Bauer
 */
public class Sidak extends AbstractTestCorrection
{
    /** The name of the correction method */
    private static final String NAME = "Sidak";

    @Override
    public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
    {
        PValue[] p = pValueCalculation.calculateRawPValues();
        int pvalsCount = countRelevantPValues(p);

        /* Adjust the values */
        for (int i = 0; i < p.length; i++)
        {
            if (!p[i].ignoreAtMTC) {
                p[i].p_adjusted = 1 - Math.pow(1.0 - p[i].p, pvalsCount);
            }
        }
        return p;
    }

    @Override
    public String getDescription()
    {
        return "";
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
