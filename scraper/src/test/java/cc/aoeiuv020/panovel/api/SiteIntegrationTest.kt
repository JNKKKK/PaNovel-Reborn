package cc.aoeiuv020.panovel.api

/**
 * Marker interface for site integration tests that require internet access.
 *
 * Run all site integration tests:
 *   ./gradlew scraper:test -Dtest.integration=true
 *
 * These tests are excluded from normal `./gradlew scraper:test` runs.
 */
interface SiteIntegrationTest
