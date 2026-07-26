package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserDepositDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDepositMapper extends BaseMapper<UserDepositDO> {

    @Select("SELECT * FROM user_deposit WHERE tx_hash = #{txHash}")
    UserDepositDO findByTxHash(@Param("txHash") String txHash);
}
