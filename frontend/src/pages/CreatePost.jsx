import { useState } from 'react';
import { Globe } from 'lucide-react';
import Button from '../components/ui/Button';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const CreatePost = () => {
  const [content, setContent] = useState('');
  const [visibility, setVisibility] = useState('campus');
  
  const handleSubmit = () => {
    // Handle create post
    console.log('Create post:', content, visibility);
  };
  
  return (
    <Layout showNav={false}>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Create a post</h1>
        </div>
        
        <div className="flex items-start gap-3 px-1">
          <Avatar size="sm" alt="You" />
          <div className="flex-1">
            <button 
              onClick={() => setVisibility(visibility === 'campus' ? 'public' : 'campus')}
              className="flex items-center gap-1.5 text-sm font-medium text-primary bg-primary-muted px-3 py-1 rounded-full hover:bg-lavender-dark transition-colors"
            >
              <Globe size={14} />
              {visibility === 'campus' ? 'Campus' : 'Public'}
            </button>
          </div>
        </div>
        
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Share something with your campus community..."
          aria-label="Post content"
          className="w-full mt-4 min-h-[280px] px-2 py-2 bg-transparent border-none text-[17px] text-text-primary placeholder-text-muted focus:outline-none resize-none leading-relaxed"
        />
        
        <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
          <span className="text-xs text-text-muted">{content.length} characters</span>
        </div>
        
        <div className="mt-4">
          <Button 
            onClick={handleSubmit} 
            size="lg"
            disabled={!content.trim()}
          >
            Post to bulletin
          </Button>
        </div>
      </div>
    </Layout>
  );
};

export default CreatePost;
