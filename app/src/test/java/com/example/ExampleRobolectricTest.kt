package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.MediaPlatform
import com.example.utils.Formatters
import com.example.utils.UrlCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MORI Downloader", appName)
  }

  @Test
  fun `url cleaner removes tracking parameters`() {
    val dirtyUrl = "https://www.tiktok.com/@user/video/123456789?utm_source=copy&utm_medium=android&igsh=abcdef"
    val cleanUrl = UrlCleaner.clean(dirtyUrl)
    assertEquals("https://www.tiktok.com/@user/video/123456789", cleanUrl)
  }

  @Test
  fun `media platform detection works for supported services`() {
    assertEquals(MediaPlatform.TIKTOK, MediaPlatform.fromUrl("https://vm.tiktok.com/ZM8abc/"))
    assertEquals(MediaPlatform.YOUTUBE, MediaPlatform.fromUrl("https://youtu.be/dQw4w9WgXcQ"))
    assertEquals(MediaPlatform.TWITTER, MediaPlatform.fromUrl("https://x.com/user/status/123456"))
    assertEquals(MediaPlatform.INSTAGRAM, MediaPlatform.fromUrl("https://www.instagram.com/reel/C8xyz/"))
    assertEquals(MediaPlatform.SPOTIFY, MediaPlatform.fromUrl("https://open.spotify.com/track/4cOdK2wGUT"))
    assertEquals(MediaPlatform.BANDCAMP, MediaPlatform.fromUrl("https://artist.bandcamp.com/track/song"))
  }

  @Test
  fun `formatter formats bytes and speed correctly`() {
    assertEquals("1.5 MB", Formatters.formatBytes(1572864))
    assertEquals("2.0 MB/s", Formatters.formatSpeed(2097152))
  }

  @Test
  fun `screen items are non null and have valid routes`() {
    val items = com.example.ui.navigation.Screen.items
    assertEquals(4, items.size)
    items.forEach { screen ->
      org.junit.Assert.assertNotNull(screen)
      org.junit.Assert.assertNotNull(screen.route)
      assertTrue(screen.route.isNotBlank())
    }
  }
}
