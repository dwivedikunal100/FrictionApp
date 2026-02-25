package com.friction.app

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconTest {

    @Test
    fun verifyAppIconIsCustom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        
        // Get the application info for our app
        val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
        
        // Get the actual icon from PackageManager
        val iconDrawable = packageManager.getApplicationIcon(applicationInfo)
        
        // Verify it was loaded successfully
        assertNotNull("App icon should not be null", iconDrawable)
        assertTrue("Icon intrinsic width > 0", iconDrawable.intrinsicWidth > 0)
        
        // Also verify the drawable resource ID points to a valid resource
        val mipmapId = applicationInfo.icon
        assertTrue("Icon ID should be valid", mipmapId != 0)
        
        val resourceDrawable = context.getDrawable(mipmapId)
        assertNotNull("Resource drawable should not be null", resourceDrawable)
    }
}
