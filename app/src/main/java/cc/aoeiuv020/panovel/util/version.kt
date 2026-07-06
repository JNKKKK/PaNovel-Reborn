package cc.aoeiuv020.panovel.util

/**
 * app版本名相关的封装，
 */

class VersionName(
        val name: String
) : Comparable<VersionName> {
    override fun compareTo(other: VersionName): Int {
        return VersionUtil.compare(name, other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is VersionName) {
            return false
        }
        return VersionUtil.compare(name, other.name) == 0
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return name
    }
}
