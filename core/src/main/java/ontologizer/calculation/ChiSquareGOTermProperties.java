package ontologizer.calculation;

public class ChiSquareGOTermProperties extends AbstractGOTermProperties
{
    /** chisquare value */
    public double chisquare;

    /** number of children */
    public int nchildren;

    /** degrees of freedom for chisquare */
    public int df;

    private static final String[] propertyNames = new String[] {
        "ID", "Pop.total", "Pop.term", "Study.total", "Study.term", "nchildren", "df", "chisquare", "p"
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
                return Integer.toString(this.nchildren);
            case 6:
                return Integer.toString(this.df);
            case 7:
                return Double.toString(this.chisquare);
            case 8:
                return Double.toString(this.p);
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
