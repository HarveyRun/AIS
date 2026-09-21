package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;

public record ExperienceReviewScore(
    int materialSupport,
    int commonRelevance,
    int learnability,
    int clarity,
    int logicConsistency,
    int informationSpecificity
) {
    public static ExperienceReviewScore of(
        Integer materialSupport,
        Integer commonRelevance,
        Integer learnability,
        Integer clarity,
        Integer logicConsistency,
        Integer informationSpecificity
    ) {
        return new ExperienceReviewScore(
            required(materialSupport, "证明材料支撑度"),
            required(commonRelevance, "普遍相关程度"),
            required(learnability, "可借鉴程度"),
            required(clarity, "表述清晰程度"),
            required(logicConsistency, "逻辑自洽程度"),
            required(informationSpecificity, "信息具体程度")
        );
    }

    public int referenceIndex() {
        return (materialSupport + commonRelevance + learnability + clarity + logicConsistency) * 2;
    }

    public boolean approved() {
        return informationSpecificity > 5;
    }

    private static int required(Integer value, String label) {
        if (value == null) {
            throw BusinessException.badRequest("请填写" + label + "评分");
        }
        if (value < 0 || value > 10) {
            throw BusinessException.badRequest(label + "评分必须为0至10分");
        }
        return value;
    }
}
