import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity,
  Bot,
  BrainCircuit,
  ClipboardList,
  DatabaseZap,
  FileText,
  MessagesSquare,
  RefreshCw,
  Search,
  Send,
  ShieldCheck,
  Sparkles,
  Ticket,
  UploadCloud
} from 'lucide-react';
import './styles.css';

type ApiResponse<T> = { code: number; message: string; data: T };
type Reference = { chunkId: number; title: string; source: string; category: string; content: string; score: number };
type Step = { name: string; type: string; status: string; costMs: number; input: string; output: string };
type ChatResponse = {
  conversationId: string;
  traceId: string;
  intent: string;
  answer: string;
  needConfirm: boolean;
  ticketDraft?: { title: string; description: string; categoryCode: string; priority: number };
  references: Reference[];
  steps: Step[];
};
type TicketRow = {
  ticketNo: string;
  title: string;
  categoryName: string;
  priority: number;
  status: string;
  assigneeName: string;
  updateTime: string;
};
type TraceRow = {
  traceId: string;
  question: string;
  finalAnswer: string;
  status: string;
  totalCostMs: number;
  createTime: string;
};

const api = async <T,>(url: string, init?: RequestInit): Promise<T> => {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    ...init
  });
  const payload = (await response.json()) as ApiResponse<T>;
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '请求失败');
  }
  return payload.data;
};

function App() {
  const [message, setMessage] = useState('我的账号登录不上，重置密码也不行');
  const [conversationId, setConversationId] = useState<string>();
  const [log, setLog] = useState<Array<{ role: 'user' | 'agent'; text: string; meta?: ChatResponse }>>([]);
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<any>();
  const [tickets, setTickets] = useState<TicketRow[]>([]);
  const [traces, setTraces] = useState<TraceRow[]>([]);
  const [docs, setDocs] = useState<any[]>([]);
  const [searchText, setSearchText] = useState('退款失败');
  const [searchResults, setSearchResults] = useState<Reference[]>([]);
  const [docContent, setDocContent] = useState('订单支付成功但退款失败时，需要收集订单号、退款发起时间、支付渠道和失败提示。超过 24 小时未到账必须创建退款售后工单。');

  const latest = useMemo(() => [...log].reverse().find((item) => item.meta)?.meta, [log]);

  const refresh = async () => {
    const [summaryData, ticketData, traceData, docData] = await Promise.all([
      api<any>('/api/dashboard/summary'),
      api<TicketRow[]>('/api/tickets'),
      api<TraceRow[]>('/api/traces?limit=8'),
      api<any[]>('/api/knowledge/documents')
    ]);
    setSummary(summaryData);
    setTickets(ticketData);
    setTraces(traceData);
    setDocs(docData);
  };

  useEffect(() => {
    refresh().catch(console.error);
  }, []);

  const send = async (confirm = false) => {
    if (!message.trim() && !confirm) return;
    const outgoing = confirm ? '确认创建工单' : message;
    setLoading(true);
    setLog((items) => [...items, { role: 'user', text: outgoing }]);
    try {
      const data = await api<ChatResponse>('/api/agent/chat', {
        method: 'POST',
        body: JSON.stringify({ conversationId, userId: 1, message: outgoing, confirmCreateTicket: confirm })
      });
      setConversationId(data.conversationId);
      setLog((items) => [...items, { role: 'agent', text: data.answer, meta: data }]);
      setMessage('');
      await refresh();
    } finally {
      setLoading(false);
    }
  };

  const search = async () => {
    setSearchResults(await api<Reference[]>(`/api/knowledge/search?query=${encodeURIComponent(searchText)}&topK=5`));
  };

  const uploadDoc = async () => {
    await api('/api/knowledge/documents', {
      method: 'POST',
      body: JSON.stringify({
        knowledgeBaseId: 1,
        title: '控制台补充知识',
        fileName: 'console-note.md',
        category: 'REFUND',
        content: docContent
      })
    });
    await refresh();
    await search();
  };

  return (
    <main className="shell">
      <aside className="rail">
        <div className="brand"><Bot size={22} /><span>SmartOps<br />Agent</span></div>
        <button title="会话"><MessagesSquare size={20} /></button>
        <button title="知识库"><DatabaseZap size={20} /></button>
        <button title="工单"><Ticket size={20} /></button>
        <button title="链路"><Activity size={20} /></button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">RAG + Tool Calling Console</p>
            <h1>工单 Agent 协同工作台</h1>
          </div>
          <button className="iconText" onClick={refresh}><RefreshCw size={16} />刷新</button>
        </header>

        <section className="metrics">
          <Metric icon={<MessagesSquare />} label="会话" value={summary?.conversations ?? 0} />
          <Metric icon={<Ticket />} label="工单" value={summary?.tickets ?? 0} />
          <Metric icon={<FileText />} label="知识文档" value={summary?.documents ?? 0} />
          <Metric icon={<ShieldCheck />} label="人工兜底" value={summary?.handoffs ?? 0} />
        </section>

        <section className="grid">
          <div className="panel chat">
            <div className="panelHead"><BrainCircuit size={18} /><strong>Agent 会话</strong><span>{conversationId || '新会话'}</span></div>
            <div className="messages">
              {log.length === 0 && <div className="empty">输入一个真实问题，Agent 会检索知识库、判断意图，并在需要时创建工单草稿。</div>}
              {log.map((item, index) => (
                <div className={`bubble ${item.role}`} key={index}>
                  <span>{item.text}</span>
                  {item.meta?.needConfirm && <button className="confirm" onClick={() => send(true)}>确认创建</button>}
                </div>
              ))}
            </div>
            <div className="composer">
              <textarea value={message} onChange={(event) => setMessage(event.target.value)} placeholder="描述账号、订单、退款或系统故障问题" />
              <button onClick={() => send(false)} disabled={loading}><Send size={18} /></button>
            </div>
          </div>

          <div className="panel trace">
            <div className="panelHead"><Activity size={18} /><strong>执行链路</strong><span>{latest?.intent || '等待请求'}</span></div>
            <div className="timeline">
              {(latest?.steps || []).map((step, index) => (
                <div className="step" key={`${step.name}-${index}`}>
                  <i>{index + 1}</i>
                  <div><b>{step.name}</b><p>{step.type} · {step.status} · {step.costMs || 0}ms</p></div>
                </div>
              ))}
              {!latest && <div className="empty">Trace 会展示 intent_detection、knowledge_retrieval、tool_call、final_response 等步骤。</div>}
            </div>
          </div>

          <div className="panel knowledge">
            <div className="panelHead"><DatabaseZap size={18} /><strong>知识库检索</strong><span>{docs.length} documents</span></div>
            <div className="searchLine">
              <input value={searchText} onChange={(event) => setSearchText(event.target.value)} />
              <button title="检索" onClick={search}><Search size={17} /></button>
            </div>
            <div className="refs">
              {searchResults.map((item) => (
                <article key={item.chunkId}>
                  <b>{item.title}</b><small>{item.category} · score {item.score.toFixed(2)}</small>
                  <p>{item.content.slice(0, 160)}</p>
                </article>
              ))}
            </div>
            <textarea className="docInput" value={docContent} onChange={(event) => setDocContent(event.target.value)} />
            <button className="iconText full" onClick={uploadDoc}><UploadCloud size={16} />上传知识片段</button>
          </div>

          <div className="panel tickets">
            <div className="panelHead"><ClipboardList size={18} /><strong>工单队列</strong><span>{tickets.length} items</span></div>
            <div className="table">
              {tickets.map((ticket) => (
                <div className="row" key={ticket.ticketNo}>
                  <b>{ticket.ticketNo}</b>
                  <span>{ticket.title}</span>
                  <em>P{ticket.priority}</em>
                  <i>{ticket.status}</i>
                </div>
              ))}
            </div>
          </div>

          <div className="panel traces">
            <div className="panelHead"><Sparkles size={18} /><strong>最近 Trace</strong><span>audit ready</span></div>
            {traces.map((trace) => (
              <div className="traceRow" key={trace.traceId}>
                <b>{trace.traceId}</b>
                <span>{trace.question}</span>
                <small>{trace.status} · {trace.totalCostMs || 0}ms</small>
              </div>
            ))}
          </div>
        </section>
      </section>
    </main>
  );
}

function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return <div className="metric">{icon}<span>{label}</span><b>{value}</b></div>;
}

createRoot(document.getElementById('root')!).render(<App />);
