package com.liuhecai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuhecai.entity.RechargeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RechargeRequestMapper extends BaseMapper<RechargeRequest> {

    @Update("""
            UPDATE recharge_requests
            SET status = #{status},
                handled_at = #{handledAt},
                handled_by = #{handlerId},
                reject_reason = #{reason},
                updated_at = #{handledAt}
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND status = 0
            """)
    int casUpdateStatus(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("status") int status,
                        @Param("handledAt") LocalDateTime handledAt,
                        @Param("handlerId") Long handlerId,
                        @Param("reason") String reason);
}
