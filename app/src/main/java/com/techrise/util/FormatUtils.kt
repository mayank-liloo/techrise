package com.techrise.util

/**
 * Formats email addresses or system IDs into clean, capitalized display names.
 * Example:
 * - "mayank@gmail.com" -> "Mayank"
 * - "admin2@gmail.com" -> "Admin2"
 * - "john.doe@gmail.com" -> "John Doe"
 * - "EMP-1004" -> "EMP 1004"
 */
fun formatEmailToName(email: String?): String {
    if (email.isNullOrBlank()) return "Unassigned"
    val part = email.substringBefore("@")
    return part.split(".", "_", "-")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
