import React, { useEffect, useState } from 'react';
import { aiBuddyApi } from '../../api/aiBuddy.js';
import { generateUUID } from '../../api/http.js';

const scenarios = [
  ['DAILY_CONVERSATION', 'Hội thoại hằng ngày'],
  ['VOCABULARY_GRAMMAR', 'Từ vựng và ngữ pháp'],
  ['ROLE_PLAY', 'Đóng vai'],
];

function safeMessage(error) {
  return error?.error?.message || 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.';
}

export function AiBuddyPage() {
  const [conversations, setConversations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [scenario, setScenario] = useState('DAILY_CONVERSATION');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const loadList = async () => {
    setLoading(true);
    try {
      const response = await aiBuddyApi.list();
      setConversations(response.data);
      if (!selected && response.data.length > 0) await selectConversation(response.data[0]);
    } catch (reason) {
      setError(safeMessage(reason));
    } finally {
      setLoading(false);
    }
  };

  const selectConversation = async (conversation) => {
    setError('');
    try {
      const response = await aiBuddyApi.read(conversation.id);
      setSelected(response.data.conversation);
      setMessages(response.data.messages);
    } catch (reason) {
      setError(safeMessage(reason));
    }
  };

  useEffect(() => { void loadList(); }, []);

  const createConversation = async (event) => {
    event.preventDefault();
    setError('');
    try {
      const response = await aiBuddyApi.create({ scenario, ...(title.trim() ? { title: title.trim() } : {}) });
      setConversations((current) => [response.data, ...current]);
      setTitle('');
      await selectConversation(response.data);
    } catch (reason) {
      setError(safeMessage(reason));
    }
  };

  const sendMessage = async (event) => {
    event.preventDefault();
    if (!selected || !content.trim() || sending) return;
    setSending(true); setError('');
    try {
      const response = await aiBuddyApi.send(selected.id, content.trim(), generateUUID());
      setMessages((current) => [...current, response.data.learnerMessage, response.data.assistantMessage]);
      setContent('');
      await loadList();
    } catch (reason) {
      setError(safeMessage(reason));
    } finally {
      setSending(false);
    }
  };

  const deleteConversation = async () => {
    if (!selected || !window.confirm('Xóa cuộc hội thoại này? Nội dung sẽ không còn dùng cho các phản hồi tiếp theo.')) return;
    try {
      await aiBuddyApi.remove(selected.id);
      setConversations((current) => current.filter((item) => item.id !== selected.id));
      setSelected(null); setMessages([]);
    } catch (reason) {
      setError(safeMessage(reason));
    }
  };

  return (
    <section className="mx-auto max-w-6xl space-y-5" aria-labelledby="ai-buddy-title" data-testid="ai-buddy-page">
      <header className="space-y-1">
        <h1 id="ai-buddy-title" className="text-2xl font-bold text-foreground">AI Buddy</h1>
        <p className="text-sm text-muted-foreground">Luyện tiếng Trung trong phạm vi hội thoại an toàn và riêng tư.</p>
      </header>
      {error && <p className="rounded-lg border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive" role="alert">{error}</p>}
      <div className="grid gap-5 lg:grid-cols-[18rem_1fr]">
        <aside className="rounded-xl border border-border bg-card p-4 space-y-4" aria-label="Danh sách hội thoại">
          <form className="space-y-3" onSubmit={createConversation}>
            <label className="block text-sm font-semibold" htmlFor="ai-buddy-scenario">Hoạt động học</label>
            <select id="ai-buddy-scenario" className="w-full rounded-lg border border-input bg-background p-2" value={scenario} onChange={(event) => setScenario(event.target.value)}>
              {scenarios.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            <label className="block text-sm font-semibold" htmlFor="ai-buddy-title">Tên hội thoại (không bắt buộc)</label>
            <input id="ai-buddy-title" className="w-full rounded-lg border border-input bg-background p-2" maxLength="120" value={title} onChange={(event) => setTitle(event.target.value)} />
            <button className="min-h-11 w-full rounded-lg bg-primary px-4 font-semibold text-primary-foreground" type="submit">Tạo hội thoại</button>
          </form>
          <div className="space-y-2" aria-live="polite">
            {loading && <p className="text-sm text-muted-foreground">Đang tải…</p>}
            {!loading && conversations.length === 0 && <p className="text-sm text-muted-foreground">Chưa có hội thoại.</p>}
            {conversations.map((conversation) => (
              <button key={conversation.id} type="button" onClick={() => void selectConversation(conversation)}
                className={`min-h-11 w-full rounded-lg p-3 text-left text-sm ${selected?.id === conversation.id ? 'bg-secondary text-foreground' : 'hover:bg-accent text-muted-foreground'}`}>
                <span className="block font-semibold text-foreground">{conversation.title || scenarios.find(([value]) => value === conversation.scenario)?.[1]}</span>
              </button>
            ))}
          </div>
        </aside>
        <div className="min-h-[32rem] rounded-xl border border-border bg-card p-4 flex flex-col">
          {!selected ? <p className="m-auto text-muted-foreground">Tạo hoặc chọn một hội thoại để bắt đầu luyện tập.</p> : <>
            <div className="flex items-start justify-between gap-3 border-b border-border pb-3">
              <div><h2 className="font-bold">{selected.title || 'AI Buddy'}</h2><p className="text-sm text-muted-foreground">{selected.scenario}</p></div>
              <button type="button" onClick={() => void deleteConversation()} className="min-h-11 rounded-lg px-3 text-sm font-semibold text-destructive hover:bg-destructive/10">Xóa</button>
            </div>
            <ol className="flex-1 space-y-3 overflow-y-auto py-4" aria-label="Tin nhắn">
              {messages.map((message) => <li key={message.id} className={message.sender === 'LEARNER' ? 'text-right' : 'text-left'}>
                <div className="inline-block max-w-[90%] rounded-xl bg-secondary p-3 text-left"><p>{message.content}</p>{message.vietnameseExplanation && <p className="mt-2 text-sm text-muted-foreground">{message.vietnameseExplanation}</p>}{message.suggestion && <p className="mt-2 text-sm font-semibold">Luyện tiếp: {message.suggestion}</p>}</div>
              </li>)}
            </ol>
            <form onSubmit={sendMessage} className="border-t border-border pt-3 space-y-2">
              <label className="sr-only" htmlFor="ai-buddy-message">Tin nhắn tiếng Trung hoặc câu hỏi học tiếng Trung</label>
              <textarea id="ai-buddy-message" className="min-h-24 w-full rounded-lg border border-input bg-background p-3" maxLength="1000" value={content} onChange={(event) => setContent(event.target.value)} placeholder="Ví dụ: ‘你好’ nghĩa là gì?" disabled={sending} />
              <div className="flex items-center justify-between gap-3"><span className="text-xs text-muted-foreground">{content.length}/1000</span><button type="submit" disabled={sending || !content.trim()} className="min-h-11 rounded-lg bg-primary px-5 font-semibold text-primary-foreground disabled:opacity-50">{sending ? 'Đang nhận phản hồi…' : 'Gửi'}</button></div>
            </form>
          </>}
        </div>
      </div>
    </section>
  );
}
