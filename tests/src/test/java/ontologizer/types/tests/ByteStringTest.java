package ontologizer.types.tests;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.types.ByteString;

public class ByteStringTest
{
    @Test(expected = NumberFormatException.class)
    public void testParseInteger()
    {
        Assert.assertEquals(1234, ByteString.parseFirstInt(new ByteString("1234")));
        Assert.assertEquals(1234, ByteString.parseFirstInt(new ByteString("001234")));
        Assert.assertEquals(4500, ByteString.parseFirstInt(new ByteString("4500")));
        Assert.assertEquals(4500, ByteString.parseFirstInt(new ByteString("0000000004500")));
        Assert.assertEquals(1234, ByteString.parseFirstInt(new ByteString("ssss1234ssss")));
        Assert.assertEquals(1234, ByteString.parseFirstInt(new ByteString("ssss001234ssss")));

        ByteString.parseFirstInt(new ByteString("sswwscs"));
        Assert.assertTrue(false);
    }

    @Test
    public void testByteParseInteger()
    {
        byte[] buf = "xx1234xx".getBytes();
        Assert.assertEquals(1234, ByteString.parseFirstInt(buf, 0, buf.length));
        Assert.assertEquals(123, ByteString.parseFirstInt(buf, 0, 5));
        Assert.assertEquals(23, ByteString.parseFirstInt(buf, 3, 2));
    }

    @Test
    public void testSubstring()
    {
        Assert.assertEquals("TEst", new ByteString("TestTEstTest").substring(4, 8).toString());
    }

    @Test
    public void testSplit()
    {
        ByteString[] split = new ByteString("str1|str2|str3").splitBySingleChar('|');
        Assert.assertEquals(3, split.length);
        Assert.assertEquals("str1", split[0].toString());
        Assert.assertEquals("str2", split[1].toString());
        Assert.assertEquals("str3", split[2].toString());

        split = new ByteString("str1|str2|str3|").splitBySingleChar('|');
        Assert.assertEquals(4, split.length);
        Assert.assertEquals("str1", split[0].toString());
        Assert.assertEquals("str2", split[1].toString());
        Assert.assertEquals("str3", split[2].toString());
        Assert.assertEquals("", split[3].toString());

        split = new ByteString("str1").splitBySingleChar('|');
        Assert.assertEquals(1, split.length);
        Assert.assertEquals("str1", split[0].toString());

        split = new ByteString("str1||str3").splitBySingleChar('|');
        Assert.assertEquals(3, split.length);
        Assert.assertEquals("str1", split[0].toString());
        Assert.assertEquals("", split[1].toString());
        Assert.assertEquals("str3", split[2].toString());
    }
}
