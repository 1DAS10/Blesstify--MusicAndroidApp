package com.example.blesstify.domain.usecase

import com.example.blesstify.core.utils.Resource

import android.net.Uri
import com.example.blesstify.domain.model.ListeningHistory
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.PlaylistTrack
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.User
import com.example.blesstify.domain.model.SearchHistory
import com.example.blesstify.domain.auth.AuthRepository
import com.example.blesstify.domain.auth.AuthResult
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.auth.AuthUser
import com.example.blesstify.domain.repository.HistoryRepository
import com.example.blesstify.domain.repository.LikesRepository
import com.example.blesstify.domain.repository.PlaylistRepository
import com.example.blesstify.domain.repository.RecommendationRepository
import com.example.blesstify.domain.repository.SearchHistoryRepository
import com.example.blesstify.domain.repository.SongRepository
import com.example.blesstify.domain.repository.UserRepository
import com.example.blesstify.domain.user.UserResult
import com.example.blesstify.domain.model.Recommendation
import com.example.blesstify.core.error.AppError
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class SignInUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, password: String): Flow<AuthResult> =
        authRepository.signIn(email, password)
}

class SignInWithGoogleUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(idToken: String): Flow<AuthResult> =
        authRepository.signInWithGoogle(idToken)
}

class SignUpUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, password: String, displayName: String?): Flow<AuthResult> =
        authRepository.signUp(email, password, displayName)
}

class SignOutUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<AuthResult> = authRepository.signOut()
}

class ObserveAuthStateUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<AuthState> = authRepository.authState()
}

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): AuthUser? = authRepository.currentUser()
}

class CreateUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(user: User): UserResult<Unit> = userRepository.createUser(user)
}

class GetUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String): UserResult<User?> = userRepository.getUser(userId)
}

class UpdateUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
        email: String?,
        locale: String?
    ): UserResult<Unit> = userRepository.updateUser(userId, displayName, avatarUrl, email, locale)
}

class UpdateUserAvatarUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String, imageUri: Uri): UserResult<String> =
        userRepository.updateUserAvatar(userId, imageUri)
}

class CreatePlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlist: Playlist): String =
        playlistRepository.createPlaylist(playlist)
}

class UpdatePlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlist: Playlist) = playlistRepository.updatePlaylist(playlist)
}

class GetPublicPlaylistsUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(limit: Int = 50): List<Playlist> =
        playlistRepository.getPublicPlaylists(limit)
}

class GetLikedPlaylistsUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(userId: String): List<Playlist> =
        playlistRepository.getLikedPlaylists(userId)
}

class GetUserPlaylistsUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(userId: String, limit: Int = 50): List<Playlist> =
        playlistRepository.getUserPlaylists(userId, limit)
}

class AddTrackToPlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String, track: PlaylistTrack) =
        playlistRepository.addTrack(playlistId, track)
}

class RemoveTrackFromPlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String, songId: String) =
        playlistRepository.removeTrack(playlistId, songId)
}

class GetPlaylistTracksUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String): List<PlaylistTrack> =
        playlistRepository.getTracks(playlistId)
}

class GetPlaylistByIdUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String): Playlist? =
        playlistRepository.getPlaylistById(playlistId)
}

class DeletePlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String) =
        playlistRepository.deletePlaylist(playlistId)
}

class UploadPlaylistCoverUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String, uri: Uri): String =
        playlistRepository.uploadPlaylistCover(playlistId, uri)
}

class LikePlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(userId: String, playlistId: String) =
        playlistRepository.likePlaylist(userId, playlistId)
}

class UnlikePlaylistUseCase(private val playlistRepository: PlaylistRepository) {
    suspend operator fun invoke(userId: String, playlistId: String) =
        playlistRepository.unlikePlaylist(userId, playlistId)
}

class IsPlaylistLikedUseCase(private val playlistRepository: PlaylistRepository) {
    operator fun invoke(userId: String, playlistId: String): Flow<Boolean> =
        playlistRepository.isPlaylistLiked(userId, playlistId)
}

class LikeSongUseCase(private val likesRepository: LikesRepository) {
    suspend operator fun invoke(userId: String, songId: String) =
        likesRepository.likeSong(userId, songId)
}

class IsLikedUseCase(private val likesRepository: LikesRepository) {
    operator fun invoke(userId: String, songId: String): Flow<Boolean> =
        likesRepository.isLiked(userId, songId)
}

class UnlikeSongUseCase(private val likesRepository: LikesRepository) {
    suspend operator fun invoke(userId: String, songId: String) =
        likesRepository.unlikeSong(userId, songId)
}

class GetLikedSongIdsUseCase(private val likesRepository: LikesRepository) {
    suspend operator fun invoke(userId: String): List<String> =
        likesRepository.getLikedSongIds(userId)
}

class LogListeningHistoryUseCase(private val historyRepository: HistoryRepository) {
    suspend operator fun invoke(userId: String, item: ListeningHistory) =
        historyRepository.logHistory(userId, item)
}

class GetListeningHistoryUseCase(private val historyRepository: HistoryRepository) {
    suspend operator fun invoke(userId: String, limit: Int = 100): List<ListeningHistory> =
        historyRepository.getHistory(userId, limit)
}

class ObserveListeningHistoryUseCase(private val historyRepository: HistoryRepository) {
    operator fun invoke(userId: String, limit: Int = 100): kotlinx.coroutines.flow.Flow<List<ListeningHistory>> =
        historyRepository.observeHistory(userId, limit)
}

class ClearListeningHistoryUseCase(private val historyRepository: HistoryRepository) {
    suspend operator fun invoke(userId: String) = historyRepository.clearHistory(userId)
}

class GetLatestRecommendationUseCase(private val recommendationRepository: RecommendationRepository) {
    suspend operator fun invoke(userId: String) = recommendationRepository.getLatestRecommendation(userId)
}

class GetSongUseCase(private val songRepository: SongRepository) {
    operator fun invoke(songId: String): Flow<Resource<Song>> = songRepository.getSong(songId)
}

class GetSongsByIdsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(songIds: List<String>): Flow<Resource<List<Song>>> = songRepository.getSongsByIds(songIds)
}

class GetPublicSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(limit: Int = 10): Flow<Resource<List<Song>>> = songRepository.getPublicSongs(limit)
}

class GetUserSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(userId: String): Flow<Resource<List<Song>>> = songRepository.getUserSongs(userId)
}

class GetTrendingSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(limit: Int = 10, monthsBack: Int = 2): Flow<Resource<List<Song>>> =
        songRepository.getTrendingSongs(limit, monthsBack)
}

class AddSongUseCase(private val songRepository: SongRepository) {
    operator fun invoke(song: Song): Flow<Resource<String>> = songRepository.addSong(song)
}

class SearchSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(query: String, userId: String? = null): Flow<Resource<List<Song>>> =
        songRepository.searchSongs(query, userId)
}

class GetSongsByGenreUseCase(private val songRepository: SongRepository) {
    operator fun invoke(genre: String, userId: String? = null): Flow<Resource<List<Song>>> =
        songRepository.getSongsByGenre(genre, userId)
}

class UploadSongUseCase(private val songRepository: SongRepository) {
    operator fun invoke(
        audioUri: Uri,
        audioFileName: String,
        coverUri: Uri?,
        coverFileName: String?,
        song: Song
    ): Flow<Resource<String>> = songRepository.uploadSong(audioUri, audioFileName, coverUri, coverFileName, song)
}

// Search History use cases
class SaveSearchHistoryUseCase(private val searchHistoryRepository: SearchHistoryRepository) {
    suspend operator fun invoke(userId: String, query: String) =
        searchHistoryRepository.saveSearch(userId, query)
}

class GetRecentSearchesUseCase(private val searchHistoryRepository: SearchHistoryRepository) {
    suspend operator fun invoke(userId: String, limit: Int = 10): List<SearchHistory> =
        searchHistoryRepository.getRecentSearches(userId, limit)
}

class DeleteSearchHistoryUseCase(private val searchHistoryRepository: SearchHistoryRepository) {
    suspend operator fun invoke(userId: String, searchId: String) =
        searchHistoryRepository.deleteSearch(userId, searchId)
}

class ClearSearchHistoryUseCase(private val searchHistoryRepository: SearchHistoryRepository) {
    suspend operator fun invoke(userId: String) =
        searchHistoryRepository.clearAll(userId)
}

class GenerateMoodPlaylistUseCase(
    private val songRepository: SongRepository,
    private val recommendationRepository: RecommendationRepository
) {
    operator fun invoke(userId: String, mood: String, energy: Double): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            songRepository.getSongsByMood(mood).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val allSongs = resource.data ?: emptyList()
                        // Filter by energy tolerance (0.3)
                        val filteredSongs = allSongs.filter { abs(it.energy - energy) < 0.3 }
                        
                        // Shuffle and limit (10-20)
                        val shuffledSongs = filteredSongs.shuffled()
                        val resultSongs = if (shuffledSongs.size > 20) shuffledSongs.take(20) else shuffledSongs
                        
                        if (resultSongs.isNotEmpty()) {
                            // Save recommendation
                            val recommendation = Recommendation(
                                userId = userId,
                                type = "mood",
                                mood = mood,
                                energy = energy,
                                songIds = resultSongs.map { it.id }
                            )
                            recommendationRepository.saveRecommendation(recommendation)
                        }
                        
                        emit(Resource.Success(resultSongs))
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(resource.error ?: AppError.Unknown("Unknown error")))
                    }
                    is Resource.Loading -> {
                        emit(Resource.Loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }
}

class GetEqualizerSettingsUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String) = userRepository.getEqualizerSettings(userId)
}

class SaveEqualizerSettingsUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String, settings: com.example.blesstify.domain.model.EqualizerSettings) =
        userRepository.saveEqualizerSettings(userId, settings)
}

class GetNotificationsUseCase(private val repository: com.example.blesstify.domain.repository.NotificationRepository) {
    operator fun invoke(userId: String) = repository.getNotifications(userId)
}

class MarkNotificationAsReadUseCase(private val repository: com.example.blesstify.domain.repository.NotificationRepository) {
    suspend operator fun invoke(notificationId: String) = repository.markAsRead(notificationId)
}

class GetUnreadCountUseCase(private val repository: com.example.blesstify.domain.repository.NotificationRepository) {
    operator fun invoke(userId: String) = repository.getUnreadCount(userId)
}

class GetSubscriptionUseCase(private val repository: com.example.blesstify.domain.repository.SubscriptionRepository) {
    suspend operator fun invoke(userId: String) = repository.getSubscription(userId)
}
