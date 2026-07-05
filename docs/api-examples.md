# API Examples

## Agent chat

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"message":"我的账号登录不上，重置密码也不行"}'
```

## Confirm ticket creation

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"CV_xxx","userId":1,"message":"确认创建","confirmCreateTicket":true}'
```

## Upload knowledge

```bash
curl -X POST http://localhost:8080/api/knowledge/documents \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"title":"退款失败补充规则","fileName":"refund-extra.md","category":"REFUND","content":"退款失败超过 24 小时未到账时，需要收集订单号、支付渠道和失败提示，并创建退款售后工单。"}'
```
