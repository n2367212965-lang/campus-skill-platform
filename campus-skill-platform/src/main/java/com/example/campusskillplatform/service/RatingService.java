package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.Rating;
import java.util.List;
import java.util.Map;

public interface RatingService extends IService<Rating> {

    boolean submitRating(Long orderId, Long fromUserId, Long toUserId,
                         Integer score, String content, Integer ratingType);
    // 查询我收到的评价（to_user_id=当前用户）
    List<Rating> getUserReceivedRatings(Long userId);
    // 新增：查询我发布的评价（from_user_id=当前用户）
    List<Rating> getUserPublishedRatings(Long userId);
    boolean replyRating(Long ratingId, Long userId, String reply);
    List<Rating> getOrderRatings(Long orderId);
    Map<String, Object> getUserRatingStats(Long userId);
    boolean updateUserCreditScore(Long userId);
}