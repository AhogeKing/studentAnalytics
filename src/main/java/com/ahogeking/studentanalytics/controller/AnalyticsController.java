package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.service.AnalyticsService;
import com.ahogeking.studentanalytics.vo.GpaDistributionItemVO;
import com.ahogeking.studentanalytics.vo.GradeClassDistributionItemVO;
import com.ahogeking.studentanalytics.vo.PerformanceAnalysisPointVO;
import com.ahogeking.studentanalytics.vo.StudentDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 支持图标：GPA 区间人数气泡图
    /* GPA 区间：
        [0.0, 0.5)
        [0.5, 1.0)
        [1.0, 1.5)
        [1.5, 2.0)
        [2.0, 2.5)
        [2.5, 3.0)
        [3.0, 3.5)
        [3.5, 4.0]
       返回结构：
        {
          "code": 0,
          "message": "操作成功",
          "data": [
            {
              "bucket_index": 0,
              "label": "[0.0, 0.5)",
              "min_gpa": 0.0,
              "max_gpa": 0.5,
              "student_count": 142,
              "percentage": 5.94
            },
            {
              "bucket_index": 1,
              "label": "[0.5, 1.0)",
              "min_gpa": 0.5,
              "max_gpa": 1.0,
              "student_count": 326,
              "percentage": 13.63
            }
          ],
          "timestamp": "..."
        }
       前端映射：
         x = label
         y = student_count
         圆面积 = student_count
         symbolSize 由前端计算，后端不返回像素大小
     */
    @GetMapping("/gpa-distribution")
    public Result<List<GpaDistributionItemVO>> getGpaDistribution() {
        return Result.success(analyticsService.selectGpaDistributionItems());
    }

    // GradeClass 柱状图
    /*
        返回结构：
        {
          "code": 0,
          "message": "操作成功",
          "data": [
            {
              "grade_class": {
                "value": 0,
                "label": "优秀"
              },
              "student_count": 77,
              "percentage": 3.22
            },
            {
              "grade_class": {
                "value": 1,
                "label": "良好"
              },
              "student_count": 244,
              "percentage": 10.20
            }
          ],
          "timestamp": "..."
        }
        前端映射：
         x = grade_class.label
         y = student_count
        GradeClass 中文标签继续复用已有：GradeClassEnum、OptionVO<Integer>
     */
    @GetMapping("/grade-class-distribution")
    public Result<List<GradeClassDistributionItemVO>> getGradeClassDistribution() {
        return Result.success(analyticsService.selectGradeClassDistributionItems());
    }

    // 统一表现点接口
    /*
        返回结构：
        {
          "code": 0,
          "message": "操作成功",
          "data": [
            {
              "student_no": 1001,
              "name": "Leo Xavier",
              "study_time_weekly": 12.5000,
              "absences": 3,
              "gpa": 3.7000,
              "grade_class": {
                "value": 0,
                "label": "优秀"
              }
            },
            {
              "student_no": 1002,
              "name": "Student 1002",
              "study_time_weekly": 4.2500,
              "absences": 18,
              "gpa": 1.8500,
              "grade_class": {
                "value": 4,
                "label": "风险"
              }
            }
          ],
          "timestamp": "..."
        }
        这一个接口支持三张图
        缺勤次数与 GPA
            [
                point.absences,
                point.gpa
            ]
        学习时长与 GPA
            [
                point.study_time_weekly
                point.gpa
            ]
        学习时长、缺勤次数、GPA 联合图
            [
                point.study_time_weekly
                point.absences
                point.gpa
            ]
            对应关系：
            x = study_time_weekly
            y = absences
            color = gpa
            前端可以用 ECharts visualMap：

        * 连续色带：直接按照 gpa 着色；
        * 分段颜色：按照 8 个 GPA 区间配置 pieces；
        * 五级颜色：按照 grade_class.value 着色。
     */
    @GetMapping("/performance-points")
    public Result<List<PerformanceAnalysisPointVO>> getPerformancePoints() {
        return Result.success(analyticsService.selectPerformanceAnalysisPoints());
    }
    // 缺勤-GPA 学习时长-GPA 三变量联合散点图
}
