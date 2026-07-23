package com.example.backend.mapper;

import com.example.backend.model.dto.StudentRecommendationView;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentRecommendationMapper {
    @Insert("INSERT INTO student_recommendation(student_id,batch_id,recommendation_type,content_snapshot) VALUES(#{studentId},#{batchId},#{type},#{content}) ON DUPLICATE KEY UPDATE content_snapshot=VALUES(content_snapshot)")
    int upsert(@Param("studentId") Long studentId, @Param("batchId") Long batchId,
               @Param("type") String type, @Param("content") String content);

    @Select("SELECT id,batch_id AS batchId,recommendation_type AS recommendationType,content_snapshot AS content,read_flag AS `read`,create_time AS createTime FROM student_recommendation WHERE student_id=#{studentId} AND deleted=0 ORDER BY read_flag,id DESC")
    List<StudentRecommendationView> findMine(@Param("studentId") Long studentId);

    @Update("UPDATE student_recommendation SET read_flag=1 WHERE id=#{id} AND student_id=#{studentId} AND deleted=0")
    int markRead(@Param("id") Long id, @Param("studentId") Long studentId);
}
