const CategoryChip = ({ 
  label, 
  isSelected = false, 
  onClick 
}) => {
  return (
    <button
      onClick={onClick}
      aria-pressed={isSelected}
      className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 whitespace-nowrap ${
        isSelected 
          ? 'bg-primary text-white shadow-md shadow-primary/20' 
          : 'bg-white border border-border text-text-primary hover:bg-lavender hover:border-lavender-dark'
      }`}
    >
      {label}
    </button>
  );
};

export default CategoryChip;
