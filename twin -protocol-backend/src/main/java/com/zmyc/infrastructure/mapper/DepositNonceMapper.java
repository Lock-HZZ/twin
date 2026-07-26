package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.DepositNonceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepositNonceMapper extends BaseMapper<DepositNonceDO> {

    DepositNonceDO findByUserIdAndNonce(@Param("userId") Long userId, @Param("nonce") Long nonce);
}
