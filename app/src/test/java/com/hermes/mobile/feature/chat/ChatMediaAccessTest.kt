package com.hermes.mobile.feature.chat

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatMediaAccessTest {
    @Test
    fun onlyExternalContentUrisRequestPersistableReadAccess() {
        assertTrue(shouldPersistPickedPhotoAccess("content://media/picker/0/photo"))
        assertTrue(shouldPersistPickedPhotoAccess("CONTENT://media/picker/0/photo"))
        assertFalse(shouldPersistPickedPhotoAccess("content://com.hermes.mobile.fileprovider/hermes-media/photo.jpg"))
        assertFalse(shouldPersistPickedPhotoAccess("content://"))
        assertFalse(shouldPersistPickedPhotoAccess("file:///tmp/photo.jpg"))
        assertFalse(shouldPersistPickedPhotoAccess("data:image/jpeg;base64,AQID"))
        assertFalse(shouldPersistPickedPhotoAccess("https://example.com/photo.jpg"))
    }
}
