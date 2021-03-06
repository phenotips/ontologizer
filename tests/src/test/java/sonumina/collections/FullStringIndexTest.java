package sonumina.collections;

import org.junit.Assert;
import org.junit.Test;

public class FullStringIndexTest
{
    @SuppressWarnings("unused")
    @Test
    public void test1()
    {
        FullStringIndex<Integer> fsi = new FullStringIndex<>();
        fsi.add("ABCD", 1);
        fsi.add("GDUJ", 2);
        fsi.add("GAQM", 3);
        fsi.add("GDUJK", 4);
        fsi.add("ABCD", 5);
        Assert.assertEquals(5, fsi.size());

        int cnt = 0;
        for (Integer i : fsi.contains("A")) {
            cnt++;
        }
        Assert.assertEquals(3, cnt);

        cnt = 0;
        for (Integer i : fsi.contains("AB")) {
            cnt++;
        }
        Assert.assertEquals(2, cnt);

        cnt = 0;
        for (Integer i : fsi.contains("AQ")) {
            cnt++;
        }
        Assert.assertEquals(1, cnt);

        cnt = 0;
        for (Integer i : fsi.contains("Z")) {
            cnt++;
        }
        Assert.assertEquals(0, cnt);

        fsi.clear();
        cnt = 0;
        for (Integer i : fsi.contains("A")) {
            cnt++;
        }
        Assert.assertEquals(0, cnt);
    }
}
