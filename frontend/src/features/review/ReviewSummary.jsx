import React from 'react';

export function ReviewSummary({ stats = { total: 0, again: 0, hard: 0, good: 0, easy: 0 }, onRestart }) {
  return (
    <div className="w-full max-w-lg mx-auto bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 p-8 text-center space-y-6">
      <div className="inline-flex items-center justify-center w-16 h-16 bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-300 rounded-full text-3xl">
        📊
      </div>

      <div className="space-y-1">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Tổng kết phiên ôn tập
        </h2>
        <p className="text-xs text-gray-500 dark:text-gray-400">
          Bạn đã hoàn thành {stats.total} lượt đánh giá từ vựng
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-2 pt-2">
        <div className="p-3 bg-rose-50 dark:bg-rose-950/30 rounded-xl border border-rose-100 dark:border-rose-900">
          <span className="block text-lg font-bold text-rose-700 dark:text-rose-300">{stats.again}</span>
          <span className="text-[10px] font-medium text-rose-600 dark:text-rose-400">Again</span>
        </div>
        <div className="p-3 bg-orange-50 dark:bg-orange-950/30 rounded-xl border border-orange-100 dark:border-orange-900">
          <span className="block text-lg font-bold text-orange-700 dark:text-orange-300">{stats.hard}</span>
          <span className="text-[10px] font-medium text-orange-600 dark:text-orange-400">Hard</span>
        </div>
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/30 rounded-xl border border-emerald-100 dark:border-emerald-900">
          <span className="block text-lg font-bold text-emerald-700 dark:text-emerald-300">{stats.good}</span>
          <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400">Good</span>
        </div>
        <div className="p-3 bg-sky-50 dark:bg-sky-950/30 rounded-xl border border-sky-100 dark:border-sky-900">
          <span className="block text-lg font-bold text-sky-700 dark:text-sky-300">{stats.easy}</span>
          <span className="text-[10px] font-medium text-sky-600 dark:text-sky-400">Easy</span>
        </div>
      </div>

      <div className="pt-4">
        <button
          type="button"
          onClick={onRestart}
          className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition-all hover:shadow-indigo-500/25 active:scale-95"
        >
          Tải phiên ôn tập mới
        </button>
      </div>
    </div>
  );
}
