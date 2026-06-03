package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Rating;
import com.example.campusskillplatform.service.RatingService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价模块控制器
 * 处理双向评价的提交、查询、回复、统计等接口
 */
@RestController
@RequestMapping("/api/rating")
public class RatingController {

    private static final Logger log = LoggerFactory.getLogger(RatingController.class);

    @Autowired
    private RatingService ratingService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 提交评价（仅已完成订单的双方可操作）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @param toUserId 被评价人ID
     * @param score 评分（1-5分）
     * @param content 评价内容
     * @param ratingType 评价类型（1-发布方评价接单方，2-接单方评价发布方）
     * @return 操作结果
     */
    @PostMapping("/submit")
    public Result<String> submitRating(HttpServletRequest request,
                                       @RequestParam Long orderId,
                                       @RequestParam Long toUserId,
                                       @RequestParam Integer score,
                                       @RequestParam String content,
                                       @RequestParam Integer ratingType) {
        Long fromUserId = tokenUtil.getUserId(request);
        if (fromUserId == null) {
            throw new RuntimeException("未登录");
        }
        if (score < 1 || score > 5) {
            throw new RuntimeException("评分必须在1-5分之间");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("评价内容不能为空");
        }
        if (ratingType != Constants.RATING_TYPE_PUBLISHER_TO_ACCEPTOR &&
                ratingType != Constants.RATING_TYPE_ACCEPTOR_TO_PUBLISHER) {
            throw new RuntimeException("评价类型不合法");
        }
        boolean success = ratingService.submitRating(orderId, fromUserId, toUserId,
                score, content, ratingType);
        if (!success) {
            throw new RuntimeException("评价提交失败");
        }
        log.info("评价提交成功，订单ID：{}，评价人：{}，被评价人：{}", orderId, fromUserId, toUserId);
        return Result.success("评价提交成功");
    }

    /**
     * 获取用户收到的评价列表
     * @param userId 用户ID
     * @return 评价列表
     */
    @GetMapping("/received/{userId}")
    public Result<List<Rating>> getReceivedRatings(@PathVariable Long userId) {
        List<Rating> ratings = ratingService.getUserReceivedRatings(userId);
        return Result.success(ratings);
    }

    /**
     * 获取用户发出的评价列表
     * @param userId 用户ID
     * @return 评价列表
     */
    @GetMapping("/published/{userId}")
    public Result<List<Rating>> getPublishedRatings(@PathVariable Long userId) {
        List<Rating> ratings = ratingService.getUserPublishedRatings(userId);
        return Result.success(ratings);
    }

    /**
     * 回复评价（仅被评价人可操作）
     * @param request HTTP请求（包含Token）
     * @param ratingId 评价ID
     * @param reply 回复内容
     * @return 操作结果
     */
    @PostMapping("/reply/{ratingId}")
    public Result<String> replyRating(HttpServletRequest request,
                                      @PathVariable Long ratingId,
                                      @RequestParam String reply) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        boolean success = ratingService.replyRating(ratingId, userId, reply);
        if (!success) {
            throw new RuntimeException("回复失败");
        }
        return Result.success("回复成功");
    }

    /**
     * 获取订单关联的评价列表
     * @param orderId 订单ID
     * @return 评价列表
     */
    @GetMapping("/order/{orderId}")
    public Result<List<Rating>> getOrderRatings(@PathVariable Long orderId) {
        List<Rating> ratings = ratingService.getOrderRatings(orderId);
        return Result.success(ratings);
    }

    /**
     * 获取用户评价统计（平均分、评分分布等）
     * @param userId 用户ID
     * @return 统计数据
     */
    @GetMapping("/stats/{userId}")
    public Result<Map<String, Object>> getUserRatingStats(@PathVariable Long userId) {
        Map<String, Object> stats = ratingService.getUserRatingStats(userId);
        return Result.success(stats);
    }
}
