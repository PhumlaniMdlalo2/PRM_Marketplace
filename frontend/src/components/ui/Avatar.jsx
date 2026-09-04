const Avatar = ({ 
  src, 
  alt, 
  size = 'md', 
  className = '' 
}) => {
  const sizes = {
    sm: 'w-8 h-8',
    md: 'w-11 h-11',
    lg: 'w-16 h-16',
    xl: 'w-24 h-24',
  };
  
  const radius = size === 'xl' ? 'rounded-[2rem]' : 'rounded-[1.20rem]';
  
  return (
    <div className={`${sizes[size]} ${radius} bg-lavender overflow-hidden flex-shrink-0 ${className}`}>
      {src ? (
        <img src={src} alt={alt} className="w-full h-full object-cover" />
      ) : (
        <div className="w-full h-full flex items-center justify-center text-text-muted font-semibold text-sm">
          {alt?.charAt(0) || '?'}
        </div>
      )}
    </div>
  );
};

export default Avatar;
