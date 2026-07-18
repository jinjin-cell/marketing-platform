package cn.qijiv.domain.strategy.service.armory;

import cn.qijiv.domain.strategy.model.StrategyAwardEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class StrategyArmoryTest {

    @Test
    public void assembleLotteryStrategy_percentRates_buildsCompleteRateTable() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(Arrays.asList(
                award(101, "80.0000"),
                award(102, "10.0000"),
                award(103, "5.0000"),
                award(104, "4.0000"),
                award(105, "0.6000"),
                award(106, "0.2000"),
                award(107, "0.2000"),
                award(108, "0.1999"),
                award(109, "0.0001")
        ));

        new StrategyArmory(repository).assembleLotteryStrategy(100001L);

        assertEquals(1_002_000, repository.rateTable.size());
        assertEquals(800_000, countAwards(repository.rateTable, 101));
        assertEquals(1, countAwards(repository.rateTable, 109));
    }

    @Test
    public void getRandomAwardId_rateRangeOne_returnsStoredAward() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(
                Arrays.asList(award(101, "100.0000")));
        StrategyArmory armory = new StrategyArmory(repository);

        armory.assembleLotteryStrategy(100001L);

        assertEquals(Integer.valueOf(101), armory.getRandomAwardId(100001L));
    }

    private static StrategyAwardEntity award(int awardId, String rate) {
        return StrategyAwardEntity.builder()
                .strategyId(100001L)
                .awardId(awardId)
                .awardRate(new BigDecimal(rate))
                .build();
    }

    private static int countAwards(List<Integer> rateTable, int awardId) {
        int count = 0;
        for (Integer value : rateTable) {
            if (value == awardId) {
                count++;
            }
        }
        return count;
    }

    private static class RecordingStrategyRepository implements IStrategyRepository {

        private final List<StrategyAwardEntity> strategyAwards;
        private List<Integer> rateTable = new ArrayList<>();

        private RecordingStrategyRepository(List<StrategyAwardEntity> strategyAwards) {
            this.strategyAwards = strategyAwards;
        }

        @Override
        public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
            return strategyAwards;
        }

        @Override
        public void storeStrategyRateTable(Long strategyId, List<Integer> rateTable) {
            this.rateTable = rateTable;
        }

        @Override
        public Integer queryStrategyRateTableSize(Long strategyId) {
            return rateTable.isEmpty() ? null : rateTable.size();
        }

        @Override
        public Integer queryStrategyAwardId(Long strategyId, Integer randomValue) {
            return rateTable.get(randomValue);
        }
    }

}
