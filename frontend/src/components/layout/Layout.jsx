import BottomNavigation from './BottomNavigation';
import TopNavigation from './TopNavigation';

const Layout = ({ children, showNav = true, className = '' }) => {
  return (
    <div className={`min-h-dvh bg-white ${className}`}>
      {showNav && <TopNavigation />}
      <main id="main-content" className={showNav ? 'lg:pb-12 pb-24' : 'pb-4'}>
        {children}
      </main>
      {showNav && <BottomNavigation />}
    </div>
  );
};

export default Layout;
