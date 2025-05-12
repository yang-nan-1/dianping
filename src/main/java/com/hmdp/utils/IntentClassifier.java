package com.hmdp.utils;

public class IntentClassifier {

    public enum Intent {
        KNOWLEDGE_QA,
        CHECK_COUPON,
        UNKNOWN
    }

    public static Intent classify(String question) {
        if (question.contains("优惠券") || question.contains("抢购")) {
            return Intent.CHECK_COUPON;
        }
        return Intent.KNOWLEDGE_QA;
    }
}
