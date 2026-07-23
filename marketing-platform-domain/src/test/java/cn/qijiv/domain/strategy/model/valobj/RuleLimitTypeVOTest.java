package cn.qijiv.domain.strategy.model.valobj;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuleLimitTypeVOTest {

    @Test
    public void matches_supportedRelations_returnsExpectedResult() {
        assertTrue(RuleLimitTypeVO.EQUAL.matches("0001", "0001"));
        assertTrue(RuleLimitTypeVO.GT.matches("10", "9"));
        assertTrue(RuleLimitTypeVO.LE.matches("10", "10"));
        assertTrue(RuleLimitTypeVO.ENUM.matches("0001", "0000, 0001"));
        assertFalse(RuleLimitTypeVO.LT.matches("10", "9"));
    }
}
