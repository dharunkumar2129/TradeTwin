import React from 'react';
import { cn } from '../../lib/utils';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  hoverable?: boolean;
}

export const Card: React.FC<CardProps> = ({
  children,
  className,
  hoverable = false,
  ...props
}) => {
  return (
    <div
      className={cn(
        'bg-surface rounded-xl border border-charcoal-200/80 shadow-subtle p-5 transition-all duration-200',
        hoverable && 'hover:border-charcoal-300 hover:shadow-card cursor-pointer',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};
