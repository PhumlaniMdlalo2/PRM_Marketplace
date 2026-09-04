import { useState } from 'react';
import { ChevronRight } from 'lucide-react';
import StatusBadge from '../components/ui/StatusBadge';
import EmptyState from '../components/ui/EmptyState';
import { PackageOpen } from 'lucide-react';
import Layout from '../components/layout/Layout';

const Orders = () => {
  const [activeTab, setActiveTab] = useState('pending');
  
  const tabs = [
    { id: 'pending', label: 'Pending' },
    { id: 'completed', label: 'Completed' },
    { id: 'cancelled', label: 'Cancelling' },
  ];
  
  const orders = [
    {
      id: 1,
      name: 'Golden ankle heels',
      price: 'R780',
      date: 'Sep 2, 2026',
      status: 'completed',
    },
    {
      id: 2,
      name: 'Ceramic vases (set of 3)',
      price: 'R340',
      date: 'Sep 1, 2026',
      status: 'pending',
    },
    {
      id: 3,
      name: 'Beige squared short boot',
      price: 'R650',
      date: 'Aug 30, 2026',
      status: 'cancelled',
    },
  ];
  
  const filteredOrders = orders.filter(
    (order) => order.status === activeTab
  );
  
  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight">Your orders</h1>
        <p className="text-text-secondary mt-1">{orders.length} total orders</p>
        
        <div className="mt-6 flex gap-2 border-b border-border pb-3">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                activeTab === tab.id
                  ? 'bg-primary text-white shadow-md shadow-primary/20'
                  : 'bg-lavender text-text-secondary hover:bg-lavender-dark'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        
        <div className="mt-5 space-y-3">
          {filteredOrders.length === 0 ? (
            <EmptyState
              icon={PackageOpen}
              title={`No ${activeTab} orders`}
              description="When you place an order, it will show up here by status."
            />
          ) : (
            filteredOrders.map((order) => (
              <button
                key={order.id}
                className="w-full bg-white border border-border rounded-2xl p-4 flex items-center gap-4 hover:border-lavender-dark hover:shadow-md hover:shadow-primary/5 active:scale-[0.99] transition-all duration-200 text-left"
              >
                <span className="w-14 h-14 bg-lavender rounded-xl flex-shrink-0"></span>
                <span className="flex-1 min-w-0">
                  <span className="block font-semibold text-text-primary truncate">{order.name}</span>
                  <span className="block text-sm text-text-secondary mt-0.5">{order.price}</span>
                  <span className="block text-xs text-text-muted mt-0.5">{order.date}</span>
                </span>
                <span className="flex flex-col items-end gap-2">
                  <StatusBadge status={order.status} />
                  <ChevronRight size={18} className="text-text-muted" />
                </span>
              </button>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Orders;
