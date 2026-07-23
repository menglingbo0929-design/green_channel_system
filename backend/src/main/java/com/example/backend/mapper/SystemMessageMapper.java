package com.example.backend.mapper;

import com.example.backend.model.domain.SystemMessageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SystemMessageMapper {

    @Insert("""
            INSERT INTO system_message (
                receiver_user_id, message_type, business_type, business_id,
                title, content, create_by
            ) VALUES (
                #{receiverUserId}, #{messageType}, #{businessType}, #{businessId},
                #{title}, #{content}, #{createBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemMessageEntity entity);

    @Select("SELECT * FROM system_message WHERE id = #{id}")
    @Results(id = "systemMessageResultMap", value = {
            @Result(column = "receiver_user_id", property = "receiverUserId"),
            @Result(column = "message_type", property = "messageType"),
            @Result(column = "business_type", property = "businessType"),
            @Result(column = "business_id", property = "businessId"),
            @Result(column = "create_by", property = "createBy"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "is_read", property = "read")
    })
    Optional<SystemMessageEntity> findById(Long id);

    @Select("""
            SELECT * FROM system_message
            WHERE receiver_user_id = #{receiverUserId}
            ORDER BY create_time DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("systemMessageResultMap")
    List<SystemMessageEntity> listByReceiver(
            @Param("receiverUserId") Long receiverUserId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM system_message WHERE receiver_user_id = #{receiverUserId}")
    long countByReceiver(Long receiverUserId);

    @Select("""
            SELECT COUNT(*)
            FROM system_message m
            LEFT JOIN message_read_record r
              ON r.message_id = m.id AND r.user_id = #{receiverUserId}
            WHERE m.receiver_user_id = #{receiverUserId}
              AND r.id IS NULL
            """)
    long countUnreadByReceiver(Long receiverUserId);

    @Select("""
            <script>
            SELECT m.*,
                   CASE WHEN r.id IS NULL THEN FALSE ELSE TRUE END AS is_read
            FROM system_message m
            LEFT JOIN message_read_record r
              ON r.message_id = m.id AND r.user_id = #{receiverUserId}
            WHERE m.receiver_user_id = #{receiverUserId}
            <if test="read != null and read">
              AND r.id IS NOT NULL
            </if>
            <if test="read != null and !read">
              AND r.id IS NULL
            </if>
            ORDER BY m.create_time DESC, m.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    @ResultMap("systemMessageResultMap")
    List<SystemMessageEntity> listByReceiverAndRead(
            @Param("receiverUserId") Long receiverUserId,
            @Param("read") Boolean read,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM system_message m
            LEFT JOIN message_read_record r
              ON r.message_id = m.id AND r.user_id = #{receiverUserId}
            WHERE m.receiver_user_id = #{receiverUserId}
            <if test="read != null and read">
              AND r.id IS NOT NULL
            </if>
            <if test="read != null and !read">
              AND r.id IS NULL
            </if>
            </script>
            """)
    long countByReceiverAndRead(
            @Param("receiverUserId") Long receiverUserId,
            @Param("read") Boolean read
    );
}
