package com.example.backend.approval.persistence.mapper;

import com.example.backend.approval.persistence.entity.MessageReadRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface MessageReadRecordMapper {

    @Insert("""
            INSERT IGNORE INTO message_read_record (
                message_id, user_id, read_time
            ) VALUES (
                #{messageId}, #{userId}, #{readTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(MessageReadRecordEntity entity);

    @Select("""
            SELECT * FROM message_read_record
            WHERE message_id = #{messageId} AND user_id = #{userId}
            """)
    @Results(id = "messageReadResultMap", value = {
            @Result(column = "message_id", property = "messageId"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "read_time", property = "readTime"),
            @Result(column = "create_time", property = "createTime")
    })
    Optional<MessageReadRecordEntity> findByMessageAndUser(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId
    );
}
