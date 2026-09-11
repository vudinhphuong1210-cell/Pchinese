import React from 'react';

const HSK_FILTERS = [
  { id: 'all', label: 'Tất cả cấp độ' },
  { id: '1', label: 'HSK 1' },
  { id: '2', label: 'HSK 2' },
  { id: '3', label: 'HSK 3' },
  { id: '4', label: 'HSK 4' },
  { id: '5', label: 'HSK 5' },
  { id: '6', label: 'HSK 6' },
];

export function HskFilter({ selectedHsk, onSelect }) {
  return (
    <div className="flex flex-wrap gap-1.5 pt-0.5">
      {HSK_FILTERS.map((hsk) => (
        <button
          key={hsk.id}
          type="button"
          onClick={() => onSelect(hsk.id)}
          className={`h-8 px-3 rounded-md text-[11px] font-bold transition-all ${
            selectedHsk === hsk.id
              ? 'bg-primary text-primary-foreground ring-2 ring-primary ring-offset-2 ring-offset-background'
              : 'bg-secondary/30 hover:bg-secondary text-muted-foreground'
          }`}
        >
          {hsk.label}
        </button>
      ))}
    </div>
  );
}
