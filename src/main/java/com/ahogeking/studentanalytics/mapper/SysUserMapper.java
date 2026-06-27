package com.ahogeking.studentanalytics.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ahogeking.studentanalytics.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    default SysUser selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, 1)
        );
    }

    default SysUser selectAnyByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
        );
    }

    @Select("""
            SELECT id,
                   username,
                   password,
                   real_name AS realName,
                   role,
                   status,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_user
            ORDER BY created_at DESC, id ASC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<SysUser> selectAdminUserPage(@Param("offset") Long offset, @Param("pageSize") Long pageSize);

    @Select("SELECT COUNT(*) FROM sys_user")
    Long countAdminUsers();
}
