import { Home, Search, ShoppingCart, MessageSquare, User } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import NotificationBell from './NotificationBell';

const navItems = [
  { icon: Home, label: 'Home', path: '/marketplace', activeWhen: ['/marketplace'] },
  { icon: Search, label: 'Search', path: '/search', activeWhen: ['/search'] },
  { icon: ShoppingCart, label: 'Orders', path: '/orders', activeWhen: ['/orders'] },
  { icon: MessageSquare, label: 'Bulletin', path: '/bulletin', activeWhen: ['/bulletin'] },
  { icon: User, label: 'Profile', path: '/profile', activeWhen: ['/profile'] },
];

const BottomNavigation = () => {
  const { pathname } = useLocation();

  return (
    <nav 
      className="fixed bottom-0 left-0 right-0 bg-background/95 border-t border-border z-50 lg:hidden"
      aria-label="Main navigation"
    >
      <div className="max-w-[480px] mx-auto flex justify-around items-center py-2 safe-area-inset-bottom">
        {navItems.map((item) => {
          const isActive = item.activeWhen.includes(pathname);
          return (
            <Link
              key={item.path}
              to={item.path}
              aria-current={isActive ? 'page' : undefined}
              className={`relative flex flex-col items-center gap-1 px-2 py-1.5 rounded-xl transition-all duration-200 active:scale-95 ${
                isActive 
                  ? 'text-primary' 
                  : 'text-text-muted hover:text-text-primary'
              }`}
            >
              <span className={`p-1.5 rounded-xl transition-all duration-200 ${isActive ? 'bg-primary-muted' : ''}`}>
                <item.icon size={20} strokeWidth={isActive ? 2.4 : 2} />
              </span>
              <span className={`text-[11px] ${isActive ? 'font-semibold' : 'font-medium'}`}>{item.label}</span>
            </Link>
          );
        })}

        <NotificationBell variant="tab" />
      </div>
    </nav>
  );
};

export default BottomNavigation;
