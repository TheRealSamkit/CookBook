package com.example.cookbook.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Review data model representing a user rating and comment for a recipe.
 * Stored in 'recipes/{recipeId}/reviews' subcollection.
 */
data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    /**
     * Convert Review to a Map for Firestore storage.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "reviewId" to reviewId,
            "userId" to userId,
            "userName" to userName,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to timestamp
        )
    }

    companion object {
        /**
         * Create a Review from Firestore document data.
         */
        fun fromMap(map: Map<String, Any>): Review {
            return Review(
                reviewId = map["reviewId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toFloat() ?: 0f,
                comment = map["comment"] as? String ?: "",
                timestamp = map["timestamp"] as? Timestamp
            )
        }
    }
}
