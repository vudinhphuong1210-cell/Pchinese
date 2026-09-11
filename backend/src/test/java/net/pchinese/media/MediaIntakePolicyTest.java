package net.pchinese.media;

import net.pchinese.content.api.ContentValidationException;
import net.pchinese.media.application.MediaIntakePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaIntakePolicyTest {

    private final MediaIntakePolicy policy = new MediaIntakePolicy();

    @Test
    void normalizesSupportedYoutubeVideoReferencesToTheCanonicalId() {
        assertCanonical("dQw4w9WgXcQ");
        assertCanonical("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals("0zdaS6qvmWY", policy.youtubeReference(
                "https://www.youtube.com/watch?v=0zdaS6qvmWY&list=RD0zdaS6qvmWY&start_radio=1")
                .providerAssetIdentifier());
        assertCanonical("https://youtu.be/dQw4w9WgXcQ?t=43");
        assertCanonical("https://www.youtube.com/embed/dQw4w9WgXcQ?rel=0");
    }

    @Test
    void rejectsUnsupportedOrAmbiguousReferences() {
        assertRejected("http://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertRejected("https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ");
        assertRejected("https://www.youtube.com/playlist?list=PL123");
        assertRejected("https://www.youtube.com/@PChinese");
        assertRejected("https://www.youtube.com/shorts/dQw4w9WgXcQ");
        assertRejected("https://www.youtube.com/watch?v=dQw4w9WgXcQ&v=dQw4w9WgXcQ");
        assertRejected("https://www.youtube.com/watch?v=too-short");
        assertRejected("<iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\"></iframe>");
        assertRejected("C:\\media\\lesson.mp4");
    }

    private void assertCanonical(String suppliedReference) {
        MediaIntakePolicy.IntakeReference reference = policy.youtubeReference(suppliedReference);

        assertEquals("YOUTUBE", reference.providerName());
        assertEquals("dQw4w9WgXcQ", reference.providerAssetIdentifier());
        assertEquals(null, reference.mimeType());
    }

    private void assertRejected(String suppliedReference) {
        assertThrows(ContentValidationException.class, () -> policy.youtubeReference(suppliedReference));
    }
}
