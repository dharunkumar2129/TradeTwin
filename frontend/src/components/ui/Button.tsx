import React from 'react';
import { cn } from '../../lib/utils';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  className,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  leftIcon,
  rightIcon,
  ...props
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-medium transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg active:scale-[0.98]';

  const variants = {
    primary: 'bg-primary-600 hover:bg-primary-700 text-white shadow-subtle hover:shadow-card focus:ring-primary-500 border border-transparent',
    secondary: 'bg-charcoal-100 hover:bg-charcoal-200 text-charcoal-800 focus:ring-charcoal-400 border border-charcoal-200',
    outline: 'bg-transparent hover:bg-charcoal-50 text-charcoal-700 border border-charcoal-300 focus:ring-primary-500',
    ghost: 'bg-transparent hover:bg-charcoal-100 text-charcoal-600 hover:text-charcoal-900 border border-transparent',
    danger: 'bg-red-600 hover:bg-red-700 text-white focus:ring-red-500 shadow-subtle',
  };

  const sizes = {
    sm: 'text-xs px-2.5 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2 gap-2',
    lg: 'text-base px-5 py-2.5 gap-2.5',
  };

  return (
    <button
      className={cn(baseStyles, variants[variant], sizes[size], className)}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : (
        leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>
      )}
      {children}
      {!isLoading && rightIcon && <span className="inline-flex shrink-0">{rightIcon}</span>}
    </button>
  );
};
