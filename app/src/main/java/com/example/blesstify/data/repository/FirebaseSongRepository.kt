package com.example.blesstify.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.blesstify.core.error.AppError
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.listArtworkUrl
import com.example.blesstify.domain.repository.SongRepository
import com.example.blesstify.util.ImageLoaderFactory
import com.example.blesstify.util.SongImageUtils
import com.example.blesstify.util.SlugUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class FirebaseSongRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context
) : SongRepository {

    private fun DocumentSnapshot.toSong(): Song? {
        if (!exists()) return null
        return try {
            Song(
                id = id,
                title = getString("title") ?: "",
                artist = getString("artist") ?: "",
                artistId = getString("artistId"),
                artistIds = (get("artistIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                album = getString("album"),
                albumId = getString("albumId"),
                durationSec = getLong("durationSec")?.toInt() ?: 0,
                coverUrl = getString("coverUrl"),
                thumbnailUrl = getString("thumbnailUrl"),
                albumCoverUrl = getString("albumCoverUrl"),
                artistCoverUrl = getString("artistCoverUrl"),
                audioUrl = getString("audioUrl"),
                genre = (get("genre") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                moodTags = (get("moodTags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                energy = getDouble("energy") ?: 0.5,
                ownerId = getString("ownerId") ?: "",
                isPublic = getBoolean("isPublic") ?: false,
                status = getString("status") ?: "draft",
                playCount = getLong("playCount")?.toInt() ?: 0,
                likeCount = getLong("likeCount")?.toInt() ?: 0,
                createdAt = getTimestamp("createdAt"),
                updatedAt = getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e("FirebaseSongRepo", "Failed to parse doc ${id}: ${e.message}")
            null
        }
    }

    private fun preloadSongArtwork(songs: List<Song>) {
        ImageLoaderFactory.preloadUrls(
            context = context,
            urls = songs.map { it.listArtworkUrl() }
        )
    }

    private suspend fun upsertArtist(
        artistId: String,
        artistName: String,
        coverUrl: String?
    ) {
        if (artistId.isBlank() || artistName.isBlank()) return

        val docRef = firestore.collection("artists").document(artistId)
        val existing = docRef.get().await()

        val data = hashMapOf<String, Any?>(
            "name" to artistName,
            "coverUrl" to coverUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!existing.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data, SetOptions.merge()).await()
    }

    private suspend fun upsertAlbum(
        albumId: String,
        albumName: String,
        artistId: String?,
        artistName: String?,
        coverUrl: String?
    ) {
        if (albumId.isBlank() || albumName.isBlank()) return

        val docRef = firestore.collection("albums").document(albumId)
        val existing = docRef.get().await()

        val data = hashMapOf<String, Any?>(
            "name" to albumName,
            "coverUrl" to coverUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!artistId.isNullOrBlank()) data["artistIds"] = listOf(artistId)
        if (!artistName.isNullOrBlank()) data["artistNames"] = listOf(artistName)

        if (!existing.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data, SetOptions.merge()).await()
    }

    override fun getSong(songId: String): Flow<Resource<Song>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection(FirestoreKeys.SONGS).document(songId).get().await()
            val song = snapshot.toSong()
            if (song != null) {
                emit(Resource.Success(song))
            } else {
                emit(Resource.Error(AppError.NotFound))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getSongsByIds(songIds: List<String>): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        if (songIds.isEmpty()) {
            emit(Resource.Success(emptyList()))
            return@flow
        }
        try {
            val songs = songIds
                .distinct()
                .chunked(10)
                .flatMap { chunk ->
                    val snapshots = firestore.collection(FirestoreKeys.SONGS)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    snapshots.documents.mapNotNull { doc -> doc.toSong() }
                }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun addSong(song: Song): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val artistIds = if (song.artistIds.isNotEmpty()) song.artistIds else song.artistId?.let { listOf(it) } ?: emptyList()
            val songMap = hashMapOf<String, Any?>(
                "title" to song.title,
                "artist" to song.artist,
                "artistId" to song.artistId,
                "artistIds" to artistIds,
                "album" to song.album,
                "albumId" to song.albumId,
                "durationSec" to song.durationSec,
                "coverUrl" to song.coverUrl,
                "thumbnailUrl" to song.thumbnailUrl,
                "albumCoverUrl" to song.albumCoverUrl,
                "artistCoverUrl" to song.artistCoverUrl,
                "audioUrl" to song.audioUrl,
                "genre" to song.genre,
                "moodTags" to song.moodTags,
                "energy" to song.energy,
                "ownerId" to song.ownerId,
                "isPublic" to song.isPublic,
                "status" to song.status,
                "playCount" to 0,
                "likeCount" to 0,
                "titleLower" to song.title.lowercase(),
                "artistLower" to song.artist.lowercase(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = firestore.collection(FirestoreKeys.SONGS).add(songMap).await()
            emit(Resource.Success(docRef.id))
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun uploadSong(
        audioUri: Uri,
        audioFileName: String,
        coverUri: Uri?,
        coverFileName: String?,
        song: Song
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        // Step 1: Generate a new song document ID
        val songDocRef = firestore.collection(FirestoreKeys.SONGS).document()
        val newSongId = songDocRef.id

        // Step 2: Create uploadJob with status "uploading"
        val audioPath = "songs/$newSongId/audio/$audioFileName"
        val coverPath = if (coverUri != null && coverFileName != null) "songs/$newSongId/cover/$coverFileName" else null
        val uploadJobMap = hashMapOf<String, Any?>(
            "userId" to song.ownerId,
            "songId" to null,
            "audioPath" to audioPath,
            "coverPath" to coverPath,
            "status" to "uploading",
            "error" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val jobRef = firestore.collection(FirestoreKeys.UPLOAD_JOBS).add(uploadJobMap).await()

        try {
            // Step 3: Upload audio to Storage
            val audioRef = storage.reference.child(audioPath)
            audioRef.putFile(audioUri).await()
            val audioDownloadUrl = audioRef.downloadUrl.await().toString()

            var coverDownloadUrl: String? = null
            var thumbnailDownloadUrl: String? = null
            if (coverUri != null && coverPath != null) {
                val coverRef = storage.reference.child(coverPath)
                coverRef.putFile(coverUri).await()
                coverDownloadUrl = coverRef.downloadUrl.await().toString()

                val thumbnailBytes = SongImageUtils.createThumbnailBytes(context, coverUri)
                if (thumbnailBytes != null) {
                    val thumbnailPath = "songs/$newSongId/cover_thumb/thumb_$coverFileName"
                    val thumbnailRef = storage.reference.child(thumbnailPath)
                    thumbnailRef.putBytes(thumbnailBytes).await()
                    thumbnailDownloadUrl = thumbnailRef.downloadUrl.await().toString()
                }
            }

            // Step 4: Upsert normalized artist/album collections
            val finalArtistId = song.artistId?.takeIf { it.isNotBlank() }
                ?: SlugUtils.slugify(song.artist)

            val finalAlbumId = song.albumId?.takeIf { !it.isNullOrBlank() }
                ?: song.album?.takeIf { it.isNotBlank() }?.let { SlugUtils.slugify("${song.artist}-${it}") }

            runCatching {
                upsertArtist(
                    artistId = finalArtistId,
                    artistName = song.artist,
                    coverUrl = song.artistCoverUrl ?: coverDownloadUrl
                )

                if (!finalAlbumId.isNullOrBlank() && !song.album.isNullOrBlank()) {
                    upsertAlbum(
                        albumId = finalAlbumId,
                        albumName = song.album,
                        artistId = finalArtistId,
                        artistName = song.artist,
                        coverUrl = song.albumCoverUrl ?: coverDownloadUrl
                    )
                }
            }.onFailure { e ->
                Log.w("FirebaseSongRepo", "Upsert album/artist failed (non-fatal): ${e.message}")
            }

            val artistIds = if (song.artistIds.isNotEmpty()) song.artistIds else listOf(finalArtistId)

            // Step 5: Create song document
            val songMap = hashMapOf<String, Any?>(
                "title" to song.title,
                "artist" to song.artist,
                "artistId" to finalArtistId,
                "artistIds" to artistIds,
                "album" to song.album,
                "albumId" to finalAlbumId,
                "audioUrl" to audioDownloadUrl,
                "coverUrl" to coverDownloadUrl,
                "thumbnailUrl" to thumbnailDownloadUrl,
                "albumCoverUrl" to (song.albumCoverUrl ?: coverDownloadUrl),
                "artistCoverUrl" to (song.artistCoverUrl ?: coverDownloadUrl),
                "durationSec" to song.durationSec,
                "genre" to song.genre,
                "moodTags" to song.moodTags,
                "energy" to song.energy,
                "ownerId" to song.ownerId,
                "isPublic" to song.isPublic,
                "status" to song.status,
                "playCount" to 0,
                "likeCount" to 0,
                "titleLower" to song.title.lowercase(),
                "artistLower" to song.artist.lowercase(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            songDocRef.set(songMap).await()

            // Step 6: Update uploadJob to completed
            jobRef.update(
                mapOf(
                    "songId" to newSongId,
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            emit(Resource.Success(newSongId))
        } catch (e: Exception) {
            // On failure, update uploadJob to failed
            try {
                jobRef.update(
                    mapOf(
                        "status" to "failed",
                        "error" to (e.message ?: "Unknown error"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) { /* best effort */ }
            Log.e("FirebaseSongRepo", "Upload failed: ${e.message}", e)
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun searchSongs(query: String, userId: String?): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val queryLower = query.lowercase().trim()
            if (queryLower.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            val baseQuery = if (userId.isNullOrBlank()) {
                firestore.collection(FirestoreKeys.SONGS)
                    .whereEqualTo("isPublic", true)
            } else {
                firestore.collection(FirestoreKeys.SONGS)
                    .where(
                        Filter.or(
                            Filter.equalTo("isPublic", true),
                            Filter.equalTo("ownerId", userId)
                        )
                    )
            }

            val snapshots = baseQuery.get().await()
            val allSongs = snapshots.documents.mapNotNull { doc -> doc.toSong() }
            val songs = allSongs.filter { song ->
                song.title.lowercase().contains(queryLower) ||
                    song.artist.lowercase().contains(queryLower)
            }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            Log.e("FirebaseSongRepo", "Search failed: ${e.message}", e)
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getPublicSongs(limit: Int): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshots = firestore.collection(FirestoreKeys.SONGS)
                .whereEqualTo("isPublic", true)
                .limit(limit.toLong())
                .get()
                .await()
            val songs = snapshots.documents.mapNotNull { it.toSong() }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            Log.e("FirebaseSongRepo", "getPublicSongs failed: ${e.message}", e)
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getUserSongs(userId: String): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshots = firestore.collection(FirestoreKeys.SONGS)
                .whereEqualTo("ownerId", userId)
                .get()
                .await()
            val songs = snapshots.documents.mapNotNull { it.toSong() }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            Log.e("FirebaseSongRepo", "getUserSongs failed: ${e.message}", e)
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getTrendingSongs(limit: Int, monthsBack: Int): Flow<Resource<List<Song>>> = flow {
        val TAG = "RecommendForToday"
        Log.d(TAG, "Starting getTrendingSongs flow. Limit: $limit, MonthsBack: $monthsBack")
        emit(Resource.Loading())
        
        try {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.MONTH, -monthsBack)
            val cutoffTimestamp = Timestamp(calendar.time)
            Log.d(TAG, "Cutoff date for trending: ${calendar.time}")

            // Thử query có điều kiện thời gian (Yêu cầu Index)
            try {
                Log.d(TAG, "Attempting primary query (Public + Recent)...")
                val snapshots = firestore.collection(FirestoreKeys.SONGS)
                    .whereEqualTo("isPublic", true)
                    .whereGreaterThanOrEqualTo("createdAt", cutoffTimestamp)
                    .get()
                    .await()
                
                val songs = snapshots.documents
                    .mapNotNull { it.toSong() }
                    .sortedByDescending { it.playCount + it.likeCount }
                    .take(limit)

                if (songs.isNotEmpty()) {
                    Log.d(TAG, "Primary query successful. Found ${songs.size} trending songs.")
                    preloadSongArtwork(songs)
                    emit(Resource.Success(songs))
                    return@flow
                } else {
                    Log.d(TAG, "Primary query returned empty results.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Primary query failed (likely missing index): ${e.message}")
            }

            // Fallback: Lấy tất cả bài public nếu query trên lỗi hoặc không có bài mới
            Log.d(TAG, "Executing fallback query (All Public songs)...")
            val allPublicSnapshots = firestore.collection(FirestoreKeys.SONGS)
                .whereEqualTo("isPublic", true)
                .limit(50)
                .get()
                .await()
            
            val allSongs = allPublicSnapshots.documents
                .mapNotNull { it.toSong() }
                .sortedByDescending { it.playCount + it.likeCount }
                .take(limit)
                
            Log.d(TAG, "Fallback successful. Found ${allSongs.size} public songs.")
            preloadSongArtwork(allSongs)
            emit(Resource.Success(allSongs))

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL FAILURE in getTrendingSongs: ${e.message}", e)
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getSongsByMood(mood: String): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection(FirestoreKeys.SONGS)
                .whereEqualTo("isPublic", true)
                .whereArrayContains("moodTags", mood)
                .limit(20)
                .get()
                .await()
            val songs = snapshot.documents.mapNotNull { it.toSong() }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    override fun getSongsByGenre(genre: String, userId: String?): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val aliases = buildGenreAliases(genre)
            val publicSongsWithDocs = fetchSongsByGenreAliases(
                aliases = aliases,
                limit = 10,
                ownerId = null,
                isPublic = true
            )
            val ownerSongsWithDocs = if (userId.isNullOrBlank()) {
                emptyList()
            } else {
                fetchSongsByGenreAliases(
                    aliases = aliases,
                    limit = 10,
                    ownerId = userId,
                    isPublic = null
                )
            }
            val songsWithDocs = (publicSongsWithDocs + ownerSongsWithDocs)
                .distinctBy { it.first.id }
                .take(10)
            val songs = songsWithDocs.mapNotNull { (doc, song) ->
                val normalized = normalizeGenres(song.genre)
                if (normalized != song.genre) {
                    doc.reference.update("genre", normalized).await()
                }
                song.copy(genre = normalized)
            }
            preloadSongArtwork(songs)
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unknown(e.message)))
        }
    }

    private suspend fun fetchSongsByGenreAliases(
        aliases: List<String>,
        limit: Int,
        ownerId: String?,
        isPublic: Boolean?
    ): List<Pair<DocumentSnapshot, Song>> {
        if (aliases.isEmpty()) return emptyList()
        val results = LinkedHashMap<String, Pair<DocumentSnapshot, Song>>()

        for (alias in aliases) {
            if (results.size >= limit) break
            var query: Query = firestore.collection(FirestoreKeys.SONGS)
                .whereArrayContains("genre", alias)
            if (isPublic != null) {
                query = query.whereEqualTo("isPublic", isPublic)
            }
            if (!ownerId.isNullOrBlank()) {
                query = query.whereEqualTo("ownerId", ownerId)
            }
            val snapshot = query.limit(limit.toLong()).get().await()
            snapshot.documents.forEach { doc ->
                if (results.size >= limit) return@forEach
                val song = doc.toSong()
                if (song != null) {
                    results.putIfAbsent(doc.id, doc to song)
                }
            }
        }

        return results.values.toList()
    }

    private fun buildGenreAliases(genre: String): List<String> {
        val trimmed = genre.trim()
        if (trimmed.isEmpty()) return emptyList()
        val lower = trimmed.lowercase()
        return when (lower) {
            "hip hop", "hip-hop", "hiphop" -> listOf("Hip Hop", "Hip-Hop", "hip hop", "hip-hop", "HipHop", "hiphop")
            "r&b", "rnb", "r and b", "r'n'b" -> listOf("R&B", "RNB", "RnB", "r&b", "rnb", "r and b")
            "lo-fi", "lo fi", "lofi" -> listOf("Lo-fi", "Lo Fi", "Lofi", "lo-fi", "lo fi", "lofi")
            "k-pop", "kpop" -> listOf("K-pop", "Kpop", "k-pop", "kpop")
            "j-pop", "jpop" -> listOf("J-pop", "Jpop", "j-pop", "jpop")
            "c-pop", "cpop" -> listOf("C-pop", "Cpop", "c-pop", "cpop")
            "edm" -> listOf("EDM", "Edm", "edm")
            else -> listOf(trimmed, lower)
        }
    }

    private fun normalizeGenres(genres: List<String>): List<String> {
        if (genres.isEmpty()) return emptyList()
        return genres.mapNotNull { value ->
            val normalized = value.trim()
            if (normalized.isEmpty()) return@mapNotNull null
            when (normalized.lowercase()) {
                "hip hop", "hip-hop", "hiphop" -> "Hip Hop"
                "r&b", "rnb", "r and b", "r'n'b" -> "R&B"
                "lo-fi", "lo fi", "lofi" -> "Lo-fi"
                "k-pop", "kpop" -> "K-pop"
                "j-pop", "jpop" -> "J-pop"
                "c-pop", "cpop" -> "C-pop"
                "edm" -> "EDM"
                "pop" -> "Pop"
                "rock" -> "Rock"
                "rap" -> "Rap"
                "electronic" -> "Electronic"
                "jazz" -> "Jazz"
                "blues" -> "Blues"
                "classical" -> "Classical"
                "country" -> "Country"
                "folk" -> "Folk"
                "metal" -> "Metal"
                "reggae" -> "Reggae"
                "latin" -> "Latin"
                "phonk" -> "Phonk"
                "other" -> "Other"
                else -> normalized
            }
        }.distinct()
    }

    override fun getFocusSongIds(userId: String): Flow<Resource<List<String>>> = callbackFlow {
        trySend(Resource.Loading())
        val collectionRef = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.FOCUS_SONGS)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(AppError.Unknown(error.message)))
                return@addSnapshotListener
            }
            val ids = snapshot?.documents?.map { it.id } ?: emptyList()
            trySend(Resource.Success(ids))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveFocusSongIds(userId: String, songIds: List<String>): Resource<Unit> {
        return try {
            val collectionRef = firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .collection(FirestoreKeys.FOCUS_SONGS)

            val existingSnapshot = collectionRef.get().await()
            val existingIds = existingSnapshot.documents.map { it.id }.toSet()
            val targetIds = songIds.toSet()

            val batch = firestore.batch()
            val now = FieldValue.serverTimestamp()

            // Remove documents not in the new list.
            existingSnapshot.documents
                .filter { it.id !in targetIds }
                .forEach { batch.delete(it.reference) }

            // Upsert documents for target ids.
            targetIds.forEach { songId ->
                val docRef = collectionRef.document(songId)
                val payload = mapOf(
                    "songId" to songId,
                    "addedAt" to now
                )
                batch.set(docRef, payload, com.google.firebase.firestore.SetOptions.merge())
            }

            val userRef = firestore.collection(FirestoreKeys.USERS).document(userId)
            batch.set(userRef, mapOf("focusInitialized" to true), com.google.firebase.firestore.SetOptions.merge())

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(AppError.Unknown(e.message))
        }
    }

    override suspend fun getFocusInitialized(userId: String): Resource<Boolean> {
        return try {
            val snapshot = firestore.collection(FirestoreKeys.USERS).document(userId).get().await()
            Resource.Success(snapshot.getBoolean("focusInitialized") ?: false)
        } catch (e: Exception) {
            Resource.Error(AppError.Unknown(e.message))
        }
    }
}
