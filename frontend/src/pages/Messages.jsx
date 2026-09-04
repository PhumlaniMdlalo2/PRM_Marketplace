import BackButton from '../components/ui/BackButton';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';

const Messages = () => {
  const conversations = [
    {
      id: 1,
      name: 'Thando Nkosi',
      lastMessage: 'Is the coffee table still available?',
      time: '2m',
      unread: 2,
      avatar: null,
    },
    {
      id: 2,
      name: 'Aisha Bello',
      lastMessage: 'Can you deliver to campus?',
      time: '1h',
      unread: 0,
      avatar: null,
    },
    {
      id: 3,
      name: 'Jabu Mokoena',
      lastMessage: 'Thanks for the quick response',
      time: '3h',
      unread: 0,
      avatar: null,
    },
    {
      id: 4,
      name: 'Lerato Khumalo',
      lastMessage: 'What\'s the lowest price you\'d accept?',
      time: 'Yesterday',
      unread: 1,
      avatar: null,
    },
  ];
  
  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Messages</h1>
        </div>
        
        <div className="space-y-1">
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              className="w-full flex items-center gap-4 p-3 rounded-2xl hover:bg-lavender/70 active:bg-lavender transition-colors text-left"
            >
              <span className="relative">
                <Avatar src={conversation.avatar} alt={conversation.name} />
                {conversation.unread > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-primary rounded-full border-2 border-white flex items-center justify-center text-[8px] font-bold text-white">
                    {/* unread count */}
                  </span>
                )}
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center justify-between">
                  <span className="font-semibold text-text-primary">{conversation.name}</span>
                  <span className="text-xs text-text-muted">{conversation.time}</span>
                </span>
                <span className={`block text-sm mt-0.5 truncate ${
                  conversation.unread > 0 ? 'text-text-primary font-medium' : 'text-text-secondary'
                }`}>
                  {conversation.lastMessage}
                </span>
              </span>
            </button>
          ))}
        </div>
        
        {conversations.length === 0 && (
          <div className="py-16 text-center">
            <p className="text-text-secondary">No conversations yet</p>
            <p className="text-sm text-text-muted mt-1">Messages from sellers appear here</p>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Messages;
