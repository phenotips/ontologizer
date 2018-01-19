package ontologizer.go;

import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DescriptionParserTest
{
    private final String testDescription = "`Distention` (PATO:0001602) of the `abdomen` (FMA:9577).";

    private final String[] txts = new String[] {
    "Distention",
    " of the ",
    "abdomen",
    "."
    };

    private final String[] tids = new String[] {
    "PATO:0001602",
    null,
    "FMA:9577",
    null
    };

    @Test
    public void test()
    {
        final ArrayList<String> refList = new ArrayList<>();

        DescriptionParser.parse(this.testDescription, new DescriptionParser.IDescriptionPartCallback()
        {
            int i;

            @Override
            public boolean part(String txt, String ref)
            {
                refList.add(ref);

                assertEquals(DescriptionParserTest.this.txts[this.i], txt);
                assertEquals(DescriptionParserTest.this.tids[this.i], ref);

                this.i++;
                return true;
            }
        });
        assertEquals(4, refList.size());
    }

    @Test
    public void test2()
    {
        final ArrayList<String> refList = new ArrayList<>();
        DescriptionParser.parse("Single Line", new DescriptionParser.IDescriptionPartCallback()
        {
            @Override
            public boolean part(String txt, String ref)
            {
                refList.add(ref);

                assertEquals("Single Line", txt);
                assertNull(ref);

                return true;
            }
        });
        assertEquals(1, refList.size());
    }
}
