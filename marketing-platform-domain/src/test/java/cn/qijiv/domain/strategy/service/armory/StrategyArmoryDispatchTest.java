package cn.qijiv.domain.strategy.service.armory;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StrategyArmoryDispatchTest {

    private static final String RULE_WEIGHT_VALUE =
            "4000:102,103,104,105 "
                    + "5000:102,103,104,105,106,107 "
                    + "6000:102,103,104,105,106,107,108,109";

    @Test
    public void assembleLotteryStrategy_buildsBaseAndWeightRateTables() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(awards());
        StrategyArmoryDispatch armoryDispatch = new StrategyArmoryDispatch(repository);

        assertTrue(armoryDispatch.assembleLotteryStrategy(100001L));

        assertTrue(repository.rateTables.containsKey("100001"));
        assertEquals(10_000, repository.rateTables.get("100001").size());
        assertTableContainsOnly(repository, "100001_4000:102,103,104,105", 102, 103, 104, 105);
        assertTableContainsOnly(repository, "100001_5000:102,103,104,105,106,107",
                102, 103, 104, 105, 106, 107);
        assertTableContainsOnly(repository, "100001_6000:102,103,104,105,106,107,108,109",
                102, 103, 104, 105, 106, 107, 108, 109);
        assertEquals(6, repository.rateTables.get("100001_4000:102,103,104,105").size());
        assertEquals(69, repository.rateTables.get("100001_5000:102,103,104,105,106,107").size());
        assertEquals(7_000,
                repository.rateTables.get("100001_6000:102,103,104,105,106,107,108,109").size());
    }

    @Test
    public void getRandomAwardId_ruleWeightValue_returnsAllowedAward() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(awards());
        StrategyArmoryDispatch armoryDispatch = new StrategyArmoryDispatch(repository);
        armoryDispatch.assembleLotteryStrategy(100001L);

        Integer awardId = armoryDispatch.getRandomAwardId(100001L, "4000:102,103,104,105");

        assertTrue(Arrays.asList(102, 103, 104, 105).contains(awardId));
    }

    @Test
    public void getRandomAwardId_missingBaseTable_assemblesOnDemand() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(awards());
        StrategyArmoryDispatch armoryDispatch = new StrategyArmoryDispatch(repository);

        Integer awardId = armoryDispatch.getRandomAwardId(100001L);

        assertTrue(repository.rateTables.containsKey("100001"));
        assertTrue(Arrays.asList(101, 102, 103, 104, 105, 106, 107, 108, 109).contains(awardId));
    }

    @Test
    public void getRandomAwardId_newWeightConfiguration_reassemblesMissingTable() {
        RecordingStrategyRepository repository = new RecordingStrategyRepository(awards());
        StrategyArmoryDispatch armoryDispatch = new StrategyArmoryDispatch(repository);
        armoryDispatch.assembleLotteryStrategy(100001L);
        repository.ruleWeightValue = "7000:108,109";

        Integer awardId = armoryDispatch.getRandomAwardId(100001L, "7000:108,109");

        assertTableContainsOnly(repository, "100001_7000:108,109", 108, 109);
        assertTrue(Arrays.asList(108, 109).contains(awardId));
    }

    private static void assertTableContainsOnly(
            RecordingStrategyRepository repository,
            String key,
            Integer... expectedAwardIds) {
        List<Integer> rateTable = repository.rateTables.get(key);
        assertTrue(rateTable != null && !rateTable.isEmpty());
        assertEquals(new HashSet<>(Arrays.asList(expectedAwardIds)), new HashSet<>(rateTable));
    }

    private static List<StrategyAwardEntity> awards() {
        return Arrays.asList(
                award(101, "0.3000"),
                award(102, "0.2000"),
                award(103, "0.2000"),
                award(104, "0.1000"),
                award(105, "0.1000"),
                award(106, "0.0500"),
                award(107, "0.0400"),
                award(108, "0.0099"),
                award(109, "0.0001"));
    }

    private static StrategyAwardEntity award(int awardId, String awardRate) {
        return StrategyAwardEntity.builder()
                .strategyId(100001L)
                .awardId(awardId)
                .awardRate(new BigDecimal(awardRate))
                .build();
    }

    private static class RecordingStrategyRepository implements IStrategyRepository {

        private final List<StrategyAwardEntity> strategyAwards;
        private final Map<String, List<Integer>> rateTables = new HashMap<>();
        private String ruleWeightValue = RULE_WEIGHT_VALUE;

        private RecordingStrategyRepository(List<StrategyAwardEntity> strategyAwards) {
            this.strategyAwards = strategyAwards;
        }

        @Override
        public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
            return strategyAwards;
        }

        @Override
        public void storeStrategyRateTable(String strategyId, List<Integer> rateTable) {
            rateTables.put(strategyId, new ArrayList<>(rateTable));
        }

        @Override
        public Integer queryStrategyRateTableSize(String strategyKey) {
            List<Integer> rateTable = rateTables.get(strategyKey);
            return rateTable == null ? null : rateTable.size();
        }

        @Override
        public Integer queryStrategyAwardId(String strategyKey, Integer randomValue) {
            List<Integer> rateTable = rateTables.get(strategyKey);
            return rateTable == null ? null : rateTable.get(randomValue);
        }

        @Override
        public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
            return StrategyEntity.builder()
                    .strategyId(strategyId)
                    .strategyDesc("抽奖策略")
                    .ruleModels("rule_weight,rule_blacklist")
                    .build();
        }

        @Override
        public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
            return StrategyRuleEntity.builder()
                    .strategyId(strategyId)
                    .ruleType(1)
                    .ruleModel(ruleModel)
                    .ruleValue(ruleWeightValue)
                    .ruleDesc("积分权重抽奖范围")
                    .build();
        }

        @Override
        public StrategyRuleEntity queryStrategyAwardRule(
                Long strategyId, Integer awardId, String ruleModel) {
            return null;
        }

        @Override
        public cn.qijiv.domain.strategy.model.valobj.StrategyAwardRuleModelVO
        queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId) {
            return null;
        }

        @Override
        public cn.qijiv.domain.strategy.model.valobj.RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
            return null;
        }

        @Override
        public boolean subtractionAwardStock(Long strategyId, Integer awardId) {
            return true;
        }
    }
}
