import { Home, Search, ShoppingCart, MessageSquare, Store, User } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import NotificationBell from './NotificationBell';

const desktopItems = [
  { icon: Home, label: 'Home', path: '/marketplace', activeWhen: ['/marketplace'] },
  { icon: Search, label: 'Search', path: '/search', activeWhen: ['/search'] },
  { icon: ShoppingCart, label: 'Orders', path: '/orders', activeWhen: ['/orders'] },
  { icon: Store, label: 'Sellers', path: '/vendors', activeWhen: ['/vendors'] },
  { icon: MessageSquare, label: 'Bulletin', path: '/bulletin', activeWhen: ['/bulletin'] },
  { icon: User, label: 'Profile', path: '/profile', activeWhen: ['/profile'] },
];

const TopNavigation = () => {
  const { pathname } = useLocation();

  return (
    <header     className="hidden lg:block sticky top-0 z-50 bg-background/95 border-b border-border">
      <div className="app-container flex items-center justify-between h-16">
        <Link to="/marketplace" className="flex items-center gap-2 shrink-0" aria-label="Vendra marketplace home">
          <span className="font-bold text-xl tracking-[-0.07em] text-text-primary">vendra<span className="text-primary">.</span></span>
        </Link>

        <div className="flex items-center gap-1">
          <nav className="flex items-center gap-1" aria-label="Primary">
            {desktopItems.map((item) => {
              const isActive = item.activeWhen.includes(pathname);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  aria-current={isActive ? 'page' : undefined}
                  className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'text-primary border-b-2 border-primary'
                      : 'text-text-secondary hover:text-text-primary hover:bg-lavender/60'
                  }`}
                >
                  <item.icon size={17} strokeWidth={isActive ? 2.4 : 2} />
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <NotificationBell />
        </div>
      </div>
    </header>
  );
};

export default TopNavigation;
