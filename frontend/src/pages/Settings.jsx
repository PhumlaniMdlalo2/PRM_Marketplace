import { ChevronRight, User, Shield, Bell, Lock, CreditCard, HelpCircle, FileText, HardDrive, Wifi, AlertCircle, UserPlus, LogOut } from 'lucide-react';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const Settings = () => {
  const sections = [
    {
      title: 'Account',
      items: [
        { icon: User, label: 'Edit profile', path: '/profile/edit' },
        { icon: Shield, label: 'Security', path: '/settings/security' },
        { icon: Bell, label: 'Notifications', path: '/settings/notifications' },
        { icon: Lock, label: 'Privacy', path: '/settings/privacy' },
      ],
    },
    {
      title: 'Support & about',
      items: [
        { icon: CreditCard, label: 'My subscription', path: '/settings/subscription' },
        { icon: HelpCircle, label: 'Help & support', path: '/settings/help' },
        { icon: FileText, label: 'Terms and policies', path: '/settings/terms' },
      ],
    },
    {
      title: 'Cache & cellular',
      items: [
        { icon: HardDrive, label: 'Free up space', path: '/settings/cache' },
        { icon: Wifi, label: 'Data saver', path: '/settings/data-saver' },
      ],
    },
    {
      title: 'Actions',
      items: [
        { icon: AlertCircle, label: 'Report a problem', path: '/settings/report' },
        { icon: UserPlus, label: 'Add account', path: '/settings/add-account' },
        { icon: LogOut, label: 'Log out', path: '/logout', danger: true },
      ],
    },
  ];
  
  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Settings</h1>
        </div>
        
        <div className="space-y-6">
          {sections.map((section) => (
            <section key={section.title}>
              <h2 className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
                {section.title}
              </h2>
              <div className="bg-white border border-border rounded-2xl overflow-hidden divide-y divide-border">
                {section.items.map((item) => (
                  <button
                    key={item.label}
                    className={`w-full flex items-center gap-3.5 px-4 py-3.5 hover:bg-lavender/50 active:bg-lavender transition-colors ${
                      item.danger ? 'text-error' : 'text-text-primary'
                    }`}
                  >
                    <span className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                      item.danger ? 'bg-error/10' : 'bg-lavender'
                    }`}>
                      <item.icon 
                        size={17} 
                        className={item.danger ? 'text-error' : 'text-primary'} 
                      />
                    </span>
                    <span className="flex-1 text-left font-medium text-[15px]">{item.label}</span>
                    <ChevronRight size={17} className="text-text-muted" />
                  </button>
                ))}
              </div>
            </section>
          ))}
        </div>
      </div>
    </Layout>
  );
};

export default Settings;
