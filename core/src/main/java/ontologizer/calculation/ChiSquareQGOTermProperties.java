/*
 * Created on 03.09.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

public class ChiSquareQGOTermProperties extends AbstractGOTermProperties
{
    /** chisquare value */
    public double chisquare;

    /** Used for the permuation */
    public int expectedHigherChisquaresCount;

    /** expected number of higher chisquares */
    public double expectedHigherChisquares;

    /** number of higher chisquares in the current observation */
    public int observedHigherChisquares;

    private static final String[] propertyNames = new String[] {
        "ID",
        "Pop.total",
        "Pop.term",
        "Study.total",
        "Study.term",
        "chisquare",
        "expected.higher",
        "observed.higher",
        "p",
        "q"
    };

    @Override
    public int getNumberOfProperties()
    {
        return propertyNames.length;
    }

    @Override
    public String getPropertyName(int propNumber)
    {
        return propertyNames[propNumber];
    }

    @Override
    public String getProperty(int propNumber)
    {
        switch (propNumber)
        {
            case 0:
                return this.goTerm.getIDAsString();
            case 1:
                return null; /* population gene count */
            case 2:
                return Integer.toString(this.annotatedPopulationGenes);
            case 3:
                return null; /* study gene count */
            case 4:
                return Integer.toString(this.annotatedStudyGenes);
            case 5:
                return Double.toString(this.chisquare);
            case 6:
                return Double.toString(this.expectedHigherChisquares);
            case 7:
                return Integer.toString(this.observedHigherChisquares);
            case 8:
                return Double.toString(this.p);
            case 9:
                return Double.toString(this.p_adjusted);
        }
        return null;
    }

    @Override
    public boolean isPropertyPopulationGeneCount(int propNumber)
    {
        return propNumber == 1;
    }

    @Override
    public boolean isPropertyStudyGeneCount(int propNumber)
    {
        return propNumber == 3;
    }

}
