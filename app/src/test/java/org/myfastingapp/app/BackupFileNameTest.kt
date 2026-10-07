package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.myfastingapp.app.backup.backupFileName
import java.time.LocalDateTime

/** Export filenames carry a yyyyMMdd_HHmm suffix so repeated exports do not clash. */
class BackupFileNameTest {
    @Test
    fun nameCarriesTimestampSuffix() {
        assertEquals(
            "myfastingapp-backup_20261007_0640.json",
            backupFileName(LocalDateTime.of(2026, 10, 7, 6, 40)),
        )
    }

    @Test
    fun singleDigitPartsStayZeroPadded() {
        assertEquals(
            "myfastingapp-backup_20260102_0304.json",
            backupFileName(LocalDateTime.of(2026, 1, 2, 3, 4)),
        )
    }
}
