package com.elroi.lemurloop

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * values-he and values-iw must stay byte-identical: MainActivity/LemurLoopApp always resolve
 * the Hebrew locale to tag "iw" (values-iw is what the app actually loads), but values-he is
 * kept as a fallback for contexts the app's locale override doesn't reach (see
 * docs/RTL-LOCALIZATION.md). A translator editing only one folder would silently desync them.
 */
class HebrewResourceParityTest {

    private val resRoot = File("src/main/res")

    @Test
    fun `values-he and values-iw strings xml are identical`() {
        assertEquals(
            File(resRoot, "values-he/strings.xml").readText(),
            File(resRoot, "values-iw/strings.xml").readText()
        )
    }

    @Test
    fun `values-he and values-iw arrays xml are identical`() {
        assertEquals(
            File(resRoot, "values-he/arrays.xml").readText(),
            File(resRoot, "values-iw/arrays.xml").readText()
        )
    }
}
