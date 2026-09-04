import { useState } from 'react';
import { ThumbsUp, MessageCircle, Send } from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const PostComments = () => {
  const [comment, setComment] = useState('');
  
  const post = {
    user: { name: 'Thandiwe Mahlangu', avatar: null },
    time: '4h',
    content: "Halfway through finals week and surviving on coffee and good wifi. Anyone else been through the library 'quiet floor' vs 'group work floor' debate?",
    likes: 24,
    comments: 3,
    tags: ['CPUT', 'StudentLife'],
  };
  
  const [comments, setComments] = useState([
    {
      id: 1,
      user: { name: 'Aisha Bello', avatar: null },
      content: 'Quiet floor all the way. I can\'t focus with group chatter around me.',
      time: '3h',
      likes: 5,
    },
    {
      id: 2,
      user: { name: 'Sipho Dlamini', avatar: null },
      content: 'Trick is to book one of the study pods early. Best of both worlds.',
      time: '2h',
      likes: 12,
    },
    {
      id: 3,
      user: { name: 'Fatima Patel', avatar: null },
      content: 'Group work floor during the day, quiet floor after 6pm. My winning combo.',
      time: '1h',
      likes: 8,
    },
  ]);
  
  const handleSend = () => {
    const trimmed = comment.trim();
    if (trimmed) {
      setComments([
        ...comments,
        {
          id: comments.length + 1,
          user: { name: 'You', avatar: null },
          content: trimmed,
          time: 'Now',
          likes: 0,
        },
      ]);
      setComment('');
    }
  };
  
  return (
    <Layout showNav={false}>
      <div className="app-container py-6 pb-32 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-xl font-bold text-text-primary tracking-tight">
            Comments <span className="text-text-muted font-medium">({comments.length})</span>
          </h1>
        </div>
        
        <article className="bg-white border border-border rounded-2xl p-4 mb-6">
          <div className="flex items-start gap-3">
            <Avatar src={post.user.avatar} alt={post.user.name} size="sm" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-text-primary">{post.user.name}</h3>
                <span className="text-xs text-text-muted">{post.time}</span>
              </div>
              <p className="mt-2 text-text-secondary leading-relaxed text-wrap-pretty">{post.content}</p>
              {post.tags && (
                <div className="mt-2.5 flex gap-1.5 flex-wrap">
                  {post.tags.map(tag => (
                    <span key={tag} className="text-xs font-medium text-primary bg-primary-muted px-2 py-0.5 rounded-md">#{tag}</span>
                  ))}
                </div>
              )}
              <div className="mt-3.5 pt-3 border-t border-border flex items-center gap-6">
                <span className="flex items-center gap-1.5 text-text-secondary">
                  <ThumbsUp size={16} />
                  <span className="text-sm font-medium">{post.likes}</span>
                </span>
                <span className="flex items-center gap-1.5 text-text-secondary">
                  <MessageCircle size={16} />
                  <span className="text-sm font-medium">{post.comments}</span>
                </span>
              </div>
            </div>
          </div>
        </article>
        
        <div className="space-y-4">
          {comments.map((c) => (
            <div key={c.id} className="flex items-start gap-3">
              <Avatar src={c.user.avatar} alt={c.user.name} size="sm" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h4 className="font-semibold text-text-primary text-sm">{c.user.name}</h4>
                  <span className="text-xs text-text-muted">{c.time}</span>
                </div>
                <p className="mt-1 text-text-secondary text-[15px] leading-relaxed">{c.content}</p>
                <button className="flex items-center gap-1 mt-1.5 text-text-muted hover:text-primary transition-colors text-xs font-medium">
                  <ThumbsUp size={13} /> {c.likes}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
      
      <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur border-t border-border p-4 z-40 safe-area-inset-bottom">
        <div className="app-container flex items-center gap-3 max-w-2xl mx-auto">
          <Avatar size="sm" alt="You" />
          <input
            type="text"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Add a comment..."
            aria-label="Add a comment"
            className="flex-1 px-4 py-2.5 bg-lavender rounded-full text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all duration-200 text-sm"
          />
          <button
            onClick={handleSend}
            disabled={!comment.trim()}
            className="w-10 h-10 bg-primary rounded-full flex items-center justify-center text-white hover:bg-primary-hover active:scale-90 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
            aria-label="Post comment"
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </Layout>
  );
};

export default PostComments;
