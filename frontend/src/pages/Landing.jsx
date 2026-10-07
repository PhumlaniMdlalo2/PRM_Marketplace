import { useState } from 'react';
import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import './Landing.css';

const sections = [
  {
    id: 'goods',
    label: 'Goods',
    title: 'Goods for study, home and everyday life',
    action: 'Browse goods',
    href: '/marketplace',
    categories: [
      { name: 'Textbooks and study materials', examples: 'Textbooks, notes, past papers and stationery' },
      { name: 'Electronics', examples: 'Laptops, phones, calculators, chargers and headphones' },
      { name: 'Clothing and accessories', examples: 'Second-hand clothes, shoes, bags and uniforms' },
      { name: 'Home and residence essentials', examples: 'Bedding, small appliances, kitchenware, furniture and storage' },
      { name: 'Food and drinks', examples: 'Vendor meals, baked goods, snacks and fresh produce' },
      { name: 'Health and beauty', examples: 'Toiletries, skincare and hair products' },
      { name: 'Sports and hobbies', examples: 'Gym gear, instruments, art supplies and games' },
      { name: 'Free items and giveaways', examples: 'Free items shared with the campus community' },
    ],
  },
  {
    id: 'services',
    label: 'Services',
    title: 'Skills and services from people nearby',
    action: 'Browse sellers',
    href: '/vendors',
    categories: [
      { name: 'Tutoring and academic help', examples: 'Get help with a subject, assignment or exam preparation' },
      { name: 'Tech and design services', examples: 'Web design, repairs and printing' },
      { name: 'Personal services', examples: 'Hair, braiding, laundry and photography' },
      { name: 'Transport and delivery', examples: 'Lift clubs, errands and moving help' },
      { name: 'Vendor and local business services', examples: 'Discover services offered by campus vendors and local businesses' },
    ],
  },
  {
    id: 'community',
    label: 'Community',
    title: 'Useful updates from your campus community',
    action: 'Visit the bulletin',
    href: '/bulletin',
    categories: [
      { name: 'Events and announcements', examples: 'Keep up with what is happening around campus' },
      { name: 'Clubs and society fundraisers', examples: 'Support student groups and their events' },
      { name: 'Lost and found', examples: 'Share a notice or look for a missing item' },
      { name: 'Accommodation and roommates', examples: 'Find accommodation or a roommate' },
      { name: 'Jobs and gigs', examples: 'Find flexible work and one-off opportunities' },
    ],
  },
];

const steps = [
  {
    title: 'Browse what is nearby',
    description: 'Browse goods, services and updates from your campus community.',
  },
  {
    title: 'Contact the right person',
    description: 'Ask a seller or service provider about the details.',
  },
  {
    title: 'Make a plan',
    description: 'Arrange a handover, ask for help or join a community event.',
  },
];

const Landing = () => {
  const [activeSection, setActiveSection] = useState('goods');
  const selectedSection = sections.find((section) => section.id === activeSection);

  const moveTab = (event) => {
    const currentIndex = sections.findIndex((section) => section.id === activeSection);
    let nextIndex = currentIndex;

    if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % sections.length;
    else if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + sections.length) % sections.length;
    else if (event.key === 'Home') nextIndex = 0;
    else if (event.key === 'End') nextIndex = sections.length - 1;
    else return;

    event.preventDefault();
    setActiveSection(sections[nextIndex].id);
    event.currentTarget.parentElement
      .querySelectorAll('[role="tab"]')[nextIndex]
      .focus();
  };

  return (
    <div className="landing-page">
      <header className="landing-header">
        <div className="landing-header-inner">
          <Link to="/" className="landing-brand" aria-label="Vendra home">
            <span>vendra<span className="landing-brand-period">.</span></span>
          </Link>

          <nav className="landing-nav" aria-label="Main">
            <Link to="/marketplace">Goods</Link>
            <Link to="/vendors">Services</Link>
            <Link to="/bulletin">Community</Link>
          </nav>

          <Link className="landing-sign-in" to="/login">Sign in</Link>
        </div>
      </header>

      <main id="main-content">
        <section className="landing-hero landing-container" aria-labelledby="landing-title">
          <div className="landing-hero-copy">
            <p className="landing-eyebrow">For your campus community</p>
            <h1 id="landing-title">A marketplace for campus life.</h1>
            <p className="landing-intro">
              Buy and sell goods, find services and share campus updates with students, staff, vendors and neighbours.
            </p>
            <div className="landing-actions">
              <Link className="landing-button landing-button-primary" to="/marketplace">
                Browse the marketplace <ArrowRight size={18} aria-hidden="true" />
              </Link>
              <Link className="landing-text-link" to="/signup">Start selling</Link>
            </div>
          </div>

          <div className="landing-hero-photos" aria-label="A look at campus life">
            <figure className="landing-photo landing-photo-main">
              <img
                src="https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=88"
                alt="Students spending time together on campus"
                fetchPriority="high"
              />
            </figure>
            <figure className="landing-photo landing-photo-books">
              <img
                src="https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=800&q=85"
                alt="Books and notes laid out for studying"
              />
            </figure>
            <figure className="landing-photo landing-photo-bag">
              <img
                src="https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=85"
                alt="A backpack for carrying everyday essentials"
              />
            </figure>
          </div>
        </section>

        <section className="landing-categories" id="marketplace-categories" aria-labelledby="categories-title">
          <div className="landing-container">
            <div className="landing-section-heading">
              <h2 id="categories-title">Three ways to take part.</h2>
              <p>Browse goods, find local services or see what is happening across your campus community.</p>
            </div>

            <div className="landing-tabs" role="tablist" aria-label="Marketplace sections">
              {sections.map((section) => (
                <button
                  key={section.id}
                  id={`landing-tab-${section.id}`}
                  type="button"
                  role="tab"
                  aria-selected={activeSection === section.id}
                  aria-controls="landing-category-panel"
                  tabIndex={activeSection === section.id ? 0 : -1}
                  onClick={() => setActiveSection(section.id)}
                  onKeyDown={moveTab}
                >
                  {section.label}
                </button>
              ))}
            </div>

            <div
              className="landing-category-panel"
              id="landing-category-panel"
              role="tabpanel"
              aria-labelledby={`landing-tab-${activeSection}`}
              tabIndex={0}
            >
              <div className="landing-panel-heading">
                <h3>{selectedSection.title}</h3>
                <Link to={selectedSection.href} className="landing-panel-link">
                  {selectedSection.action} <ArrowRight size={17} aria-hidden="true" />
                </Link>
              </div>
              <div className="landing-category-grid">
                {selectedSection.categories.map((category) => (
                  <article className="landing-category" key={category.name}>
                    <h4 className="landing-category-name">{category.name}</h4>
                    <p>{category.examples}</p>
                  </article>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="landing-discovery landing-container" aria-labelledby="discovery-title">
          <div>
            <h2 id="discovery-title">Find what fits.</h2>
            <p>Filter by condition, price range, seller type and campus. Look for verified sellers nearby.</p>
          </div>
          <div className="landing-discovery-groups">
            <div>
              <h3>Filters</h3>
              <ul className="landing-tags" aria-label="Marketplace filters">
                {['New', 'Like new', 'Used', 'Student', 'Faculty', 'Vendor', 'Resident', 'Verified seller', 'Campus or nearby'].map((filter) => (
                  <li key={filter}>{filter}</li>
                ))}
              </ul>
            </div>
            <div>
              <h3>Collections</h3>
              <ul className="landing-tags" aria-label="Marketplace collections">
                {['Sale', 'New arrivals', 'Best sellers', 'Eco-friendly', 'Second-hand'].map((collection) => (
                  <li key={collection}>{collection}</li>
                ))}
              </ul>
            </div>
          </div>
        </section>

        <section className="landing-how landing-container" id="how-it-works" aria-labelledby="how-title">
          <div className="landing-how-intro">
            <h2 id="how-title">Three steps to get involved.</h2>
            <p>Find what you need, connect with people nearby and take part in campus life.</p>
          </div>
          <ol className="landing-steps">
            {steps.map((step) => (
              <li key={step.title}>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </li>
            ))}
          </ol>
        </section>
      </main>

      <footer className="landing-footer">
        <div className="landing-container landing-footer-inner">
          <Link to="/" className="landing-brand" aria-label="Vendra home">
            <span>vendra<span className="landing-brand-period">.</span></span>
          </Link>
          <p>A marketplace for your campus community.</p>
          <Link to="/marketplace">Browse the marketplace</Link>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
