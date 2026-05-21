package com.example.blesstify.presentation.ui.song

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.repository.AlbumRepository
import com.example.blesstify.domain.repository.ArtistRepository
import com.example.blesstify.domain.repository.SongRepository
import com.example.blesstify.util.SongImageUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SongEditorUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val didSave: Boolean = false,

    val songId: String = "",
    val originalTitle: String = "",

    val title: String = "",

    // Artist select/create
    val artist: String = "",
    val artistId: String? = null,
    val originalArtist: String = "",
    val originalArtistId: String? = null,
    val artistOptions: List<com.example.blesstify.domain.model.Artist> = emptyList(),

    // Album select/create
    val album: String = "",
    val albumId: String? = null,
    val originalAlbum: String = "",
    val originalAlbumId: String? = null,
    val albumOptions: List<com.example.blesstify.domain.model.Album> = emptyList(),

    val genres: List<String> = listOf("Other"),
    val isPublic: Boolean = false,

    // Cover
    val existingCoverUrl: String? = null,
    val coverUri: Uri? = null,
    val coverFileName: String? = null,

    val canSave: Boolean = false
)

@HiltViewModel
class SongEditorViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SongEditorVM"

        val allowedGenres = listOf(
            "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
            "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
            "C-pop", "Latin", "Phonk", "Lo-fi", "Other"
        )
    }

    private val _uiState = MutableStateFlow(SongEditorUiState())
    val uiState: StateFlow<SongEditorUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "[init] SongEditorViewModel created")
        viewModelScope.launch {
            runCatching {
                val artists = artistRepository.getAllArtists()
                val albums = albumRepository.getAllAlbums()
                _uiState.update { it.copy(artistOptions = artists, albumOptions = albums) }
                Log.d(TAG, "[init] artistOptions=${artists.size} albumOptions=${albums.size}")
            }.onFailure { e ->
                Log.w(TAG, "[init] load albums/artists failed: ${e.message}")
            }
        }
    }

    fun load(songId: String) {
        if (songId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Missing song id")
            return
        }
        Log.d(TAG, "[load] songId=$songId")
        _uiState.value = SongEditorUiState(isLoading = true, songId = songId)

        viewModelScope.launch {
            songRepository.getSong(songId).collectLatest { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        val s = res.data
                        if (s == null) {
                            _uiState.update { it.copy(isLoading = false, error = "Song not found") }
                            return@collectLatest
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                didSave = false,
                                songId = songId,
                                originalTitle = s.title,
                                title = s.title,
                                artist = s.artist,
                                artistId = s.artistId,
                                originalArtist = s.artist,
                                originalArtistId = s.artistId,
                                album = s.album.orEmpty(),
                                albumId = s.albumId,
                                originalAlbum = s.album.orEmpty(),
                                originalAlbumId = s.albumId,
                                genres = if (s.genre.isEmpty()) listOf("Other") else s.genre,
                                isPublic = s.isPublic,
                                existingCoverUrl = s.coverUrl,
                                coverUri = null,
                                coverFileName = null,
                                canSave = s.title.isNotBlank() && s.artist.isNotBlank()
                            )
                        }
                        Log.d(TAG, "[load] loaded title=${s.title}")
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = res.error?.message ?: "Load failed"
                            )
                        }
                        Log.e(TAG, "[load] error=${_uiState.value.error}")
                    }
                }
            }
        }
    }

    fun onTitleChange(v: String) {
        Log.d(TAG, "[input] title len=${v.length}")
        _uiState.update { it.copy(title = v, canSave = v.isNotBlank() && it.artist.isNotBlank()) }
    }

    fun onArtistChange(v: String) {
        Log.d(TAG, "[input] artist=$v")
        _uiState.update { it.copy(artist = v, artistId = null, canSave = it.title.isNotBlank() && v.isNotBlank()) }
    }

    fun onArtistSelected(artistId: String?) {
        val selected = artistId?.let { id -> _uiState.value.artistOptions.firstOrNull { it.id == id } }
        Log.d(TAG, "[select] artistId=$artistId artistName=${selected?.name}")
        _uiState.update {
            it.copy(
                artistId = artistId,
                artist = selected?.name ?: it.artist,
                canSave = it.title.isNotBlank() && (selected?.name ?: it.artist).isNotBlank()
            )
        }
    }

    fun onAlbumChange(v: String) {
        _uiState.update { it.copy(album = v, albumId = null) }
    }

    fun onAlbumSelected(albumId: String?) {
        val selected = albumId?.let { id -> _uiState.value.albumOptions.firstOrNull { it.id == id } }
        Log.d(TAG, "[select] albumId=$albumId albumName=${selected?.name}")
        _uiState.update { it.copy(albumId = albumId, album = selected?.name ?: it.album) }
    }

    fun onGenreToggle(value: String) {
        if (value !in allowedGenres) return
        _uiState.update { state ->
            val current = state.genres.toMutableList()
            if (current.contains(value)) {
                current.remove(value)
                if (current.isEmpty()) current.add("Other")
            } else {
                current.add(value)
            }
            val next = current.distinct()
            Log.d(TAG, "[genre] toggle=$value result=$next")
            state.copy(genres = next)
        }
    }

    fun onPublicChange(v: Boolean) {
        _uiState.update { it.copy(isPublic = v) }
    }

    fun onCoverSelected(uri: Uri, fileName: String) {
        Log.d(TAG, "[cover] selected uri=$uri fileName=$fileName")
        _uiState.update { it.copy(coverUri = uri, coverFileName = fileName) }
    }

    fun removeCoverSelection() {
        Log.d(TAG, "[cover] remove selected cover")
        _uiState.update { it.copy(coverUri = null, coverFileName = null) }
    }

    fun save() {
        val state = _uiState.value
        Log.d(TAG, "[save] click songId=${state.songId} title=${state.title} artist=${state.artist} coverUri=${state.coverUri}")
        if (state.songId.isBlank()) return
        if (state.title.isBlank() || state.artist.isBlank()) {
            _uiState.update { it.copy(error = "Title and Artist are required") }
            Log.w(TAG, "[save] blocked validation title/artist empty")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Upload cover if selected
                var coverUrl: String? = state.existingCoverUrl
                var thumbnailUrl: String? = null

                val coverUri = state.coverUri
                val coverFileName = state.coverFileName
                if (coverUri != null && !coverFileName.isNullOrBlank()) {
                    val coverPath = "songs/${state.songId}/cover/$coverFileName"
                    Log.d(TAG, "[save] cover upload start path=$coverPath uri=$coverUri")

                    val coverRef = storage.reference.child(coverPath)
                    coverRef.putFile(coverUri).await()
                    coverUrl = coverRef.downloadUrl.await().toString()
                    Log.d(TAG, "[save] cover upload ok coverUrl=$coverUrl")

                    val thumbBytes = SongImageUtils.createThumbnailBytes(appContext, coverUri)
                    if (thumbBytes != null) {
                        val thumbPath = "songs/${state.songId}/cover_thumb/thumb_$coverFileName"
                        val thumbRef = storage.reference.child(thumbPath)
                        thumbRef.putBytes(thumbBytes).await()
                        thumbnailUrl = thumbRef.downloadUrl.await().toString()
                        Log.d(TAG, "[save] thumbnail upload ok thumbUrl=$thumbnailUrl")
                    } else {
                        Log.w(TAG, "[save] thumbnail skipped: createThumbnailBytes returned null")
                    }
                }

                // Auto-create new Artist/Album if user typed name but did not select existing ID
                var resolvedArtistId: String? = state.artistId?.takeIf { it.isNotBlank() }
                var resolvedArtistName: String = state.artist.trim()
                if (resolvedArtistId == null && resolvedArtistName.isNotBlank()) {
                    val already = state.artistOptions.firstOrNull { it.name.equals(resolvedArtistName, ignoreCase = true) }
                    if (already != null) {
                        resolvedArtistId = already.id
                        resolvedArtistName = already.name
                        Log.d(TAG, "[artistResolve] matched existing artistId=$resolvedArtistId name=$resolvedArtistName")
                    } else {
                        Log.d(TAG, "[artistCreate] start name=$resolvedArtistName")
                        val newId = artistRepository.createArtist(
                            com.example.blesstify.domain.model.Artist(
                                name = resolvedArtistName,
                                songIds = listOf(state.songId),
                                songCount = 1
                            )
                        )
                        resolvedArtistId = newId
                        Log.d(TAG, "[artistCreate] success artistId=$resolvedArtistId")
                    }
                }

                var resolvedAlbumId: String? = state.albumId?.takeIf { it.isNotBlank() }
                var resolvedAlbumName: String = state.album.trim()
                if (resolvedAlbumId == null && resolvedAlbumName.isNotBlank()) {
                    val already = state.albumOptions.firstOrNull { it.name.equals(resolvedAlbumName, ignoreCase = true) }
                    if (already != null) {
                        resolvedAlbumId = already.id
                        resolvedAlbumName = already.name
                        Log.d(TAG, "[albumResolve] matched existing albumId=$resolvedAlbumId name=$resolvedAlbumName")
                    } else {
                        Log.d(TAG, "[albumCreate] start name=$resolvedAlbumName artistId=$resolvedArtistId")
                        val newId = albumRepository.createAlbum(
                            com.example.blesstify.domain.model.Album(
                                name = resolvedAlbumName,
                                artistIds = resolvedArtistId?.let { listOf(it) } ?: emptyList(),
                                artistNames = if (resolvedArtistName.isNotBlank()) listOf(resolvedArtistName) else emptyList(),
                                songIds = listOf(state.songId),
                                songCount = 1
                            )
                        )
                        resolvedAlbumId = newId
                        Log.d(TAG, "[albumCreate] success albumId=$resolvedAlbumId")
                    }
                }

                val safeGenres = state.genres.filter { it in allowedGenres }.ifEmpty { listOf("Other") }

                val updates = hashMapOf<String, Any?>(
                    "title" to state.title.trim(),
                    "artist" to resolvedArtistName,
                    "artistId" to resolvedArtistId,
                    "album" to resolvedAlbumName.ifBlank { null },
                    "albumId" to resolvedAlbumId,
                    "genre" to safeGenres,
                    "isPublic" to state.isPublic,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (!coverUrl.isNullOrBlank()) {
                    updates["coverUrl"] = coverUrl
                }
                if (!thumbnailUrl.isNullOrBlank()) {
                    updates["thumbnailUrl"] = thumbnailUrl
                }

                Log.d(TAG, "[save] songId=${state.songId} updatesKeys=${updates.keys}")

                firestore.collection("songs")
                    .document(state.songId)
                    .update(updates)
                    .await()

                val oldAlbumId = state.originalAlbumId?.takeIf { it.isNotBlank() }
                val newAlbumId = resolvedAlbumId?.takeIf { it.isNotBlank() }
                val oldAlbumName = state.originalAlbum.trim()
                val newAlbumName = resolvedAlbumName.trim()
                val albumChanged = oldAlbumId != newAlbumId

                val oldArtistId = state.originalArtistId?.takeIf { it.isNotBlank() }
                val newArtistId = resolvedArtistId?.takeIf { it.isNotBlank() }
                val oldArtistName = state.originalArtist.trim()
                val newArtistName = resolvedArtistName.trim()
                val artistChanged = oldArtistId != newArtistId

                // Rename-only behavior: if user keeps same albumId but edits album text, treat as rename album doc
                if (!newAlbumId.isNullOrBlank() && newAlbumName.isNotBlank()) {
                    val selectedAlbumName = state.albumOptions.firstOrNull { it.id == newAlbumId }?.name?.trim().orEmpty()
                    if (selectedAlbumName.isNotBlank() && selectedAlbumName != newAlbumName) {
                        Log.d(TAG, "[albumRename] start albumId=$newAlbumId from=$selectedAlbumName to=$newAlbumName")
                        firestore.collection("albums")
                            .document(newAlbumId)
                            .update(
                                mapOf(
                                    "name" to newAlbumName,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .await()
                        Log.d(TAG, "[albumRename] success albumId=$newAlbumId")
                    }
                }

                // Rename-only behavior: if user keeps same artistId but edits artist text, treat as rename artist doc
                if (!newArtistId.isNullOrBlank() && newArtistName.isNotBlank()) {
                    val selectedArtistName = state.artistOptions.firstOrNull { it.id == newArtistId }?.name?.trim().orEmpty()
                    if (selectedArtistName.isNotBlank() && selectedArtistName != newArtistName) {
                        Log.d(TAG, "[artistRename] start artistId=$newArtistId from=$selectedArtistName to=$newArtistName")
                        firestore.collection("artists")
                            .document(newArtistId)
                            .update(
                                mapOf(
                                    "name" to newArtistName,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .await()
                        Log.d(TAG, "[artistRename] success artistId=$newArtistId")
                    }
                }

                if (albumChanged) {
                    Log.d(
                        TAG,
                        "[albumSync] start songId=${state.songId} oldAlbumId=$oldAlbumId oldAlbumName=$oldAlbumName newAlbumId=$newAlbumId newAlbumName=$newAlbumName"
                    )

                    suspend fun syncAlbum(albumId: String, shouldContainSong: Boolean) {
                        val albumRef = firestore.collection("albums").document(albumId)
                        val snapshot = albumRef.get().await()
                        if (!snapshot.exists()) {
                            Log.w(TAG, "[albumSync] skip missing albumId=$albumId")
                            return
                        }

                        val currentSongIds = snapshot.get("songIds") as? List<*>
                        val nextSongIds = currentSongIds
                            .orEmpty()
                            .mapNotNull { it as? String }
                            .toMutableList()

                        if (shouldContainSong) {
                            if (state.songId !in nextSongIds) nextSongIds.add(state.songId)
                        } else {
                            nextSongIds.removeAll { it == state.songId }
                        }

                        albumRef.update(
                            mapOf(
                                "songIds" to nextSongIds.distinct(),
                                "songCount" to nextSongIds.distinct().size,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        Log.d(TAG, "[albumSync] updated albumId=$albumId contain=$shouldContainSong songCount=${nextSongIds.distinct().size}")
                    }

                    if (!oldAlbumId.isNullOrBlank() && oldAlbumId != newAlbumId) {
                        syncAlbum(oldAlbumId, shouldContainSong = false)
                    }
                    if (!newAlbumId.isNullOrBlank()) {
                        syncAlbum(newAlbumId, shouldContainSong = true)
                    }
                } else {
                    Log.d(TAG, "[albumSync] skipped no album change")
                }

                if (artistChanged) {
                    Log.d(
                        TAG,
                        "[artistSync] start songId=${state.songId} oldArtistId=$oldArtistId oldArtistName=$oldArtistName newArtistId=$newArtistId newArtistName=$newArtistName"
                    )

                    suspend fun syncArtist(artistId: String, shouldContainSong: Boolean) {
                        val artistRef = firestore.collection("artists").document(artistId)
                        val snapshot = artistRef.get().await()
                        if (!snapshot.exists()) {
                            Log.w(TAG, "[artistSync] skip missing artistId=$artistId")
                            return
                        }

                        val currentSongIds = snapshot.get("songIds") as? List<*>
                        val nextSongIds = currentSongIds
                            .orEmpty()
                            .mapNotNull { it as? String }
                            .toMutableList()

                        if (shouldContainSong) {
                            if (state.songId !in nextSongIds) nextSongIds.add(state.songId)
                        } else {
                            nextSongIds.removeAll { it == state.songId }
                        }

                        artistRef.update(
                            mapOf(
                                "songIds" to nextSongIds.distinct(),
                                "songCount" to nextSongIds.distinct().size,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        Log.d(TAG, "[artistSync] updated artistId=$artistId contain=$shouldContainSong songCount=${nextSongIds.distinct().size}")
                    }

                    if (!oldArtistId.isNullOrBlank() && oldArtistId != newArtistId) {
                        syncArtist(oldArtistId, shouldContainSong = false)
                    }
                    if (!newArtistId.isNullOrBlank()) {
                        syncArtist(newArtistId, shouldContainSong = true)
                    }
                } else {
                    Log.d(TAG, "[artistSync] skipped no artist change")
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        didSave = true,
                        originalArtist = state.artist.trim(),
                        originalArtistId = state.artistId,
                        originalAlbum = state.album.trim(),
                        originalAlbumId = state.albumId
                    )
                }
                Log.d(TAG, "[save] success")
            } catch (e: Exception) {
                Log.e(TAG, "[save] failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Save failed: ${e.message}") }
            }
        }
    }
}
