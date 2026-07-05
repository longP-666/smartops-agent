# Demo Script

1. 打开 `http://localhost:5173`。
2. 在 Agent 会话里发送：`我的账号登录不上，重置密码也不行`。
3. 观察执行链路：`intent_detection -> knowledge_retrieval -> handoff_decision`。
4. 点击 `确认创建`，系统会调用 `createTicket` 工具生成真实工单。
5. 复制工单编号，继续发送：`查一下 TK202607050001 的处理进度`。
6. 在 Trace 面板查看工具调用、耗时和最终回答。

这个流程展示了项目不是普通聊天机器人，而是能检索企业知识、调用业务工具、写入工单数据并保留审计链路的 Agent。
