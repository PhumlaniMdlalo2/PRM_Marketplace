import { forwardRef } from 'react';

const Button = forwardRef(({ 
  children, 
  variant = 'primary', 
  size = 'md', 
  className = '', 
  disabled = false,
  ...props 
}, ref) => {
  const baseStyles = 'inline-flex items-center justify-center font-semibold rounded-xl transition-all duration-200 active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-primary disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100';
  
  const variants = {
    primary: 'bg-primary text-white hover:bg-primary-hover hover:shadow-lg hover:shadow-primary/25 active:bg-primary-hover',
    secondary: 'bg-transparent border border-border text-text-primary hover:bg-lavender hover:border-lavender-dark active:bg-lavender-dark',
    ghost: 'bg-transparent text-text-secondary hover:text-text-primary hover:bg-lavender active:bg-lavender-dark',
    soft: 'bg-primary-muted text-primary hover:bg-lavender-dark active:bg-lavender-dark',
  };
  
  const sizes = {
    sm: 'px-3 py-1.5 text-sm gap-1.5',
    md: 'px-4 py-2.5 text-base gap-2',
    lg: 'px-6 py-3.5 text-base w-full gap-2',
  };
  
  return (
    <button
      ref={ref}
      className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
});

Button.displayName = 'Button';

export default Button;
