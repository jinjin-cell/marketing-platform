package cn.qijiv.domain.activity.service.quota;

import cn.qijiv.domain.activity.model.entity.ActivityAccountDayEntity;
import cn.qijiv.domain.activity.model.entity.ActivityAccountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityAccountMonthEntity;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class RaffleActivityAccountQuotaQueryTest {

    @Test
    public void queryUsesCurrentMonthAndDayDetailSurplus() {
        ActivityAccountEntity main = ActivityAccountEntity.builder()
                .totalCount(20).totalCountSurplus(12)
                .monthCount(10).monthCountSurplus(1)
                .dayCount(5).dayCountSurplus(1)
                .build();
        ActivityAccountMonthEntity month = ActivityAccountMonthEntity.builder()
                .monthCount(10).monthCountSurplus(7).build();
        ActivityAccountDayEntity day = ActivityAccountDayEntity.builder()
                .dayCount(5).dayCountSurplus(4).build();
        RaffleActivityAccountQuotaService service = service(repository(main, month, day));

        ActivityAccountEntity result = service.queryActivityAccountEntity(100301L, "unit-user");

        assertEquals(Integer.valueOf(12), result.getTotalCountSurplus());
        assertEquals(Integer.valueOf(7), result.getMonthCountSurplus());
        assertEquals(Integer.valueOf(4), result.getDayCountSurplus());
    }

    @Test
    public void missingCurrentDetailUsesConfiguredFullQuota() {
        ActivityAccountEntity main = ActivityAccountEntity.builder()
                .totalCount(20).totalCountSurplus(12)
                .monthCount(10).monthCountSurplus(0)
                .dayCount(5).dayCountSurplus(0)
                .build();
        RaffleActivityAccountQuotaService service = service(repository(main, null, null));

        ActivityAccountEntity result = service.queryActivityAccountEntity(100301L, "unit-user");

        assertEquals(Integer.valueOf(10), result.getMonthCountSurplus());
        assertEquals(Integer.valueOf(5), result.getDayCountSurplus());
    }

    private RaffleActivityAccountQuotaService service(IActivityRepository repository) {
        return new RaffleActivityAccountQuotaService(repository, null, Collections.emptyMap());
    }

    private IActivityRepository repository(ActivityAccountEntity main,
                                           ActivityAccountMonthEntity month,
                                           ActivityAccountDayEntity day) {
        return (IActivityRepository) Proxy.newProxyInstance(
                IActivityRepository.class.getClassLoader(),
                new Class<?>[]{IActivityRepository.class},
                (proxy, method, args) -> {
                    if ("queryActivityAccountEntity".equals(method.getName())) return main;
                    if ("queryActivityAccountMonthByUserId".equals(method.getName())) return month;
                    if ("queryActivityAccountDayByUserId".equals(method.getName())) return day;
                    return null;
                });
    }
}
