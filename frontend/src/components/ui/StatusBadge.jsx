const statusStyles = {
  pending: 'bg-[#E8A838]/10 text-[#E8A838]',
  completed: 'bg-[#34B37A]/10 text-[#34B37A]',
  cancelled: 'bg-[#E05B6A]/10 text-[#E05B6A]',
};

const StatusBadge = ({ status }) => {
  const normalized = (status || '').toLowerCase();
  const style = statusStyles[normalized] || statusStyles.pending;
  
  return (
    <span className={`px-2.5 py-1 rounded-md text-xs font-semibold ${style} inline-flex items-center gap-1`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current" aria-hidden="true" />
      {status.charAt(0).toUpperCase() + status.slice(1)}
    </span>
  );
};

export default StatusBadge;
