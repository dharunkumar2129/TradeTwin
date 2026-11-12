import React from 'react';
import { cn } from '../../lib/utils';
import { AlertCircle, CheckCircle2, Info, AlertTriangle } from 'lucide-react';

export interface AlertProps {
  type?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  children: React.ReactNode;
  className?: string;
}

export const Alert: React.FC<AlertProps> = ({
  type = 'info',
  title,
  children,
  className,
}) => {
  const icons = {
    info: <Info className="w-5 h-5 text-primary-600 shrink-0 mt-0.5" />,
    success: <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />,
    error: <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />,
  };

  const styles = {
    info: 'bg-primary-50/60 border-primary-200/80 text-primary-900',
    success: 'bg-emerald-50/60 border-emerald-200/80 text-emerald-900',
    warning: 'bg-amber-50/60 border-amber-200/80 text-amber-900',
    error: 'bg-rose-50/60 border-rose-200/80 text-rose-900',
  };

  return (
    <div
      className={cn(
        'flex gap-3 p-4 rounded-xl border text-sm',
        styles[type],
        className
      )}
    >
      {icons[type]}
      <div className="flex-1">
        {title && <h5 className="font-semibold mb-1">{title}</h5>}
        <div className="text-xs leading-relaxed opacity-90">{children}</div>
      </div>
    </div>
  );
};
