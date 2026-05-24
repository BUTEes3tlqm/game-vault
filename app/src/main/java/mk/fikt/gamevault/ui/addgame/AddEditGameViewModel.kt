package mk.fikt.gamevault.ui.addgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.model.GamePlatform
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.di.AppContainer
import java.util.UUID

class AddEditGameViewModel(private val repo: GameRepository) : ViewModel() {

    data class FormState(
        val id: String? = null,
        val title: String = "",
        val platform: GamePlatform = GamePlatform.PC,
        val releaseYear: String = "",
        val genre: String = "",
        val status: GameStatus = GameStatus.BACKLOG,
        val personalRating: Float = 0f,
        val hoursPlayed: String = "",
        val progressPercent: Int = 0,
        val notes: String = "",
        val coverUri: String? = null,
    )

    sealed class Event {
        data object Saved : Event()
        data object Deleted : Event()
        data class ValidationFailed(val field: Field) : Event()
        enum class Field { TITLE, YEAR }
    }

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    fun load(gameId: String?) {
        if (gameId.isNullOrBlank()) return
        viewModelScope.launch {
            repo.getById(gameId)?.let { g ->
                _form.value = FormState(
                    id = g.id,
                    title = g.title,
                    platform = g.platform,
                    releaseYear = g.releaseYear?.toString().orEmpty(),
                    genre = g.genre.orEmpty(),
                    status = g.status,
                    personalRating = g.personalRating,
                    hoursPlayed = if (g.hoursPlayed > 0) g.hoursPlayed.toString() else "",
                    progressPercent = g.progressPercent,
                    notes = g.notes.orEmpty(),
                    coverUri = g.coverUri,
                )
            }
        }
    }

    fun updateTitle(v: String) = _form.update { it.copy(title = v) }
    fun updatePlatform(v: GamePlatform) = _form.update { it.copy(platform = v) }
    fun updateYear(v: String) = _form.update { it.copy(releaseYear = v) }
    fun updateGenre(v: String) = _form.update { it.copy(genre = v) }
    fun updateStatus(v: GameStatus) = _form.update { it.copy(status = v) }
    fun updateRating(v: Float) = _form.update { it.copy(personalRating = v) }
    fun updateHours(v: String) = _form.update { it.copy(hoursPlayed = v) }
    fun updateProgress(v: Int) = _form.update { it.copy(progressPercent = v) }
    fun updateNotes(v: String) = _form.update { it.copy(notes = v) }
    fun updateCoverUri(uri: String?) = _form.update { it.copy(coverUri = uri) }
    fun consumeEvent() { _events.value = null }

    fun save() {
        val f = _form.value
        if (f.title.isBlank()) {
            _events.value = Event.ValidationFailed(Event.Field.TITLE)
            return
        }
        val year = if (f.releaseYear.isBlank()) null
        else f.releaseYear.toIntOrNull()?.takeIf { it in 1950..2100 }
            ?: return run { _events.value = Event.ValidationFailed(Event.Field.YEAR) }

        val hours = f.hoursPlayed.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val uid = AppContainer.authRepository.currentUser()?.uid.orEmpty()

        val isNew = f.id == null
        viewModelScope.launch {
            repo.upsert(
                GameEntity(
                    id = f.id ?: UUID.randomUUID().toString(),
                    ownerUid = uid,
                    title = f.title.trim(),
                    platform = f.platform,
                    releaseYear = year,
                    genre = f.genre.trim().takeIf { it.isNotBlank() },
                    coverUri = f.coverUri,
                    status = f.status,
                    hoursPlayed = hours,
                    personalRating = f.personalRating,
                    progressPercent = f.progressPercent.coerceIn(0, 100),
                    notes = f.notes.trim().takeIf { it.isNotBlank() },
                )
            )
            if (isNew) AppContainer.analytics.logGameAdded(f.status.name)
            else AppContainer.analytics.logGameStatusChanged(f.status.name)
            _events.value = Event.Saved
        }
    }

    fun delete() {
        val id = _form.value.id ?: return
        viewModelScope.launch {
            repo.delete(id)
            _events.value = Event.Deleted
        }
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AddEditGameViewModel(AppContainer.gameRepository) as T
        }
    }
}
