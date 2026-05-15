package com.hermes.mobile.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageMediaUtilsTest {
    @Test
    fun parsesPersistedImageUrisSafely() {
        val photo = "content://media/picker/0/photo"
        assertEquals(listOf(photo), messageImageUrisFromJson("""["$photo"]"""))
        assertEquals(listOf(photo), messageImageUrisFromJson("""[" $photo "," "]"""))
        assertEquals(listOf(photo), messageImageUrisFromJson("""["$photo"," $photo "]"""))
        assertEquals(listOf(photo), messageImageUrisFromJson("""["not-uri","$photo,"]"""))
        assertEquals(listOf(photo), messageImageUrisFromJson("""["\n $photo\nignored "]"""))
        assertEquals(emptyList<String>(), messageImageUrisFromJson(""))
        assertEquals(emptyList<String>(), messageImageUrisFromJson("not-json"))
    }

    @Test
    fun parsesLegacySingleImageUri() {
        assertEquals(listOf("content://media/picker/0/photo"), messageImageUrisFromJson(" content://media/picker/0/photo "))
        assertEquals(listOf("content://media/picker/0/photo"), messageImageUrisFromJson("content://media/picker/0/photo;"))
        assertEquals(listOf("content://media/picker/0/photo"), messageImageUrisFromJson("content://media/picker/0/photo)"))
        assertEquals(listOf("content://media/picker/0/photo"), messageImageUrisFromJson("content://media/picker/0/photo."))
        assertEquals(listOf("data:image/jpeg;base64,AQID"), messageImageUrisFromJson("data:image/jpeg;base64,AQID"))
        assertEquals(listOf("DATA:IMAGE/jpeg;base64,AQID"), messageImageUrisFromJson("DATA:IMAGE/jpeg;base64,AQID"))
    }

    @Test
    fun legacyImageUrisIgnoreRemoteHttpUrls() {
        val content = """
            Attachments:
            https://tracker.example/pixel.png
            http://host/image.jpg
        """.trimIndent()

        assertEquals(emptyList<String>(), legacyImageUrisFromText(content))
        assertEquals(emptyList<String>(), messageImageUrisFromJson("HTTPS://example.com/photo.jpg"))
    }

    @Test
    fun legacyImageUrisIgnoreArbitraryFileUrls() {
        val content = """
            Attachments:
            file:///sdcard/secret.png
        """.trimIndent()

        assertEquals(emptyList<String>(), legacyImageUrisFromText(content))
        assertEquals(emptyList<String>(), messageImageUrisFromJson("file:///sdcard/secret.png"))
    }

    @Test
    fun legacyImageUrisKeepAppContentAndDataImages() {
        val appPhoto = "content://com.hermes.mobile.fileprovider/hermes_media/hermes-photo-1.jpg"
        val dataPhoto = "data:image/png;base64,AQID"
        val content = """
            Attachments:
            $appPhoto
            $dataPhoto
        """.trimIndent()

        assertEquals(listOf(appPhoto, dataPhoto), legacyImageUrisFromText(content))
    }

    @Test
    fun parsesLegacyAttachmentBlockImageUris() {
        val content = """
            What is in this photo?

            Attachments:
            • image: Photo:
            content://media/picker/0/photo
            content://media/picker/0/photo
        """.trimIndent()

        assertEquals(listOf("content://media/picker/0/photo"), legacyImageUrisFromText(content))
        assertEquals(listOf("content://media/picker/0/photo"), messageImageUrisFromJson(content))
    }

    @Test
    fun parsesSingularLegacyAttachmentBlockImageUris() {
        val content = """
            What is in this photo?

            Attachment	:
            image: content://media/picker/0/photo
        """.trimIndent()

        assertEquals(listOf("content://media/picker/0/photo"), legacyImageUrisFromText(content))
        assertEquals("What is in this photo?", visibleMessageText(content, imageCount = 1))
    }

    @Test
    fun stitchesWrappedLegacyContentImageUris() {
        val content = """
            Photo attached.

            Attachments:
            • image: Photo:
            content://media/picker/0/com
            .android.providers.media.photopicker
            /media/1000108830
        """.trimIndent()

        assertEquals(
            listOf("content://media/picker/0/com.android.providers.media.photopicker/media/1000108830"),
            legacyImageUrisFromText(content),
        )
        assertEquals(
            listOf("content://media/picker/0/com.android.providers.media.photopicker/media/1000108830"),
            messageImageUrisFromJson(content),
        )
    }

    @Test
    fun stitchesWrappedLegacyContentImageUrisAfterTrailingSlash() {
        val content = """
            Photo attached.

            Attachments:
            content://media/picker/0/com.android.providers.media.photopicker/media/
            1000108830
        """.trimIndent()

        assertEquals(
            listOf("content://media/picker/0/com.android.providers.media.photopicker/media/1000108830"),
            legacyImageUrisFromText(content),
        )
    }

    @Test
    fun formatsPhotoSummary() {
        assertEquals("", formatPhotoSummary(-1))
        assertEquals("", formatPhotoSummary(0))
        assertEquals("Photo", formatPhotoSummary(1))
        assertEquals("2 photos", formatPhotoSummary(2))
        assertEquals("", readableMessageText("", imageCount = -1))
    }

    @Test
    fun hidesGeneratedPhotoOnlyPromptWhenImageExists() {
        assertEquals("", visibleMessageText("   ", imageCount = 1))
        assertEquals("", visibleMessageText("Photo attached.", imageCount = 1))
        assertEquals("", visibleMessageText(" photo attached ", imageCount = 2))
        assertEquals("", visibleMessageText("Photo attached.\n\nAttachments:\nimage: content://photo/1", imageCount = 1))
        assertEquals("", visibleMessageText("Attachments:\nimage: content://photo/1", imageCount = 1))
        assertEquals("Caption", visibleMessageText("Caption\n\nAttachments:\nimage: content://photo/1", imageCount = 1))
        assertEquals("Caption", visibleMessageText("Caption\n  Attachments:\nimage: content://photo/1", imageCount = 1))
        assertEquals("Photo attached.", visibleMessageText("Photo attached.", imageCount = 0))
        assertEquals("Caption", visibleMessageText("  Caption  ", imageCount = 1))
        assertEquals("Caption", visibleMessageText("Caption", imageCount = 1))
        assertEquals("Photo", readableMessageText("Photo attached.", imageCount = 1))
    }
}
