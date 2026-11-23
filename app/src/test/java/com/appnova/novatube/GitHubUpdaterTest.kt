package com.appnova.novatube

import com.appnova.novatube.updater.GitHubUpdater
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdaterTest {
    @Test
    fun detects_newer_semantic_versions() {
        assertTrue(GitHubUpdater.isNewerVersion("v1.2.0", "1.1.0"))
        assertTrue(GitHubUpdater.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(GitHubUpdater.isNewerVersion("v1.1.1", "1.1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.1.0", "1.1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("1.0.9", "1.1.0"))
    }
}
