package sonumina.collections;

import org.junit.Assert;
import org.junit.Test;

public class ReferencePoolTest
{
    @Test
    public void testWithInteger()
    {
        ReferencePool<Integer> integerPool = new ReferencePool<>();
        Integer ref = integerPool.map(new Integer(10));
        Integer n = new Integer(10);
        Assert.assertNotSame(ref, n);
        Assert.assertSame(ref, integerPool.map(n));
    }
}
