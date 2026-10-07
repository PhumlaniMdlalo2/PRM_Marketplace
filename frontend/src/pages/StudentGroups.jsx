import { ArrowUpRight, MessagesSquare, Plus, Users } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import {
  createStudentGroup, createStudentGroupPost, joinStudentGroup, leaveStudentGroup,
  listStudentGroupPosts, listStudentGroups,
} from '../api/studentGroups';
import Layout from '../components/layout/Layout';

const errorText = (error) => error?.message || 'The request failed. Please try again.';

const StudentGroups = () => {
  const { user } = useAuth();
  const [groups, setGroups] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [posts, setPosts] = useState([]);
  const [loadingGroups, setLoadingGroups] = useState(true);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [groupForm, setGroupForm] = useState({ name: '', description: '' });
  const [postForm, setPostForm] = useState({ title: '', body: '' });

  const selectedGroup = useMemo(
    () => groups.find((group) => group.id === selectedId) ?? null,
    [groups, selectedId],
  );

  const refreshGroups = useCallback(async () => {
    try {
      const result = await listStudentGroups();
      setGroups(result);
      setSelectedId((current) => (
        result.some((group) => group.id === current)
          ? current
          : result.find((group) => group.joined)?.id ?? result[0]?.id ?? ''
      ));
      setError('');
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setLoadingGroups(false);
    }
  }, []);

  useEffect(() => {
    let current = true;
    listStudentGroups()
      .then((result) => {
        if (!current) return;
        setGroups(result);
        setSelectedId(result.find((group) => group.joined)?.id ?? result[0]?.id ?? '');
        setError('');
      })
      .catch((caught) => {
        if (current) setError(errorText(caught));
      })
      .finally(() => {
        if (current) setLoadingGroups(false);
      });
    return () => { current = false; };
  }, []);

  useEffect(() => {
    let current = true;
    const load = async () => {
      if (!selectedGroup?.joined) {
        setPosts([]);
        return;
      }
      setLoadingPosts(true);
      try {
        const result = await listStudentGroupPosts(selectedGroup.id);
        if (current) {
          setPosts(result);
          setError('');
        }
      } catch (caught) {
        if (current) setError(errorText(caught));
      } finally {
        if (current) setLoadingPosts(false);
      }
    };
    load();
    return () => { current = false; };
  }, [selectedGroup]);

  const submitGroup = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const created = await createStudentGroup(groupForm);
      setGroupForm({ name: '', description: '' });
      await refreshGroups();
      setSelectedId(created.id);
      setNotice(`“${created.name}” is ready for students at ${created.campus}.`);
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setBusy(false);
    }
  };

  const changeMembership = async (group) => {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (group.joined) await leaveStudentGroup(group.id);
      else await joinStudentGroup(group.id);
      await refreshGroups();
      setSelectedId(group.id);
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setBusy(false);
    }
  };

  const submitPost = async (event) => {
    event.preventDefault();
    if (!selectedGroup) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const created = await createStudentGroupPost(selectedGroup.id, postForm);
      setPosts((current) => [created, ...current]);
      setPostForm({ title: '', body: '' });
      setNotice('Your discussion has been posted.');
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setBusy(false);
    }
  };

  if (user?.role !== 'STUDENT') {
    return (
      <Layout>
        <main className="app-container max-w-4xl py-12">
          <h1 className="text-3xl font-bold tracking-tight text-text-primary">Student groups</h1>
          <p className="mt-3 max-w-xl text-text-secondary">
            Campus discussion groups are available to student accounts.
          </p>
          <Link to="/bulletin" className="mt-5 inline-flex font-semibold text-primary hover:underline">
            Return to the bulletin
          </Link>
        </main>
      </Layout>
    );
  }

  return (
    <Layout>
      <main className="app-container max-w-6xl py-7">
        <header className="mb-7 border-b border-border pb-6">
          <p className="text-sm font-semibold text-primary">Your campus, your conversations</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-text-primary">Student groups</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-text-secondary">
            Find people studying, building and trading on your campus. Only students with the same
            campus on their profile can join or read a group discussion.
          </p>
          {user?.campus ? (
            <p className="mt-3 inline-flex items-center gap-2 rounded-lg bg-lavender px-3 py-2 text-sm font-medium text-text-primary">
              <Users size={16} aria-hidden="true" /> {user.campus}
              <Link to="/profile/edit" className="ml-1 text-primary hover:underline">Change</Link>
            </p>
          ) : (
            <p className="mt-3 text-sm text-text-secondary">
              Add your campus before joining groups. <Link className="font-semibold text-primary hover:underline" to="/profile/edit">Update profile</Link>
            </p>
          )}
        </header>

        {error && <p className="mb-5 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800" role="alert">{error}</p>}
        {notice && <p className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900" role="status">{notice}</p>}

        <div className="grid items-start gap-8 lg:grid-cols-[minmax(16rem,0.78fr)_minmax(0,1.5fr)]">
          <aside>
            <section aria-labelledby="campus-groups-heading">
              <div className="mb-3 flex items-center justify-between">
                <h2 id="campus-groups-heading" className="text-lg font-semibold text-text-primary">At your campus</h2>
                <span className="text-xs tabular-nums text-text-muted">{groups.length} groups</span>
              </div>

              {loadingGroups ? (
                <p className="py-6 text-sm text-text-secondary">Loading campus groups…</p>
              ) : groups.length === 0 ? (
                <div className="border-y border-border py-7">
                  <p className="font-medium text-text-primary">Start the first conversation</p>
                  <p className="mt-1 text-sm leading-6 text-text-secondary">
                    Create a group for your course, residence, society or campus interests.
                  </p>
                </div>
              ) : (
                <ul className="divide-y divide-border border-y border-border">
                  {groups.map((group) => (
                    <li key={group.id} className={`py-4 ${group.id === selectedId ? 'border-l-2 border-primary pl-3' : 'pl-4'}`}>
                      <button
                        type="button"
                        onClick={() => setSelectedId(group.id)}
                        className="w-full text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                        aria-pressed={group.id === selectedId}
                      >
                        <span className="flex items-start justify-between gap-3">
                          <span className="font-semibold text-text-primary">{group.name}</span>
                          <span className="shrink-0 text-xs tabular-nums text-text-muted">{group.memberCount} members</span>
                        </span>
                        <span className="mt-1 block text-sm leading-5 text-text-secondary">{group.description}</span>
                      </button>
                      <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-xs text-text-muted">{group.joined ? 'Member' : 'Campus group'}</span>
                        <button
                          type="button"
                          disabled={busy || !user?.campus}
                          onClick={() => changeMembership(group)}
                          className="min-h-9 rounded-lg border border-border px-3 text-sm font-semibold text-primary transition-colors hover:bg-lavender disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {group.joined ? 'Leave' : 'Join'}
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <form onSubmit={submitGroup} className="mt-8 border-t border-border pt-5">
              <h2 className="flex items-center gap-2 font-semibold text-text-primary"><Plus size={17} aria-hidden="true" /> Create a group</h2>
              <label className="mt-4 block text-sm font-medium text-text-primary" htmlFor="group-name">Group name</label>
              <input
                id="group-name"
                required
                maxLength={120}
                value={groupForm.name}
                onChange={(event) => setGroupForm((form) => ({ ...form, name: event.target.value }))}
                className="mt-1 min-h-10 w-full rounded-lg border border-border bg-white px-3 text-sm text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
              />
              <label className="mt-3 block text-sm font-medium text-text-primary" htmlFor="group-description">What is this group for?</label>
              <textarea
                id="group-description"
                required
                maxLength={500}
                rows={3}
                value={groupForm.description}
                onChange={(event) => setGroupForm((form) => ({ ...form, description: event.target.value }))}
                className="mt-1 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
              />
              <button
                type="submit"
                disabled={busy || !user?.campus}
                className="mt-3 min-h-10 rounded-lg bg-primary px-4 text-sm font-semibold text-white transition hover:brightness-95 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50"
              >
                Create campus group
              </button>
            </form>
          </aside>

          <section aria-labelledby="group-discussion-heading" className="min-w-0">
            {!selectedGroup ? (
              <div className="border-y border-border py-10 text-center">
                <MessagesSquare size={26} className="mx-auto text-text-muted" aria-hidden="true" />
                <p className="mt-3 font-semibold text-text-primary">Choose a campus group</p>
                <p className="mt-1 text-sm text-text-secondary">Join one to read and post in its private discussion.</p>
              </div>
            ) : (
              <>
                <header className="border-b border-border pb-4">
                  <p className="text-xs font-semibold text-primary">{selectedGroup.campus} · {selectedGroup.memberCount} members</p>
                  <h2 id="group-discussion-heading" className="mt-1 text-2xl font-bold tracking-tight text-text-primary">{selectedGroup.name}</h2>
                  <p className="mt-2 max-w-2xl text-sm leading-6 text-text-secondary">{selectedGroup.description}</p>
                </header>

                {selectedGroup.joined ? (
                  <form onSubmit={submitPost} className="border-b border-border py-5">
                    <label htmlFor="discussion-title" className="block text-sm font-semibold text-text-primary">Start a discussion</label>
                    <input
                      id="discussion-title"
                      required
                      maxLength={200}
                      placeholder="Give your post a clear title"
                      value={postForm.title}
                      onChange={(event) => setPostForm((form) => ({ ...form, title: event.target.value }))}
                      className="mt-2 min-h-10 w-full rounded-lg border border-border bg-white px-3 text-sm text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                    />
                    <textarea
                      required
                      maxLength={5000}
                      rows={3}
                      aria-label="Discussion text"
                      placeholder="Share the details with your group"
                      value={postForm.body}
                      onChange={(event) => setPostForm((form) => ({ ...form, body: event.target.value }))}
                      className="mt-2 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                    />
                    <button type="submit" disabled={busy} className="min-h-10 rounded-lg bg-primary px-4 text-sm font-semibold text-white transition hover:brightness-95 disabled:opacity-50">
                      Post to group
                    </button>
                  </form>
                ) : (
                  <div className="my-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-lavender/60 p-4">
                    <p className="text-sm text-text-primary">Join this group to read discussions and post with campus members.</p>
                    <button
                      type="button"
                      disabled={busy || !user?.campus}
                      onClick={() => changeMembership(selectedGroup)}
                      className="min-h-10 rounded-lg bg-primary px-4 text-sm font-semibold text-white transition hover:brightness-95 disabled:opacity-50"
                    >
                      Join group
                    </button>
                  </div>
                )}

                {selectedGroup.joined && (
                  <div className="divide-y divide-border">
                    {loadingPosts ? (
                      <p className="py-7 text-sm text-text-secondary">Loading group discussions…</p>
                    ) : posts.length === 0 ? (
                      <p className="py-7 text-sm text-text-secondary">No discussions yet. Start one above.</p>
                    ) : posts.map((post) => (
                      <article key={post.id} className="py-5">
                        <h3 className="text-lg font-semibold leading-snug text-text-primary">
                          <Link to={`/bulletin/${post.id}`} className="hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
                            {post.title}
                          </Link>
                        </h3>
                        <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-text-secondary">{post.body}</p>
                        <Link to={`/bulletin/${post.id}`} className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
                          Open discussion <ArrowUpRight size={15} aria-hidden="true" />
                        </Link>
                      </article>
                    ))}
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </main>
    </Layout>
  );
};

export default StudentGroups;
