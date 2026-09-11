import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ContentAdminPage } from './ContentAdminPage.jsx';
import { adminContentApi } from '../../../api/adminContent.js';

jest.mock('../../../api/adminContent.js', () => ({
  adminContentApi: {
    listTopics: jest.fn(),
    listLessons: jest.fn(),
    listSegments: jest.fn(),
    listMedia: jest.fn(),
    createTopic: jest.fn(),
    updateTopic: jest.fn(),
    publishTopic: jest.fn(),
    unpublishTopic: jest.fn(),
    archiveTopic: jest.fn(),
    createLesson: jest.fn(),
    updateLesson: jest.fn(),
    publishLesson: jest.fn(),
    unpublishLesson: jest.fn(),
    archiveLesson: jest.fn(),
    createSegment: jest.fn(),
    updateSegment: jest.fn(),
    publishSegment: jest.fn(),
    unpublishSegment: jest.fn(),
    archiveSegment: jest.fn(),
    createMedia: jest.fn(),
    updateMedia: jest.fn(),
    approveMedia: jest.fn(),
    rejectMedia: jest.fn(),
    quarantineMedia: jest.fn(),
  },
}));

describe('F04 Content & Media Administration UI tests', () => {
  const topicId = '11111111-1111-1111-1111-111111111111';
  const lessonId = '22222222-2222-2222-2222-222222222222';
  const segmentId = '33333333-3333-3333-3333-333333333333';
  const mediaId = '44444444-4444-4444-4444-444444444444';

  const mockTopic = {
    topicId,
    title: 'HSK 1 Stories',
    slug: 'hsk1-stories',
    description: 'Basic stories',
    hskLevel: 1,
    sortOrder: 0,
    publicationState: 'DRAFT',
    version: 0,
  };

  const mockLesson = {
    lessonId,
    topicId,
    title: 'Lesson 1 Hello',
    slug: 'lesson-1-hello',
    summary: 'Greetings',
    hskLevel: 1,
    accessLevel: 'FREE',
    publicationState: 'DRAFT',
    version: 0,
  };

  const mockSegment = {
    segmentId,
    lessonId,
    mediaAssetId: mediaId,
    sequenceNo: 1,
    segmentType: 'BOTH',
    transcriptHanzi: '你好！',
    transcriptPinyin: 'nǐ hǎo!',
    translationVi: 'Xin chào!',
    publicationState: 'DRAFT',
    version: 0,
  };

  const mockMedia = {
    mediaAssetId: mediaId,
    providerName: 'YOUTUBE',
    providerAssetIdentifier: 'dQw4w9WgXcQ',
    title: 'Hello video',
    approvalStatus: 'PENDING_SCAN',
    version: 0,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    adminContentApi.listTopics.mockResolvedValue({ success: true, data: [mockTopic] });
    adminContentApi.listLessons.mockResolvedValue({ success: true, data: [mockLesson] });
    adminContentApi.listSegments.mockResolvedValue({ success: true, data: [mockSegment] });
    adminContentApi.listMedia.mockResolvedValue({ success: true, data: [mockMedia] });
  });

  test('renders subtabs and lists versioned topics', async () => {
    render(<ContentAdminPage />);

    expect(await screen.findByTestId('content-admin-page')).toBeInTheDocument();
    expect(screen.getByTestId('subtab-topics')).toBeInTheDocument();
    expect(screen.getByTestId('subtab-lessons')).toBeInTheDocument();
    expect(screen.getByTestId('subtab-segments')).toBeInTheDocument();
    expect(screen.getByTestId('subtab-media')).toBeInTheDocument();

    expect(await screen.findByTestId(`topic-card-${topicId}`)).toBeInTheDocument();
    expect(screen.getByText('HSK 1 Stories')).toBeInTheDocument();
    expect(screen.getByTestId('publication-status-badge')).toHaveTextContent('DRAFT');
  });

  test('publishes topic when Publish button is clicked', async () => {
    adminContentApi.publishTopic.mockResolvedValueOnce({
      success: true,
      data: { ...mockTopic, publicationState: 'PUBLISHED', version: 1 },
    });

    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('publish-btn'));
    await waitFor(() => {
      expect(adminContentApi.publishTopic).toHaveBeenCalledWith(topicId, 0);
    });
  });

  test('handles 409 STATE_CONFLICT stale version error and displays reload banner', async () => {
    adminContentApi.publishTopic.mockRejectedValueOnce({
      status: 409,
      code: 'STATE_CONFLICT',
      message: 'Stale expectedVersion; resource updated concurrently.',
    });

    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('publish-btn'));

    expect(await screen.findByTestId('stale-conflict-banner')).toBeInTheDocument();
    expect(screen.getByTestId('reload-btn')).toBeInTheDocument();
  });

  test('opens ContentEditor on Create button click', async () => {
    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('create-content-btn'));
    expect(await screen.findByTestId('content-editor-form')).toBeInTheDocument();
  });

  test('renders a safe YouTube thumbnail derived from the canonical video ID', async () => {
    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('subtab-media'));
    const thumbnail = await screen.findByTestId(`youtube-thumbnail-${mediaId}`);
    expect(thumbnail).toHaveAttribute('src', 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg');
    expect(thumbnail).toHaveAttribute('alt', 'YouTube thumbnail for Hello video');
  });

  test('creates media from a YouTube URL or ID without offering a local file upload', async () => {
    adminContentApi.createMedia.mockResolvedValueOnce({ success: true, data: mockMedia });
    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('subtab-media'));
    fireEvent.click(screen.getByTestId('create-content-btn'));

    const reference = await screen.findByTestId('media-youtube-reference-input');
    expect(reference).toHaveAccessibleName('YouTube URL or Video ID');
    expect(screen.getByText(/original URL is not stored/i)).toBeInTheDocument();
    expect(document.querySelector('input[type="file"]')).toBeNull();

    fireEvent.change(reference, { target: { value: 'https://youtu.be/dQw4w9WgXcQ' } });
    fireEvent.change(screen.getByTestId('media-title-input'), { target: { value: 'Greeting video' } });
    fireEvent.submit(screen.getByTestId('content-editor-form'));

    await waitFor(() => expect(adminContentApi.createMedia).toHaveBeenCalledWith({
      youtubeVideoReference: 'https://youtu.be/dQw4w9WgXcQ',
      durationMilliseconds: 120000,
      title: 'Greeting video',
      altText: '',
    }));
    expect(await screen.findByRole('status')).toHaveTextContent('YouTube video registered as ID: dQw4w9WgXcQ');
  });

  test('surfaces the safe API error in the open editor and keeps the editor open', async () => {
    adminContentApi.createMedia.mockRejectedValueOnce({
      status: 400,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'A supported YouTube video URL or 11-character video ID is required.',
      },
      meta: { correlationId: '55555555-5555-4555-8555-555555555555' },
    });
    render(<ContentAdminPage />);
    await screen.findByTestId(`topic-card-${topicId}`);

    fireEvent.click(screen.getByTestId('subtab-media'));
    fireEvent.click(screen.getByTestId('create-content-btn'));
    fireEvent.change(await screen.findByTestId('media-youtube-reference-input'), { target: { value: 'http://youtube.com/watch?v=dQw4w9WgXcQ' } });
    fireEvent.change(screen.getByTestId('media-title-input'), { target: { value: 'Greeting video' } });
    fireEvent.submit(screen.getByTestId('content-editor-form'));

    const alert = await screen.findByTestId('content-editor-server-error');
    expect(alert).toHaveTextContent('VALIDATION_ERROR');
    expect(alert).toHaveTextContent('A supported YouTube video URL or 11-character video ID is required.');
    expect(screen.getByTestId('content-editor-form')).toBeInTheDocument();
    expect(screen.queryByTestId('content-admin-error-banner')).not.toBeInTheDocument();
  });
});
