package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学生课表 前端控制器
 * </p>
 *
 * @author guolihong
 * @since 2024-11-18
 */
@Api(tags = "我的课程表相关接口")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;

    @ApiOperation("查询我的课表，排序字段 latest_learn_time:学习时间，create_time:创建时间")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
        return learningLessonService.queryMyLessons(query);
    }

    @ApiOperation("查询正在学习的课程")
    @GetMapping("/lessons/now")
    public LearningLessonVO queryLearningLesson(){
        return learningLessonService.queryLearningLesson();
    }

    @ApiOperation("删除课程表中的课程")
    @GetMapping("/lessons/delete")
    public void deleteLesson(@PathVariable Long courseId){
        learningLessonService.deleteLesson(null, CollUtils.singletonList(courseId));
    }

    @ApiOperation("检验当前课程是否有效")
    @GetMapping("/lessons/{courseId}/valid")
    Long isLessonValid(@PathVariable("courseId") Long courseId){
        return learningLessonService.isLessonValid(courseId);
    }

    @ApiOperation("查询指定课程的状态")
    @GetMapping("/ls/lessons/{courseId}")
    public LearningLessonVO queryCourseStatus(@PathVariable Long courseId){
        return learningLessonService.queryCourseStatus(courseId);
    }

    @ApiOperation("查询指定课程的学习人数")
    @GetMapping("/lessons/{courseId}/count")
    public Long queryLessonCount(@PathVariable Long courseId){
        return learningLessonService.queryLessonCount(courseId);
    }
}
