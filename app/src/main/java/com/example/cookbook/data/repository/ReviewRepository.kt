package com.example.cookbook.data.repository

import com.example.cookbook.data.model.Review
import com.example.cookbook.util.Constants
import com.example.cookbook.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling review-related Firestore operations.
 * Manages fetching and adding reviews for recipes.
 */
class ReviewRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Get all reviews for a specific recipe, ordered by timestamp (newest first).
     */
    fun getReviewsForRecipe(recipeId: String): Flow<Result<List<Review>>> = callbackFlow {
        val listener = firestore.collection(Constants.RECIPES_COLLECTION)
            .document(recipeId)
            .collection(Constants.REVIEWS_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Review.fromMap(doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.Success(reviews))
            }

        trySend(Result.Loading)

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Add a new review to a recipe.
     * Uses a transaction to update the recipe's average rating and review count.
     */
    fun addReview(recipeId: String, review: Review): Flow<Result<Unit>> = flow {
        try {
            emit(Result.Loading)

            val recipeRef = firestore.collection(Constants.RECIPES_COLLECTION).document(recipeId)
            val reviewRef = recipeRef.collection(Constants.REVIEWS_SUBCOLLECTION).document()
            
            val finalReview = review.copy(reviewId = reviewRef.id)

            firestore.runTransaction { transaction ->
                // 1. Get current recipe data
                val recipeDoc = transaction.get(recipeRef)
                val currentRating = (recipeDoc.get("averageRating") as? Number)?.toFloat() ?: 0f
                val currentCount = (recipeDoc.get("reviewCount") as? Number)?.toInt() ?: 0

                // 2. Calculate new aggregate values
                val newCount = currentCount + 1
                val newRating = ((currentRating * currentCount) + review.rating) / newCount

                // 3. Update recipe document
                transaction.update(recipeRef, mapOf(
                    "averageRating" to newRating,
                    "reviewCount" to newCount
                ))

                // 4. Add review document
                transaction.set(reviewRef, finalReview.toMap())
            }.await()

            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
