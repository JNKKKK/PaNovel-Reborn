package cc.aoeiuv020.regex

import java.util.regex.Pattern

fun compileRegex(pattern: String): Regex = Regex(pattern)

fun compilePattern(pattern: String): Pattern = Pattern.compile(pattern)

fun String.pick(pattern: String): List<String> {
    val matcher = Pattern.compile(pattern).matcher(this)
    if (!matcher.find()) return emptyList()
    return (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
}

fun String.pick(pattern: Pattern): List<String> {
    val matcher = pattern.matcher(this)
    if (!matcher.find()) return emptyList()
    return (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
}

fun String.matches(pattern: String): Boolean = Pattern.compile(pattern).matcher(this).matches()
