package com.smartops.agent.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentService {
    private static final Pattern TICKET_NO = Pattern.compile("TK\\d{12}", Pattern.CASE_INSENSITIVE);

    public Map<String, Object> detect(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        Matcher matcher = TICKET_NO.matcher(message == null ? "" : message);
        String ticketNo = matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
        String intent;
        double confidence;
        if (ticketNo != null && containsAny(text, "查", "进度", "状态", "处理", "ticket")) {
            intent = "QUERY_TICKET";
            confidence = 0.93;
        } else if (ticketNo != null && containsAny(text, "补充", "追加", "回复", "错误码", "还有")) {
            intent = "SUPPLEMENT_TICKET";
            confidence = 0.9;
        } else if (containsAny(text, "人工", "客服", "没人处理", "转人工")) {
            intent = "HUMAN_SERVICE";
            confidence = 0.88;
        } else if (containsAny(text, "创建工单", "建工单", "提交工单", "帮我处理", "确认创建", "创建一个")) {
            intent = "CREATE_TICKET";
            confidence = 0.86;
        } else if (containsAny(text, "你好", "谢谢", "在吗")) {
            intent = "CHITCHAT";
            confidence = 0.72;
        } else {
            intent = "FAQ_QUERY";
            confidence = 0.76;
        }
        return Map.of("intent", intent, "confidence", confidence, "ticketNo", ticketNo == null ? "" : ticketNo);
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
