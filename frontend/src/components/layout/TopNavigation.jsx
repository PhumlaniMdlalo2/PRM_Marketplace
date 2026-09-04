import { Home, Search, Compass, ShoppingCart, MessageSquare, User, LayoutGrid } from 'lucide-react';
import { NavLink, Link, useLocation } from 'react-router-dom';

const desktopItems = [
  { icon: Home, label: 'Home', path: '/', activeWhen: ['/'] },
  { icon: Search, label: 'Search', path: '/search', activeWhen: ['/search'] },
  { icon: Compass, label: 'For You', path: '/#for-you', activeWhen: ['/'] },
  { icon: ShoppingCart, label: 'Orders', path: '/orders', activeWhen: ['/orders'] },
  { icon: MessageSquare, label: 'Bulletin', path: '/bulletin', activeWhen: ['/bulletin'] },
  { icon: User, label: 'Profile', path: '/profile', activeWhen: ['/profile'] },
];

const TopNavigation = () => {
  const { pathname } = useLocation();

  return (
    <header className="hidden lg:block sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-border">
      <div className="app-container flex items-center justify-between h-16">
        <Link to="/" className="flex items-center gap-2 shrink-0" aria-label="PRM Marketplace home">
          <span className="w-9 h-9 rounded-xl bg-lavender flex items-center justify-center">
            <LayoutGrid size={18} className="text-primary" />
          </span>
          <span className="font-bold text-lg tracking-tight text-text-primary">PRM Marketplace</span>
        </Link>

        <nav className="flex items-center gap-1" aria-label="Primary">
          {desktopItems.map((item) => {
            const isActive = item.activeWhen.includes(pathname);
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-primary-muted text-primary'
                    : 'text-text-secondary hover:text-text-primary hover:bg-lavender/60'
                }`}
              >
                <item.icon size={17} strokeWidth={isActive ? 2.4 : 2} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>
      </div>
    </header>
  );
};

export default TopNavigation;
