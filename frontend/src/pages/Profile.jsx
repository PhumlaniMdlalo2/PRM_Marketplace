import { Settings, ChevronRight, Package, Heart, MessageCircle, Plus } from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { Link } from 'react-router-dom';

const Profile = () => {
  const menuItems = [
    { icon: Package, label: 'My orders', path: '/orders', count: 3 },
    { icon: Heart, label: 'Saved items', path: '/saved', count: 12 },
    { icon: MessageCircle, label: 'Messages', path: '/messages', count: 5 },
  ];
  
  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Profile</h1>
          <Link 
            to="/settings" 
            className="p-2 rounded-full text-text-secondary hover:text-text-primary hover:bg-lavender transition-colors"
            aria-label="Settings"
          >
            <Settings size={22} />
          </Link>
        </div>
        
        <div className="flex items-center gap-4 p-4 bg-lavender/70 rounded-3xl">
          <Avatar size="lg" alt="Primary user" />
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-text-primary">Naledi Zulu</h2>
            <p className="text-sm text-text-secondary truncate">naledi.zulu@cput.ac.za</p>
            <span className="inline-block mt-1.5 text-xs font-semibold text-primary bg-white/70 px-2 py-0.5 rounded-md">
              Student seller
            </span>
          </div>
        </div>
        
        <div className="mt-6 space-y-2.5">
          {menuItems.map((item) => (
            <Link
              key={item.label}
              to={item.path}
              className="flex items-center gap-4 p-4 bg-white border border-border rounded-2xl hover:border-lavender-dark hover:bg-lavender/40 active:scale-[0.99] transition-all duration-200"
            >
              <span className="w-9 h-9 rounded-xl bg-lavender flex items-center justify-center flex-shrink-0">
                <item.icon size={18} className="text-primary" />
              </span>
              <span className="flex-1 font-medium text-text-primary">{item.label}</span>
              {item.count > 0 && (
                <span className="text-xs font-semibold bg-primary-muted text-primary px-2 py-0.5 rounded-md">
                  {item.count}
                </span>
              )}
              <ChevronRight size={18} className="text-text-muted" />
            </Link>
          ))}
        </div>
        
        <Link
          to="/listing/create"
          className="mt-6 flex items-center justify-center gap-2 py-3.5 bg-primary text-white font-semibold rounded-2xl shadow-md shadow-primary/20 hover:bg-primary-hover active:scale-[0.98] transition-all duration-200"
        >
          <Plus size={20} />
          Sell an item
        </Link>
        
        <Link
          to="/profile/edit"
          className="mt-3 block w-full text-center py-3.5 bg-lavender text-text-primary font-medium rounded-2xl hover:bg-lavender-dark active:scale-[0.98] transition-all duration-200"
        >
          Edit profile
        </Link>
      </div>
    </Layout>
  );
};

export default Profile;
