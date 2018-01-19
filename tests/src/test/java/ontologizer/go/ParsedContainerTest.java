package ontologizer.go;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ParsedContainerTest
{
    // reusable fields for dependent tests
    public TermContainer container;

    // internal fields
    private OBOParser oboParser;

    private Term bioproc = new Term("GO:0008150", "biological_process");

    private Term cellcomp = new Term("GO:0005575", "cellular_component");

    private Term molfunc = new Term("GO:0003674", "molecular_function");

    @Before
    public void setUp() throws Exception
    {
        this.oboParser = new OBOParser(OBOParserTest.GOtermsOBOFile);
        this.oboParser.doParse();
        this.container =
            new TermContainer(this.oboParser.getTermMap(), this.oboParser.getFormatVersion(), this.oboParser.getDate());
    }

    @Test
    public void testBasicStructure()
    {
        Assert.assertTrue(this.container.termCount() == this.oboParser.getTermMap().size());
        Assert.assertTrue(this.container.getFormatVersion().equals(this.oboParser.getFormatVersion()));
        Assert.assertTrue(this.container.getDate().equals(this.oboParser.getDate()));

        Assert.assertTrue(this.container.getGOName("GO:0008150").equals("biological_process"));
        Assert.assertTrue(this.container.getGOName(this.bioproc.getID()).equals("biological_process"));
        Assert.assertTrue(this.container.getGOName("GO:0005575").equals("cellular_component"));
        Assert.assertTrue(this.container.getGOName(this.cellcomp.getID()).equals("cellular_component"));
        Assert.assertTrue(this.container.getGOName("GO:0003674").equals("molecular_function"));
        Assert.assertTrue(this.container.getGOName(this.molfunc.getID()).equals("molecular_function"));

        Assert.assertTrue(this.container.get("GO:0008150").equals(this.bioproc));
        Assert.assertTrue(this.container.get(this.bioproc.getID()).equals(this.bioproc));
        Assert.assertTrue(this.container.get("GO:0005575").equals(this.cellcomp));
        Assert.assertTrue(this.container.get(this.cellcomp.getID()).equals(this.cellcomp));
        Assert.assertTrue(this.container.get("GO:0003674").equals(this.molfunc));
        Assert.assertTrue(this.container.get(this.molfunc.getID()).equals(this.molfunc));

        Assert.assertTrue(this.container.get("GO:9999999") == null);
        Term anotherTerm = new Term("GO:9999999", "dummy");
        Assert.assertTrue(this.container.get(anotherTerm.getID()) == null);
    }
}
