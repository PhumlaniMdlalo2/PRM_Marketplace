const CategoryChip = ({ 
  label, 
  isSelected = false, 
  onClick 
}) => {
  return (
    <button
      onClick={onClick}
      aria-pressed={isSelected}
      className={`px-4 py-2 rounded-md text-sm font-medium transition-all duration-200 active:scale-[0.98] whitespace-nowrap ${
        isSelected 
          ? 'bg-primary-muted text-primary'
          : 'bg-transparent border border-border text-text-primary hover:bg-lavender hover:border-lavender-dark'
      }`}
    >
      {label}
    </button>
  );
};

export default CategoryChip;
