package org.radarbase.connect.upload

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import org.radarbase.connect.upload.UploadSourceTask.Companion.untilNow
import java.time.Duration
import java.time.Instant

class UploadSourceTaskUnitTest {
    @Test
    fun durations() {
        val pastInstant = Instant.now() - Duration.ofSeconds(10)
        assertThat(pastInstant.untilNow().toMillis(), lessThan(0L))

        val futureInstant = Instant.now() + Duration.ofSeconds(10)
        assertThat(futureInstant.untilNow().toMillis(), greaterThan(0L))
    }
}
