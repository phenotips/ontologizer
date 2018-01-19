package ontologizer.association;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.go.ParsedContainerTest;
import ontologizer.go.TermContainer;
import ontologizer.types.ByteString;

public class AssociationParserTest2
{
    public TermContainer container;

    private AssociationParser assocParser;

    public AssociationContainer assocContainer;

    // data for testing
    private String GOAssociationFile = "data/gene_association.sgd.gz";

    private int nAnnotatedGenes = 6359;

    private int nAssociations = 87599;

    private int nSynonyms = 9317;

    private int nDBObjects = 6359;

    private String[] someGenes = { "SRL1", "DDR2", "UFO1" };

    private int[] someGeneTermCounts = { 11, 4, 8 };

    @Before
    public void setUp() throws Exception
    {
        ParsedContainerTest oboPT = new ParsedContainerTest();
        oboPT.setUp();
        this.container = oboPT.container;

        this.assocParser = new AssociationParser(this.GOAssociationFile, this.container, null);

        this.assocContainer = new AssociationContainer(this.assocParser.getAssociations(),
            this.assocParser.getSynonym2gene(),
            this.assocParser.getDbObject2gene());
    }

    @Test
    public void testBasicStructure()
    {
        Assert.assertEquals("number of parsed associations", this.nAssociations,
            this.assocParser.getAssociations().size());
        Assert.assertEquals("number of parsed synonyms", this.nSynonyms, this.assocParser.getSynonym2gene().size());
        Assert.assertEquals("number of parsed DB objects", this.nDBObjects, this.assocParser.getDbObject2gene().size());
        Assert.assertEquals("number of annotated genes", this.nAnnotatedGenes,
            this.assocContainer.getAllAnnotatedGenes().size());

        for (int i = 0; i < this.someGenes.length; i++) {
            Assert.assertEquals(this.assocContainer.get(new ByteString(this.someGenes[i])).getAssociations().size(),
                this.someGeneTermCounts[i]);
        }
    }
}
