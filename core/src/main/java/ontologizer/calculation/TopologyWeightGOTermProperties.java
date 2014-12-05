package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;

import ontologizer.types.ByteString;

public class TopologyWeightGOTermProperties extends AbstractGOTermProperties
{
    private static final String[] propertyNames = new String[] {
        "ID", "Pop.total", "Pop.term", "Study.total", "Study.term", "Pop.family", "Study.family", "is.trivial", "p",
        "p.adjusted", "p.min"
    };

    /** Number of genes annotated to family (term and parents) in population set. */
    public int popFamilyGenes;

    /** Number of genes annotated to family (term and parents) in study set. */
    public int studyFamilyGenes;

    /** Genes which should be removed for terms in upper levels */
    public HashSet<ByteString> markedGenes;

    /** Weight of the annotated genes */
    private HashMap<ByteString, Double> weight = new HashMap<ByteString, Double>();

    @Override
    public int getNumberOfProperties()
    {
        return propertyNames.length;
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
                return Integer.toString(this.popFamilyGenes);
            case 6:
                return Integer.toString(this.studyFamilyGenes);
            case 7:
                return Boolean.toString(this.ignoreAtMTC);
            case 8:
                return Double.toString(this.p);
            case 9:
                return Double.toString(this.p_adjusted);
            case 10:
                return Double.toString(this.p_min);
        }
        return null;
    }

    @Override
    public String getPropertyName(int propNumber)
    {
        return propertyNames[propNumber];
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

    /**
     * Returns the weight of the given gene. If no weight has been set yet, returns 1.
     *
     * @param gene
     * @return
     */
    public double getWeight(ByteString gene)
    {
        Double w = this.weight.get(gene);
        if (w == null) {
            return 1;
        }
        return w;
    }

    /**
     * Sets the weight.
     *
     * @param gene
     * @param w
     */
    public void setWeight(ByteString gene, double w)
    {
        this.weight.put(gene, w);
    }

}
