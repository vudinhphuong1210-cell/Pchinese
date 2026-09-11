package net.pchinese.media.application;

import net.pchinese.content.api.ContentValidationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates the small, explicit set of YouTube references accepted by F04.
 * The caller keeps only the returned canonical video ID, never the supplied URL.
 */
@Service
public class MediaIntakePolicy {
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Set<String> YOUTUBE_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");

    public IntakeReference youtubeReference(String suppliedReference) {
        if (suppliedReference == null) {
            throw invalidReference();
        }

        String candidate = suppliedReference.trim();
        if (candidate.isEmpty() || candidate.length() > 2048) {
            throw invalidReference();
        }
        if (VIDEO_ID.matcher(candidate).matches()) {
            return canonical(candidate);
        }

        try {
            URI uri = new URI(candidate);
            if (!uri.isAbsolute()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw invalidReference();
            }

            String host = uri.getHost();
            if (host == null) {
                throw invalidReference();
            }
            host = host.toLowerCase(Locale.ROOT);

            String videoId = "youtu.be".equals(host)
                    ? fromShareUri(uri)
                    : YOUTUBE_HOSTS.contains(host) ? fromYoutubeUri(uri) : null;
            if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
                throw invalidReference();
            }
            return canonical(videoId);
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw invalidReference();
        }
    }

    private String fromShareUri(URI uri) {
        return singlePathSegment(uri.getRawPath());
    }

    private String fromYoutubeUri(URI uri) {
        String path = uri.getRawPath();
        if ("/watch".equals(path)) {
            return videoIdFromWatchQuery(uri.getRawQuery());
        }
        if (path != null && path.startsWith("/embed/")) {
            return singlePathSegment(path.substring("/embed".length()));
        }
        return null;
    }

    private String videoIdFromWatchQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String videoId = null;
        for (String part : rawQuery.split("&", -1)) {
            int separator = part.indexOf('=');
            String key = decode(separator < 0 ? part : part.substring(0, separator));
            String value = decode(separator < 0 ? "" : part.substring(separator + 1));
            if ("v".equals(key)) {
                if (videoId != null) {
                    throw invalidReference();
                }
                videoId = value;
            }
        }
        return videoId;
    }

    private String singlePathSegment(String rawPath) {
        if (rawPath == null || !rawPath.startsWith("/")) {
            return null;
        }
        String value = rawPath.substring(1);
        return value.indexOf('/') >= 0 ? null : value;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private IntakeReference canonical(String videoId) {
        return new IntakeReference("YOUTUBE", videoId, null);
    }

    private ContentValidationException invalidReference() {
        return new ContentValidationException("A supported YouTube video URL or 11-character video ID is required.");
    }

    public record IntakeReference(String providerName, String providerAssetIdentifier, String mimeType) { }
}
