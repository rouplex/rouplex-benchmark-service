package org.rouplex.service.benchmark;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class UtilTest {
    @Test
    public void testConvertIsoInstantToString() throws Exception {
        long nowMillisBefore = System.currentTimeMillis();
        long nowMillisAfter = Util.convertIsoInstantToMillis(Util.convertIsoInstantToString(nowMillisBefore));
        Assert.assertEquals(nowMillisBefore, nowMillisAfter);
    }
}