import React, { forwardRef } from 'react';
import { cn } from '../../lib/utils';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({
  label,
  error,
  hint,
  leftIcon,
  className,
  id,
  ...props
}, ref) => {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={inputId} className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
          {label}
        </label>
      )}
      <div className="relative rounded-lg shadow-subtle">
        {leftIcon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-charcoal-400">
            {leftIcon}
          </div>
        )}
        <input
          id={inputId}
          ref={ref}
          className={cn(
            'block w-full rounded-lg border bg-white px-3.5 py-2 text-sm text-charcoal-900 placeholder:text-charcoal-400 transition duration-150',
            'border-charcoal-200 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
            leftIcon && 'pl-9',
            error && 'border-red-400 focus:border-red-500 focus:ring-red-500/20',
            className
          )}
          {...props}
        />
      </div>
      {error && <p className="mt-1.5 text-xs text-red-600 font-medium">{error}</p>}
      {hint && !error && <p className="mt-1 text-xs text-charcoal-500">{hint}</p>}
    </div>
  );
});

Input.displayName = 'Input';
