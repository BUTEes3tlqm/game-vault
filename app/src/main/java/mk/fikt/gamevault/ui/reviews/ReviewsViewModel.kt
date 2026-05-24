package mk.fikt.gamevault.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.data.repo.ReviewRepository
import mk.fikt.gamevault.di.AppContainer
import java.util.UUID

class ReviewsViewModel(
    private val reviewRepo: ReviewRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    val reviews: StateFlow<List<ReviewEntity>> = reviewRepo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            return ReviewsViewModel(AppContainer.reviewRepository, AppContainer.authRepository) as T
        }
    }
}
