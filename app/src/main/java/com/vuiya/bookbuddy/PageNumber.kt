package com.vuiya.bookbuddy

/**
 * Represents a page number in decimal notation (e.g., 1, 1.1, 1.2, 2, 2.1)
 * This allows inserting pages between existing pages.
 */
data class PageNumber(val value: String) : Comparable<PageNumber> {

    /**
     * Converts decimal page number to sortable format
     * Example: "1" -> [1], "1.1" -> [1, 1], "2.3.1" -> [2, 3, 1]
     */
    private fun toIntArray(): List<Int> {
        return value.split(".").map { it.toIntOrNull() ?: 0 }
    }

    override fun compareTo(other: PageNumber): Int {
        val thisArray = toIntArray()
        val otherArray = other.toIntArray()

        val maxLength = maxOf(thisArray.size, otherArray.size)
        for (i in 0 until maxLength) {
            val thisVal = thisArray.getOrNull(i) ?: 0
            val otherVal = otherArray.getOrNull(i) ?: 0

            if (thisVal != otherVal) {
                return thisVal.compareTo(otherVal)
            }
        }
        return 0
    }

    /**
     * Generates the next page number after this one
     * Example: 1 -> 2, 1.1 -> 1.2, 2.9 -> 2.10
     */
    fun next(): PageNumber {
        val parts = toIntArray().toMutableList()
        if (parts.isEmpty()) return PageNumber("1")

        parts[parts.lastIndex] = parts.last() + 1
        return PageNumber(parts.joinToString("."))
    }

    /**
     * Creates a page number between this and the next page
     * Example: between 1 and 2 -> 1.1, between 1.1 and 1.2 -> 1.1.1
     */
    fun insertAfter(): PageNumber {
        return PageNumber("$value.1")
    }

    override fun toString(): String = value

    companion object {
        fun fromFileName(fileName: String): PageNumber? {
            // Extract page number from filename like "page_1.txt" or "page_1.1.txt"
            val match = Regex("page_([0-9.]+)\\.txt").find(fileName)
            return match?.groupValues?.get(1)?.let { PageNumber(it) }
        }

        fun toFileName(pageNumber: PageNumber): String {
            return "page_${pageNumber.value}.txt"
        }
    }
}

