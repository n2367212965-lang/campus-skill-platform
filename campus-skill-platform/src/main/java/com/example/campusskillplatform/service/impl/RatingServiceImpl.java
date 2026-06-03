package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.entity.Rating;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.mapper.RatingMapper;
import com.example.campusskillplatform.mapper.OrderInfoMapper;
import com.example.campusskillplatform.mapper.UserMapper;
import com.example.campusskillplatform.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingServiceImpl.class);

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitRating(Long orderId, Long fromUserId, Long toUserId,
                                Integer score, String content, Integer ratingType) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 【核心修正】仅已完成的订单可提交评价
        if (order.getStatus() != Constants.ORDER_STATUS_COMPLETED) {
            throw new RuntimeException("仅已完成的订单可提交评价");
        }

        // 权限校验：仅订单双方可评价
        if (!fromUserId.equals(order.getPublisherId()) && !fromUserId.equals(order.getAcceptorId())) {
            throw new RuntimeException("无权评价此订单");
        }

        // 评价类型和身份匹配校验
        if (ratingType == Constants.RATING_TYPE_PUBLISHER_TO_ACCEPTOR && !fromUserId.equals(order.getPublisherId())) {
            throw new RuntimeException("评价类型与身份不匹配，仅发布方可评价接单方");
        }
        if (ratingType == Constants.RATING_TYPE_ACCEPTOR_TO_PUBLISHER && !fromUserId.equals(order.getAcceptorId())) {
            throw new RuntimeException("评价类型与身份不匹配，仅接单方可评价发布方");
        }

        // 校验被评价人必须是订单的另一方
        if (ratingType == Constants.RATING_TYPE_PUBLISHER_TO_ACCEPTOR && !toUserId.equals(order.getAcceptorId())) {
            throw new RuntimeException("被评价人必须是订单接单方");
        }
        if (ratingType == Constants.RATING_TYPE_ACCEPTOR_TO_PUBLISHER && !toUserId.equals(order.getPublisherId())) {
            throw new RuntimeException("被评价人必须是订单发布方");
        }

        if (score < 1 || score > 5) {
            throw new RuntimeException("评分必须在1-5分之间");
        }

        // 重复评价校验
        QueryWrapper<Rating> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("from_user_id", fromUserId)
                .eq("rating_type", ratingType);
        Rating existingRating = this.getOne(queryWrapper);
        if (existingRating != null) {
            throw new RuntimeException("您已评价过此订单，不可重复评价");
        }

        Rating rating = new Rating();
        rating.setOrderId(orderId);
        rating.setFromUserId(fromUserId);
        rating.setToUserId(toUserId);
        rating.setScore(score);
        rating.setContent(content);
        rating.setRatingType(ratingType);
        boolean saved = this.save(rating);
        if (saved) {
            log.info("评价提交成功，评价ID：{}，订单ID：{}", rating.getId(), orderId);
            updateUserCreditScore(toUserId);
        }
        return saved;
    }

    @Override
    public List<Rating> getUserReceivedRatings(Long userId) {
        QueryWrapper<Rating> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("to_user_id", userId)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<Rating> getUserPublishedRatings(Long userId) {
        QueryWrapper<Rating> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_user_id", userId)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public boolean replyRating(Long ratingId, Long userId, String reply) {
        Rating rating = this.getById(ratingId);
        if (rating == null) {
            throw new RuntimeException("评价不存在");
        }

        if (!userId.equals(rating.getToUserId())) {
            throw new RuntimeException("无权回复此评价");
        }

        rating.setReply(reply);
        boolean updated = this.updateById(rating);
        if (updated) {
            log.info("评价回复成功，评价ID：{}，回复人：{}", ratingId, userId);
        }
        return updated;
    }

    @Override
    public List<Rating> getOrderRatings(Long orderId) {
        QueryWrapper<Rating> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        return this.list(queryWrapper);
    }

    @Override
    public Map<String, Object> getUserRatingStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Rating> ratings = getUserReceivedRatings(userId);

        double totalScore = 0;
        Map<Integer, Integer> scoreDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            scoreDistribution.put(i, 0);
        }

        for (Rating rating : ratings) {
            totalScore += rating.getScore();
            scoreDistribution.put(rating.getScore(),
                    scoreDistribution.get(rating.getScore()) + 1);
        }

        double averageScore = ratings.isEmpty() ? 0 : totalScore / ratings.size();

        stats.put("totalRatings", ratings.size());
        stats.put("averageScore", String.format("%.1f", averageScore));
        stats.put("scoreDistribution", scoreDistribution);

        return stats;
    }

    @Override
    public boolean updateUserCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        Map<String, Object> stats = getUserRatingStats(userId);
        double averageScore = Double.parseDouble(stats.get("averageScore").toString());
        int totalRatings = (int) stats.get("totalRatings");

        double creditScore = 80 + averageScore * 4 + Math.log10(totalRatings + 1) * 10;
        creditScore = Math.min(100, Math.max(0, creditScore));

        user.setCreditScore((int) creditScore);
        int rows = userMapper.updateById(user);
        if (rows > 0) {
            log.info("用户信誉分更新，用户ID：{}，新信誉分：{}", userId, (int) creditScore);
        }
        return rows > 0;
    }
}
