package com.elroi.lemurloop.ui.screen.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmCreationWizardTest {

    @Test
    fun `previousWizardPage from first page stays at zero`() {
        assertEquals(0, previousWizardPage(0))
    }

    @Test
    fun `previousWizardPage from second page goes to first`() {
        assertEquals(0, previousWizardPage(1))
    }

    @Test
    fun `previousWizardPage from later page decrements by one`() {
        assertEquals(2, previousWizardPage(3))
    }
}

