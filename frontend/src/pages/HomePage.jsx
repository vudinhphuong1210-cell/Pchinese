import React from 'react';
import { DailyStreakWidget } from '../features/home/components/DailyStreakWidget.jsx';
import { HomeHeroSection } from '../features/home/components/HomeHeroSection.jsx';
import { HomeLessonRail } from '../features/home/components/HomeLessonRail.jsx';
import { LearningPillarsSection } from '../features/home/components/LearningPillarsSection.jsx';
import { InteractiveDemoSection } from '../features/home/components/InteractiveDemoSection.jsx';
import { CommunityStatsSection } from '../features/home/components/CommunityStatsSection.jsx';
import { PricingComparisonSection } from '../features/home/components/PricingComparisonSection.jsx';
import { FaqAccordionSection } from '../features/home/components/FaqAccordionSection.jsx';
import { HomeFooterCta } from '../features/home/components/HomeFooterCta.jsx';

export function HomePage({ authState, onOpenLesson, onSelectTab, onOpenUpgradeModal, onOpenAuth, isPremiumUser: propIsPremium }) {
  const isAuthenticated = Boolean(authState?.isAuthenticated);
  const isPremiumUser = propIsPremium ?? Boolean(authState?.isPremium || authState?.roles?.includes('PREMIUM') || authState?.planCode === 'PREMIUM');
  const start = () => isAuthenticated ? onSelectTab('catalog') : onOpenAuth('register');
  const explore = () => {
    const el = document.getElementById('learning-pillars');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      window.location.hash = '#learning-pillars';
    }
  };

  return (
    <div className="space-y-12 pb-8 sm:space-y-16" data-testid="home-page">
      <HomeHeroSection isAuthenticated={isAuthenticated} onStart={start} onShadowing={() => onSelectTab('catalog')} onExplore={explore} />
      <DailyStreakWidget isGuest={!isAuthenticated} onRequireAuth={() => onOpenAuth('login')} />
      <HomeLessonRail onOpenLesson={onOpenLesson} onViewAllCatalog={() => onSelectTab('catalog')} />
      <LearningPillarsSection onNavigate={onSelectTab} />
      <InteractiveDemoSection />
      <CommunityStatsSection />
      <PricingComparisonSection onUpgradeClick={onOpenUpgradeModal} isPremiumUser={isPremiumUser} />
      <FaqAccordionSection />
      <HomeFooterCta isAuthenticated={isAuthenticated} onStart={start} />
    </div>
  );
}
