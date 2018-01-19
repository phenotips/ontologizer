package ontologizer.go;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TermContainerTest
{
    private TermContainer container;

    private Term root;

    private Term bioproc;

    private Term cellcomp;

    private Term molfunc;

    @Before
    public void setUp() throws Exception
    {
        this.root = new Term("GO:0000000", "root", (ParentTermID[]) null);
        ArrayList<ParentTermID> rootlist = new ArrayList<>();
        rootlist.add(new ParentTermID(this.root.getID(), TermRelation.PART_OF_A));
        this.bioproc = new Term("GO:0008150", "biological process", new Namespace("B"), rootlist);
        this.cellcomp = new Term("GO:0005575", "cellular component", new Namespace("C"), rootlist);
        this.molfunc = new Term("GO:0003674", "molecular function", new Namespace("F"), rootlist);

        HashSet<Term> termsConstructed = new HashSet<>();
        termsConstructed.add(this.root);
        termsConstructed.add(this.bioproc);
        termsConstructed.add(this.cellcomp);
        termsConstructed.add(this.molfunc);

        this.container = new TermContainer(termsConstructed, "noformat", "nodate");
    }

    @Test
    public void testBasicStructure()
    {

        Assert.assertTrue(this.container.termCount() == 4);
        Assert.assertTrue(this.container.getFormatVersion().equals("noformat"));
        Assert.assertTrue(this.container.getDate().equals("nodate"));

        Assert.assertTrue(this.container.getGOName("GO:0000000").equals("root"));
        Assert.assertTrue(this.container.getGOName(this.root.getID()).equals("root"));
        Assert.assertTrue(this.container.getGOName("GO:0008150").equals("biological process"));
        Assert.assertTrue(this.container.getGOName(this.bioproc.getID()).equals("biological process"));
        Assert.assertTrue(this.container.getGOName("GO:0005575").equals("cellular component"));
        Assert.assertTrue(this.container.getGOName(this.cellcomp.getID()).equals("cellular component"));
        Assert.assertTrue(this.container.getGOName("GO:0003674").equals("molecular function"));
        Assert.assertTrue(this.container.getGOName(this.molfunc.getID()).equals("molecular function"));

        Assert.assertTrue(this.container.get("GO:0000000").equals(this.root));
        Assert.assertTrue(this.container.get(this.root.getID()).equals(this.root));
        Assert.assertTrue(this.container.get("GO:0008150").equals(this.bioproc));
        Assert.assertTrue(this.container.get(this.bioproc.getID()).equals(this.bioproc));
        Assert.assertTrue(this.container.get("GO:0005575").equals(this.cellcomp));
        Assert.assertTrue(this.container.get(this.cellcomp.getID()).equals(this.cellcomp));
        Assert.assertTrue(this.container.get("GO:0003674").equals(this.molfunc));
        Assert.assertTrue(this.container.get(this.molfunc.getID()).equals(this.molfunc));

        Assert.assertTrue(this.container.get("GO:0000815") == null);
        Term anotherTerm = new Term("GO:0000815", "dummy", (ParentTermID[]) null);
        Assert.assertTrue(this.container.get(anotherTerm.getID()) == null);
    }
}
