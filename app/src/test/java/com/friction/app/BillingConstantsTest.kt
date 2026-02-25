package com.friction.app

import com.friction.app.billing.FrictionBillingManager
import org.junit.Assert.*
import org.junit.Test

class BillingConstantsTest {

    @Test
    fun `monthly product ID matches expected value`() {
        assertEquals("friction_premium_monthly", FrictionBillingManager.PRODUCT_MONTHLY)
    }

    @Test
    fun `annual product ID matches expected value`() {
        assertEquals("friction_premium_annual", FrictionBillingManager.PRODUCT_ANNUAL)
    }

    @Test
    fun `product IDs are distinct`() {
        assertNotEquals(
                FrictionBillingManager.PRODUCT_MONTHLY,
                FrictionBillingManager.PRODUCT_ANNUAL
        )
    }

    @Test
    fun `product IDs follow naming convention`() {
        assertTrue(FrictionBillingManager.PRODUCT_MONTHLY.startsWith("friction_premium_"))
        assertTrue(FrictionBillingManager.PRODUCT_ANNUAL.startsWith("friction_premium_"))
    }
}
