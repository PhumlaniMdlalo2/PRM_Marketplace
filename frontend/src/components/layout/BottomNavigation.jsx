import { Home, Search, Compass, ShoppingCart, MessageSquare, User } from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';

const navItems = [
  { icon: Home, label: 'Home', path: '/', activeWhen: ['/'] },
  { icon: Search, label: 'Search', path: '/search', activeWhen: ['/search'] },
  { icon: Compass, label: 'For You', path: '/#for-you', activeWhen: ['/'] },
  { icon: ShoppingCart, label: 'Orders', path: '/orders', activeWhen: ['/orders'] },
  { icon: MessageSquare, label: 'Bulletin', path: '/bulletin', activeWhen: ['/bulletin'] },
  { icon: User, label: 'Profile', path: '/profile', activeWhen: ['/profile'] },
];

const BottomNavigation = () => {
  const { pathname } = useLocation();

  return (
    <nav 
      className="fixed bottom-0 left-0 right-0 bg-white/90 backdrop-blur-lg border-t border-border z-50 lg:hidden"
      aria-label="Main navigation"
    >
      <div className="max-w-[480px] mx-auto flex justify-around items-center py-2 safe-area-inset-bottom">
        {navItems.map((item) => {
          const isActive = item.activeWhen.includes(pathname);
          return (
            <NavLink
              key={item.path}
              to={item.path}
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
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
};

export default BottomNavigation;
