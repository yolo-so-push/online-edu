package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.prometheus.client.Collector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课表 服务实现类
 * </p>
 *
 * @author guolihong
 * @since 2024-11-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("all")
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;
    @Override
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //添加课程到课程表
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if (userId==null){
            log.error("用户信息不存在");
            return;
        }
        if (CollUtils.isEmpty(simpleInfoList)){
            log.error("课程信息不存在，无法添加到课程表");
            return;
        }
        //循环遍历处理数据
        List<LearningLesson> learningLessonList=new ArrayList<>();
        for (CourseSimpleInfoDTO infoDTO : simpleInfoList) {
            LearningLesson learningLesson=new LearningLesson();
            Integer validDuration = infoDTO.getValidDuration();
            if (validDuration!=null&& validDuration>0){
                LocalDateTime now = LocalDateTime.now();
                learningLesson.setCreateTime(now);
                learningLesson.setExpireTime(now.plusMonths(validDuration));
            }
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(infoDTO.getId());
            learningLessonList.add(learningLesson);
        }
        saveBatch(learningLessonList);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        Long userId = UserContext.getUser();
        Page<LearningLesson> page = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //查询课程信息
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = queryCourseSimpleInfoList(records);
        //封装VO
        List<LearningLessonVO> list = new ArrayList<>();
        for (LearningLesson record : records) {
            LearningLessonVO learningLessonVO = BeanUtils.copyBean(record, LearningLessonVO.class);
            CourseSimpleInfoDTO infoDTO = courseSimpleInfoDTOMap.get(record.getCourseId());
            learningLessonVO.setCourseName(infoDTO.getName());
            learningLessonVO.setCourseCoverUrl(infoDTO.getCoverUrl());
            learningLessonVO.setLearnedSections(infoDTO.getSectionNum());
            list.add(learningLessonVO);
        }
        return PageDTO.of(page,list);
    }

    @Override
    public LearningLessonVO queryLearningLesson() {
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1").one();
        if (learningLesson==null){
            return null;
        }
        LearningLessonVO vo = BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
        CourseFullInfoDTO course = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if (course==null){
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(course.getName());
        vo.setCourseCoverUrl(course.getCoverUrl());
        vo.setSections(course.getSectionNum());
        //统计课程表中的课程数量
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(learningLesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataSimpleInfoDTOS)){
            CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
            vo.setLearnedSections(cataSimpleInfoDTO.getCIndex());
            vo.setLatestSectionName(cataSimpleInfoDTO.getName());
        }
        return vo;
    }

    @Override
    public void deleteLesson(Long userId, List<Long> courseIds) {
        if (userId == null) {
            userId = UserContext.getUser();
        }
        if (CollUtils.isEmpty(courseIds)) {
            log.error("没有课程要删除");
            return;
        }
        LambdaQueryChainWrapper<LearningLesson> queryChainWrapper = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds)
                .eq(LearningLesson::getStatus, LessonStatus.EXPIRED);
        remove(queryChainWrapper);
    }

    @Override
    public Long isLessonValid(Long courseId) {
        if (courseId==null){
            return null;
        }
        LambdaQueryChainWrapper<LearningLesson> queryChainWrapper = lambdaQuery().eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED);
        LearningLesson learningLesson = getOne(queryChainWrapper);
        return learningLesson.getId();
    }

    @Override
    public LearningLessonVO queryCourseStatus(Long courseId) {
        if (courseId==null){
            return null;
        }
        LambdaQueryChainWrapper<LearningLesson> queryChainWrapper = lambdaQuery().eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId);
        LearningLesson learningLesson = getOne(queryChainWrapper);
        return BeanUtils.copyBean(learningLesson,LearningLessonVO.class);
    }

    @Override
    public Long queryLessonCount(Long courseId) {
        Long count = Long.valueOf(lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .count());
        return count;
    }

    private Map<Long,CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        Set<Long> idSet = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(idSet);
        if (CollUtils.isEmpty(simpleInfoList)){
            throw new BadRequestException("课程信息不存在");
        }
        Map<Long, CourseSimpleInfoDTO> cMap = simpleInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }
}
