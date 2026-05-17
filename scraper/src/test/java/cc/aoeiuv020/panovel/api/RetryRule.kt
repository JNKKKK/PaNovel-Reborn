package cc.aoeiuv020.panovel.api

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException

class RetryRule(private val maxRetries: Int = 3, private val delayMs: Long = 15_000) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var lastError: Throwable? = null
                for (attempt in 1..maxRetries) {
                    try {
                        base.evaluate()
                        return
                    } catch (e: Throwable) {
                        if (isRetriable(e)) {
                            lastError = e
                            if (attempt < maxRetries) {
                                System.err.println("${description.displayName}: attempt $attempt failed (${e.message}), retrying in ${delayMs}ms...")
                                Thread.sleep(delayMs)
                            }
                        } else {
                            throw e
                        }
                    }
                }
                throw lastError!!
            }
        }
    }

    private fun isRetriable(e: Throwable): Boolean {
        if (e is IOException || e.cause is IOException) return true
        if (e is AssertionError) return true
        return false
    }
}
