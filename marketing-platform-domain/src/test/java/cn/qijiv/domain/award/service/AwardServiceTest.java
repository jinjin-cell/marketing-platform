package cn.qijiv.domain.award.service;

import cn.qijiv.domain.award.event.SendAwardMessageEvent;
import cn.qijiv.domain.award.model.entity.DistributeAwardEntity;
import cn.qijiv.domain.award.respository.IAwardRepository;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class AwardServiceTest {

    @Test
    public void missingAwardKeyRemainsPending() {
        AtomicInteger completed = new AtomicInteger();
        AwardService service = new AwardService(repository(null, completed),
                new SendAwardMessageEvent(), Collections.emptyMap());

        service.distributeAward(award());

        assertEquals(0, completed.get());
    }

    @Test
    public void unsupportedAwardImplementationRemainsPending() {
        AtomicInteger completed = new AtomicInteger();
        AwardService service = new AwardService(repository("physical_award", completed),
                new SendAwardMessageEvent(), Collections.emptyMap());

        service.distributeAward(award());

        assertEquals(0, completed.get());
    }

    private DistributeAwardEntity award() {
        return DistributeAwardEntity.builder()
                .userId("unit-user")
                .orderId("unit-order")
                .awardId(106)
                .build();
    }

    private IAwardRepository repository(String awardKey, AtomicInteger completed) {
        return (IAwardRepository) Proxy.newProxyInstance(
                IAwardRepository.class.getClassLoader(),
                new Class<?>[]{IAwardRepository.class},
                (proxy, method, args) -> {
                    if ("queryAwardKey".equals(method.getName())) {
                        return awardKey;
                    }
                    if ("completeAwardRecord".equals(method.getName())) {
                        completed.incrementAndGet();
                    }
                    return null;
                });
    }
}
