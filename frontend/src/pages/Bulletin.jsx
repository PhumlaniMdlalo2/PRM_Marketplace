import { MessageCircle, ThumbsUp, Plus } from 'lucide-react';
import { useState } from 'react';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { Link } from 'react-router-dom';

const Bulletin = () => {
  const [liked, setLiked] = useState({});
  
  const posts = [
    {
      id: 1,
      user: { name: 'Thandiwe Mahlangu', avatar: null },
      time: '4h',
      content: "Halfway through finals week and surviving on coffee and good wifi. Anyone else been through the library 'quiet floor' vs 'group work floor' debate?",
      likes: 24,
      comments: 8,
      tags: ['CPUT', 'StudentLife'],
    },
    {
      id: 2,
      user: { name: 'Sipho Dlamini', avatar: null },
      time: '6h',
      content: 'Just put my vintage bike up for sale on the marketplace. Perfect for commuting around Bellville. Comes with a new lock.',
      likes: 15,
      comments: 3,
      tags: ['Marketplace', 'Bikes'],
    },
    {
      id: 3,
      user: { name: 'Fatima Patel', avatar: null },
      time: '1d',
      content: 'Looking for a decent second-hand laptop for graphic design work. Budget around R6k. Recommendations welcome.',
      likes: 42,
      comments: 12,
      tags: ['Electronics', 'Advice'],
    },
  ];
  
  const toggleLike = (id) => {
    setLiked((prev) => ({ ...prev, [id]: !prev[id] }));
  };
  
  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight mb-6">Bulletin</h1>
        
        <div className="space-y-3.5">
          {posts.map((post) => {
            const isLiked = liked[post.id];
            return (
              <article key={post.id} className="bg-white border border-border rounded-2xl p-4">
                <div className="flex items-start gap-3">
                  <Avatar src={post.user.avatar} alt={post.user.name} size="sm" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h3 className="font-semibold text-text-primary">{post.user.name}</h3>
                      <span className="text-xs text-text-muted">{post.time}</span>
                    </div>
                    <p className="mt-2 text-text-secondary leading-relaxed text-wrap-pretty">{post.content}</p>
                    
                    {post.tags && post.tags.length > 0 && (
                      <div className="mt-3 flex gap-1.5 flex-wrap">
                        {post.tags.map(tag => (
                          <span key={tag} className="text-xs font-medium text-primary bg-primary-muted px-2 py-0.5 rounded-md">
                            #{tag}
                          </span>
                        ))}
                      </div>
                    )}
                    
                    <div className="mt-4 pt-3 border-t border-border flex items-center gap-6">
                      <button 
                        onClick={() => toggleLike(post.id)}
                        className={`flex items-center gap-1.5 transition-all duration-200 active:scale-90 ${
                          isLiked ? 'text-primary' : 'text-text-secondary hover:text-primary'
                        }`}
                        aria-pressed={isLiked}
                      >
                        <ThumbsUp size={17} className={isLiked ? 'fill-primary' : ''} />
                        <span className="text-sm font-medium">{post.likes + (isLiked ? 1 : 0)}</span>
                      </button>
                      <button className="flex items-center gap-1.5 text-text-secondary hover:text-primary transition-colors">
                        <MessageCircle size={17} />
                        <span className="text-sm font-medium">{post.comments}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
        
        <Link
          to="/bulletin/create"
          className="fixed bottom-24 right-4 lg:bottom-6 w-14 h-14 bg-primary rounded-full flex items-center justify-center shadow-lg shadow-primary/30 hover:bg-primary-hover hover:shadow-primary/40 active:scale-90 transition-all duration-200 z-40"
          aria-label="Create a post"
        >
          <Plus size={26} className="text-white" />
        </Link>
      </div>
    </Layout>
  );
};

export default Bulletin;
