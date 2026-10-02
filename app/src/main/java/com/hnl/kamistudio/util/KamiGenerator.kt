package com.hnl.kamistudio.util

import java.security.SecureRandom

object KamiGenerator {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val random = SecureRandom()

    fun generate(prefix: String, length: Int): String {
        val sb = StringBuilder(prefix)
        val bodyLength = length - prefix.length
        repeat(bodyLength) {
            sb.append(CHARS[random.nextInt(CHARS.length)])
        }
        // 每4位加一个分隔符，更易读
        return formatWithDashes(sb.toString())
    }

    fun generateBatch(prefix: String, length: Int, count: Int): List<String> {
        val codes = mutableSetOf<String>()
        while (codes.size < count) {
            codes.add(generate(prefix, length))
        }
        return codes.toList()
    }

    private fun formatWithDashes(code: String): String {
        if (code.length <= 8) return code
        val sb = StringBuilder()
        code.forEachIndexed { index, c ->
            if (index > 0 && index % 4 == 0) sb.append('-')
            sb.append(c)
        }
        return sb.toString()
    }

    fun getTypeLabel(type: String): String = when (type) {
        "daily" -> "日卡"
        "weekly" -> "周卡"
        "monthly" -> "月卡"
        "quarterly" -> "季卡"
        "yearly" -> "年卡"
        "forever" -> "永久卡"
        else -> "自定义"
    }

    fun getTypeDays(type: String, customDays: Int = 0): Int = when (type) {
        "daily" -> 1
        "weekly" -> 7
        "monthly" -> 30
        "quarterly" -> 90
        "yearly" -> 365
        "forever" -> -1
        else -> customDays
    }
}
