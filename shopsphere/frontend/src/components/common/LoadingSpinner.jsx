import React from 'react';
import { Loader2 } from 'lucide-react';

export default function LoadingSpinner({ text = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center space-y-3">
      <Loader2 className="w-8 h-8 text-purple-600 animate-spin" />
      {text && <p className="text-xs font-semibold text-slate-500 animate-pulse">{text}</p>}
    </div>
  );
}
