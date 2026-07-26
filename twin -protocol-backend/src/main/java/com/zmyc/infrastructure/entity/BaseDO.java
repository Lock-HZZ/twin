package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * 基础实体类，包含审计字段
 */
public abstract class BaseDO {
    
    /**
     * 创建时间（10位时间戳）
     */
    @TableField(value = "created_date", fill = FieldFill.INSERT)
    private Long createdDate;
    
    /**
     * 最后更新时间（10位时间戳）
     */
    @TableField(value = "last_updated_date", fill = FieldFill.INSERT_UPDATE)
    private Long lastUpdatedDate;
    
    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
    
    /**
     * 最后更新人ID
     */
    @TableField(value = "last_updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long lastUpdatedBy;
    
    // Getters and Setters
    public Long getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }
    
    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }
    
    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public Long getLastUpdatedBy() {
        return lastUpdatedBy;
    }
    
    public void setLastUpdatedBy(Long lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
} 