package com.friction.app

import com.friction.app.accessibility.FrictionAccessibilityService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccessibilityServiceCompanionTest {

    @Before
    fun setUp() {
        // Clear all state before each test
        FrictionAccessibilityService.clearAllAllowedPackages()
    }

    @Test
    fun `allowPackage makes package allowed`() {
        FrictionAccessibilityService.allowPackage("com.test.app")
        assertTrue(FrictionAccessibilityService.isPackageAllowed("com.test.app"))
    }

    @Test
    fun `unallowed package returns false`() {
        assertFalse(FrictionAccessibilityService.isPackageAllowed("com.unknown.app"))
    }

    @Test
    fun `clearAllowedPackage removes specific package`() {
        FrictionAccessibilityService.allowPackage("com.test.app1")
        FrictionAccessibilityService.allowPackage("com.test.app2")

        FrictionAccessibilityService.clearAllowedPackage("com.test.app1")

        assertFalse(FrictionAccessibilityService.isPackageAllowed("com.test.app1"))
        assertTrue(FrictionAccessibilityService.isPackageAllowed("com.test.app2"))
    }

    @Test
    fun `clearAllAllowedPackages removes everything`() {
        FrictionAccessibilityService.allowPackage("com.test.app1")
        FrictionAccessibilityService.allowPackage("com.test.app2")
        FrictionAccessibilityService.allowPackage("com.test.app3")

        FrictionAccessibilityService.clearAllAllowedPackages()

        assertFalse(FrictionAccessibilityService.isPackageAllowed("com.test.app1"))
        assertFalse(FrictionAccessibilityService.isPackageAllowed("com.test.app2"))
        assertFalse(FrictionAccessibilityService.isPackageAllowed("com.test.app3"))
    }

    @Test
    fun `clearing non-existent package does not throw`() {
        // Should not throw
        FrictionAccessibilityService.clearAllowedPackage("com.nonexistent")
    }

    @Test
    fun `multiple allows for same package does not throw`() {
        FrictionAccessibilityService.allowPackage("com.test.app")
        FrictionAccessibilityService.allowPackage("com.test.app")
        assertTrue(FrictionAccessibilityService.isPackageAllowed("com.test.app"))
    }
}
