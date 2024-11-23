package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * <p>
 * 学生课表 服务类
 * </p>
 *
 * @author guolihong
 * @since 2024-11-18
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    LearningLessonVO queryLearningLesson();

    void deleteLesson(Long userId, List<Long> courseIds);

    Long isLessonValid(Long courseId);

    LearningLessonVO queryCourseStatus(Long courseId);

    Long queryLessonCount(Long courseId);
}
