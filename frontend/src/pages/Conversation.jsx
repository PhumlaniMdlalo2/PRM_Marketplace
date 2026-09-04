import { useState } from 'react';
import { Send } from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';

const Conversation = () => {
  const [message, setMessage] = useState('');
  
  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: 'other',
      text: 'Hi! Is the coffee table still available?',
      time: '10:30',
    },
    {
      id: 2,
      sender: 'me',
      text: 'Yes, it is! When would you like to view it?',
      time: '10:32',
    },
    {
      id: 3,
      sender: 'other',
      text: 'Is tomorrow afternoon okay? I can come to campus.',
      time: '10:35',
    },
    {
      id: 4,
      sender: 'me',
      text: 'Sure, 2pm works for me. I\'ll send you the exact location.',
      time: '10:36',
    },
  ]);
  
  const handleSend = () => {
    const trimmed = message.trim();
    if (trimmed) {
      setMessages([
        ...messages,
        { id: messages.length + 1, sender: 'me', text: trimmed, time: 'Now' },
      ]);
      setMessage('');
    }
  };
  
  return (
    <div className="min-h-dvh bg-white flex flex-col">
      <header className="px-4 py-3.5 border-b border-border">
        <div className="max-w-2xl mx-auto flex items-center gap-3">
        <BackButton />
        <Avatar size="sm" alt="Thando Nkosi" />
        <div>
          <h2 className="font-semibold text-text-primary">Thando Nkosi</h2>
          <p className="text-xs text-success flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-success" aria-hidden="true" />
            Active now
          </p>
        </div>
        </div>
      </header>
      
      <div className="flex-1 overflow-y-auto py-5">
      <div className="max-w-2xl mx-auto px-4 space-y-3">
        <div className="text-center mb-3">
          <span className="text-[11px] font-medium text-text-muted bg-lavender px-3 py-1 rounded-full">Today</span>
        </div>
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex ${msg.sender === 'me' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[78%] px-4 py-2.5 rounded-2xl ${
                msg.sender === 'me'
                  ? 'bg-primary text-white rounded-br-md shadow-sm shadow-primary/20'
                  : 'bg-lavender text-text-primary rounded-bl-md'
              }`}
            >
              <p className="text-sm leading-relaxed">{msg.text}</p>
              <p className={`text-[11px] mt-1 ${msg.sender === 'me' ? 'text-white/70' : 'text-text-muted'}`}>
                {msg.time}
              </p>
            </div>
          </div>
        ))}
      </div>
      </div>
      
      <footer className="px-4 py-4 border-t border-border bg-white/95 backdrop-blur safe-area-inset-bottom">
        <div className="max-w-2xl mx-auto flex items-center gap-3">
          <input
            type="text"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Type a message..."
            aria-label="Type a message"
            className="flex-1 px-4 py-3 bg-lavender rounded-full text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all duration-200"
          />
          <button
            onClick={handleSend}
            disabled={!message.trim()}
            className="w-11 h-11 bg-primary rounded-full flex items-center justify-center text-white hover:bg-primary-hover active:scale-90 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
            aria-label="Send message"
          >
            <Send size={18} />
          </button>
        </div>
      </footer>
    </div>
  );
};

export default Conversation;
