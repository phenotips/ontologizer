package ontologizer.go;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.go.Ontology.IVisitingGOVertex;

public class OntologyTest
{
    private TermContainer goTerms;

    private Ontology graph;

    @Before
    public void setUp() throws Exception
    {
        String GOtermsOBOFile = "data/gene_ontology.1_2.obo.gz";

        /* Parse file and create term container */
        OBOParser oboParser = new OBOParser(GOtermsOBOFile);
        oboParser.doParse();
        this.goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());

        /* Build graph */
        this.graph = new Ontology(this.goTerms);
    }

    @Test
    public void testRoot()
    {
        Set<String> terms;

        terms = this.graph.getTermChildrenAsStrings("GO:0000000");
        Assert.assertTrue("Test we get some Set<String> object back", terms != null);
        Assert.assertEquals("Root has three descendants", 3, terms.size());

        terms = this.graph.getTermParentsAsStrings("GO:0000000");
        Assert.assertTrue("Test we gat some Set<String> object back", terms != null);
        Assert.assertTrue("Root has no ancestors", terms.size() == 0);
    }

    @Test
    public void testExistsPath()
    {
        Assert.assertTrue(this.graph.existsPath(new TermID("GO:0009987"),
            new TermID("GO:0006281")));
        Assert.assertFalse(this.graph.existsPath(new TermID("GO:0006281"),
            new TermID("GO:0009987")));

        Assert.assertTrue(this.graph.existsPath(new TermID("GO:0008150"),
            new TermID("GO:0006281")));
        Assert.assertFalse(this.graph.existsPath(new TermID("GO:0006281"),
            new TermID("GO:0008150")));

        Assert.assertFalse(this.graph.existsPath(new TermID("GO:0006139"),
            new TermID("GO:0009719")));
        Assert.assertFalse(this.graph.existsPath(new TermID("GO:0009719"),
            new TermID("GO:0006139")));
    }

    @Test
    public void testWalkToRoot()
    {
        /**
         * A basic visitor: It simply counts up the number of visisted terms.
         *
         * @author Sebastian Bauer
         */
        class VisitingGOVertex implements IVisitingGOVertex
        {
            public int count = 0;

            @Override
            public boolean visited(Term term)
            {
                this.count++;
                System.out.println(term + " " + this.count);
                return true;
            }

            public void resetCount()
            {
                this.count = 0;
            };

            public int getCount()
            {
                return this.count;
            };
        }

        VisitingGOVertex vistingGOVertex = new VisitingGOVertex();

        /*
         * Note, if GO changes these values are no longer correct. But you can verify them then via www.godatabase.org.
         */
        this.graph.walkToSource(new TermID("GO:0008152"), vistingGOVertex);
        Assert.assertEquals(3, vistingGOVertex.getCount());
        vistingGOVertex.resetCount();

        this.graph.walkToSource(new TermID("GO:0044237"), vistingGOVertex);
        Assert.assertEquals(5, vistingGOVertex.getCount());
        vistingGOVertex.resetCount();

        this.graph.walkToSource(new TermID("GO:0006281"), vistingGOVertex);
        Assert.assertEquals(19, vistingGOVertex.getCount());
    }
}
