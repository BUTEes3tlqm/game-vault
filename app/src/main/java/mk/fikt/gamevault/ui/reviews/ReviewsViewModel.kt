package mk.fikt.gamevault.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.data.repo.ReviewRepository
import mk.fikt.gamevault.di.AppContainer
import java.util.UUID

class ReviewsViewModel(
    private val reviewRepo: ReviewRepository,
    private val gameRepo: GameRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val currentUid: String? = authRepo.currentUser()?.uid

    private fun reviewsFlow(): Flow<List<ReviewEntity>> =
        currentUid?.let { reviewRepo.observeByAuthor(it) } ?: flowOf(emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = reviewsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myGames: StateFlow<List<GameEntity>> = gameRepo.observeAll()
        .map { list -> list.filter { it.status.isReviewable } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteReview(id: String) {
        viewModelScope.launch {
            reviewRepo.deleteReview(id)
        }
    }

    fun postReview(gameTitle: String, rating: Float, text: String, gameId: String? = null) {
        val user = authRepo.currentUser() ?: return
        val review = ReviewEntity(
            id = UUID.randomUUID().toString(),
            gameTitle = gameTitle.trim(),
            gameId = gameId,
            authorUid = user.uid,
            authorName = user.displayName ?: user.email ?: "Guest",
            rating = rating,
            text = text.trim(),
        )
        viewModelScope.launch {
            reviewRepo.postReview(review)
            AppContainer.analytics.logReviewPosted(rating)
        }
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ReviewsViewModel(
                AppContainer.reviewRepository,
                AppContainer.gameRepository,
                AppContainer.authRepository,
            ) as T
        }
    }
}
