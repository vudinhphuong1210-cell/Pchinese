import React from 'react';

export function TopicFilter({ topics, selectedTopic, onSelect }) {
  return (
    <div className="flex flex-wrap gap-2">
      <button
        type="button"
        onClick={() => onSelect('all')}
        className={`h-9 px-4 rounded-pill text-xs font-semibold transition-all flex items-center space-x-1.5 ${
          selectedTopic === 'all'
            ? 'bg-primary text-primary-foreground shadow-sm'
            : 'bg-secondary/40 hover:bg-secondary text-foreground border border-border'
        }`}
      >
        <span>Tất cả</span>
      </button>
      {topics.map((topic) => (
        <button
          key={topic.id}
          type="button"
          onClick={() => onSelect(topic.id)}
          className={`h-9 px-4 rounded-pill text-xs font-semibold transition-all flex items-center space-x-1.5 ${
            selectedTopic === topic.id
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'bg-secondary/40 hover:bg-secondary text-foreground border border-border'
          }`}
        >
          <span>{topic.title}</span>
          <span className={`text-[11px] font-medium ${selectedTopic === topic.id ? 'opacity-80' : 'text-muted-foreground'}`}>
            ({topic.publishedLessonCount})
          </span>
        </button>
      ))}
    </div>
  );
}
