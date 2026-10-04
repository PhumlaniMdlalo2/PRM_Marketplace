import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';
import { createPost } from '../api/bulletin';
import { useAuth } from '../auth/useAuth';

const TITLE_LIMIT = 200;
const BODY_LIMIT = 5000;

/**
 * The audience toggle maps onto the post's `category` field, which is the only free-form label the
 * BulletinPost entity carries. It is a tag rather than an access control: the server treats every
 * post as equally readable, so the label here must not imply that "Campus" posts are hidden from
 * signed-out visitors — they are not.
 */
const AUDIENCES = [
  { id: 'Campus', label: 'Campus', hint: 'Only relevant to people on campus' },
  { id: 'Marketplace', label: 'Marketplace', hint: 'Buy, sell and general finds' },
  { id: 'Advice', label: 'Advice', hint: 'Questions and recommendations' },
];

const CreatePost = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState(AUDIENCES[0].id);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting) return;

    const body = content.trim();
    // The server requires a title, so the body cannot be the only field on a post. Rather than
    // inventing a title from the first line, the field is simply required here and says so.
    const headline = title.trim();
    if (!body || !headline) return;

    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});
    try {
      await createPost({ title: headline, body, category });
      navigate('/bulletin');
    } catch (caught) {
      setFormError(caught.message);
      if (caught.fieldErrors) setFieldErrors(caught.fieldErrors);
      setSubmitting(false);
    }
  };

  const bodyTooLong = content.length > BODY_LIMIT;
  const titleTooLong = title.length > TITLE_LIMIT;
  const canSubmit = !submitting && content.trim().length > 0 && title.trim().length > 0
    && !bodyTooLong && !titleTooLong;

  return (
    <Layout showNav={false}>
      <form className="app-container py-6 max-w-2xl mx-auto" onSubmit={handleSubmit}>
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Create a post</h1>
        </div>

        <div className="flex items-start gap-3 px-1">
          <Avatar size="sm" src={user?.avatarUrl || null} alt={user?.name ?? 'You'} />
          <div className="flex-1">
            <label htmlFor="post-audience" className="sr-only">Post audience</label>
            <select
              id="post-audience"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="flex items-center gap-1.5 text-sm font-medium text-primary bg-primary-muted px-3 py-1 rounded-full hover:bg-lavender-dark transition-colors"
            >
              {AUDIENCES.map((audience) => (
                <option key={audience.id} value={audience.id}>{audience.label}</option>
              ))}
            </select>
            <p className="text-xs text-text-muted mt-1.5">
              {AUDIENCES.find((audience) => audience.id === category)?.hint}
            </p>
          </div>
        </div>

        <div className="mt-4 px-2">
          <label htmlFor="post-title" className="sr-only">Post title</label>
          <input
            id="post-title"
            type="text"
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              if (fieldErrors.title) setFieldErrors({ ...fieldErrors, title: undefined });
            }}
            placeholder="Give your post a title"
            maxLength={TITLE_LIMIT}
            className={`w-full bg-transparent border-none text-lg font-semibold text-text-primary placeholder-text-muted placeholder-font-normal focus:outline-none ${
              fieldErrors.title ? 'text-red-700' : ''
            }`}
          />
          {fieldErrors.title && (
            <p className="text-sm text-red-700 mt-1">{fieldErrors.title}</p>
          )}
        </div>

        <label htmlFor="post-body" className="sr-only">Post content</label>
        <textarea
          id="post-body"
          value={content}
          onChange={(e) => {
            setContent(e.target.value);
            if (fieldErrors.body) setFieldErrors({ ...fieldErrors, body: undefined });
          }}
          placeholder="Share something with your campus community..."
          className="w-full mt-2 min-h-[240px] px-2 py-2 bg-transparent border-none text-[17px] text-text-primary placeholder-text-muted focus:outline-none resize-none leading-relaxed"
        />
        {fieldErrors.body && (
          <p className="text-sm text-red-700 mt-1 px-2">{fieldErrors.body}</p>
        )}

        <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
          <span className={`text-xs ${bodyTooLong ? 'text-red-700' : 'text-text-muted'}`}>
            {content.length} characters
            {bodyTooLong && ` — over the ${BODY_LIMIT} limit`}
          </span>
          {titleTooLong && (
            <span className="text-xs text-red-700">Title over {TITLE_LIMIT} characters</span>
          )}
        </div>

        {formError && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{formError}</p>
          </div>
        )}

        <div className="mt-4">
          <Button type="submit" size="lg" disabled={!canSubmit}>
            {submitting ? 'Posting…' : 'Post to bulletin'}
          </Button>
        </div>
      </form>
    </Layout>
  );
};

export default CreatePost;