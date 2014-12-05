package sonumina.math.changelog;

import org.junit.Assert;
import org.junit.Test;

import sonumina.util.changelog.BuildChangeLog;
import sonumina.util.changelog.Change;

public class BuildChangeLogTest
{
    @Test
    public void testBasicLog()
    {
        Change[] result = BuildChangeLog.process(this.getClass().getResourceAsStream("/sample-changelog.xml"));
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("Test1", result[0].logString);
        Assert.assertEquals("Sebastian Bauer", result[0].authorString);
        Assert.assertEquals("2012-07-10", result[0].dateString);
        Assert.assertEquals("Test2", result[1].logString);
        Assert.assertEquals("Sebastian Bauer", result[1].authorString);
        Assert.assertEquals("2012-05-11", result[1].dateString);
    }
}
