package com.example.blesstify.data.di

import com.example.blesstify.domain.repository.UserRepository
import com.example.blesstify.data.repository.FirebaseUserRepository
import com.example.blesstify.data.repository.FirebaseSongRepository
import com.example.blesstify.data.repository.FirebasePlaylistRepository
import com.example.blesstify.data.repository.FirebaseSearchHistoryRepository
import com.example.blesstify.data.repository.FirebaseLikesRepository
import com.example.blesstify.domain.repository.SongRepository
import com.example.blesstify.domain.repository.PlaylistRepository
import com.example.blesstify.domain.repository.SearchHistoryRepository
import com.example.blesstify.domain.repository.LikesRepository
import com.example.blesstify.domain.repository.HistoryRepository
import com.example.blesstify.domain.repository.RecommendationRepository
import com.example.blesstify.data.repository.FirebaseHistoryRepository
import com.example.blesstify.data.repository.FirebaseRecommendationRepository
import com.example.blesstify.data.repository.FirebaseNotificationRepository
import com.example.blesstify.data.repository.FirebaseSubscriptionRepository
import com.example.blesstify.domain.repository.NotificationRepository
import com.example.blesstify.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.blesstify.domain.usecase.CreateUserUseCase
import com.example.blesstify.domain.usecase.GetUserUseCase
import com.example.blesstify.domain.usecase.UpdateUserAvatarUseCase
import com.example.blesstify.domain.usecase.UpdateUserUseCase
import com.example.blesstify.domain.usecase.AddSongUseCase
import com.example.blesstify.domain.usecase.SearchSongsUseCase
import com.example.blesstify.domain.usecase.UploadSongUseCase
import com.example.blesstify.domain.usecase.CreatePlaylistUseCase
import com.example.blesstify.domain.usecase.UpdatePlaylistUseCase
import com.example.blesstify.domain.usecase.GetPublicPlaylistsUseCase
import com.example.blesstify.domain.usecase.GetUserPlaylistsUseCase
import com.example.blesstify.domain.usecase.GetPlaylistByIdUseCase
import com.example.blesstify.domain.usecase.DeletePlaylistUseCase
import com.example.blesstify.domain.usecase.UploadPlaylistCoverUseCase
import com.example.blesstify.domain.usecase.AddTrackToPlaylistUseCase
import com.example.blesstify.domain.usecase.RemoveTrackFromPlaylistUseCase
import com.example.blesstify.domain.usecase.GetPlaylistTracksUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.GetPublicSongsUseCase
import com.example.blesstify.domain.usecase.GetSongsByGenreUseCase
import com.example.blesstify.domain.usecase.SaveSearchHistoryUseCase
import com.example.blesstify.domain.usecase.GetRecentSearchesUseCase
import com.example.blesstify.domain.usecase.DeleteSearchHistoryUseCase
import com.example.blesstify.domain.usecase.ClearSearchHistoryUseCase
import com.example.blesstify.domain.usecase.LikeSongUseCase
import com.example.blesstify.domain.usecase.UnlikeSongUseCase
import com.example.blesstify.domain.usecase.IsLikedUseCase
import com.example.blesstify.domain.usecase.GetLikedSongIdsUseCase
import com.example.blesstify.domain.usecase.LogListeningHistoryUseCase
import com.example.blesstify.domain.usecase.GetListeningHistoryUseCase
import com.example.blesstify.domain.usecase.ClearListeningHistoryUseCase
import com.example.blesstify.domain.usecase.LikePlaylistUseCase
import com.example.blesstify.domain.usecase.UnlikePlaylistUseCase
import com.example.blesstify.domain.usecase.IsPlaylistLikedUseCase
import com.example.blesstify.domain.usecase.GetLatestRecommendationUseCase
import com.example.blesstify.domain.usecase.GetNotificationsUseCase
import com.example.blesstify.domain.usecase.MarkNotificationAsReadUseCase
import com.example.blesstify.domain.usecase.GetUnreadCountUseCase
import com.example.blesstify.domain.usecase.GetSubscriptionUseCase
import com.example.blesstify.domain.usecase.GetRecentPlaylistsUseCase
import com.example.blesstify.domain.usecase.GenerateMoodPlaylistUseCase
import com.example.blesstify.domain.usecase.GetEqualizerSettingsUseCase
import com.example.blesstify.domain.usecase.SaveEqualizerSettingsUseCase

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): UserRepository = FirebaseUserRepository(firestore, storage)

    @Provides
    @Singleton
    fun provideSongRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): SongRepository = FirebaseSongRepository(firestore, storage)

    // Provides the main playlist repository implementation
    @Provides
    @Singleton
    fun providePlaylistRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): PlaylistRepository = FirebasePlaylistRepository(firestore, storage)

    @Provides
    @Singleton
    fun provideSearchHistoryRepository(
        firestore: FirebaseFirestore
    ): SearchHistoryRepository = FirebaseSearchHistoryRepository(firestore)

    @Provides
    @Singleton
    fun provideLikesRepository(
        firestore: FirebaseFirestore
    ): LikesRepository = FirebaseLikesRepository(firestore)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        firestore: FirebaseFirestore
    ): HistoryRepository = FirebaseHistoryRepository(firestore)

    @Provides
    @Singleton
    fun provideRecommendationRepository(
        firestore: FirebaseFirestore
    ): RecommendationRepository = FirebaseRecommendationRepository(firestore)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore
    ): NotificationRepository = FirebaseNotificationRepository(firestore)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        firestore: FirebaseFirestore
    ): SubscriptionRepository = FirebaseSubscriptionRepository(firestore)

    // User use cases
    @Provides
    fun provideCreateUserUseCase(userRepository: UserRepository): CreateUserUseCase =
        CreateUserUseCase(userRepository)

    @Provides
    fun provideGetUserUseCase(userRepository: UserRepository): GetUserUseCase =
        GetUserUseCase(userRepository)

    @Provides
    fun provideUpdateUserUseCase(userRepository: UserRepository): UpdateUserUseCase =
        UpdateUserUseCase(userRepository)

    @Provides
    fun provideUpdateUserAvatarUseCase(userRepository: UserRepository): UpdateUserAvatarUseCase =
        UpdateUserAvatarUseCase(userRepository)

    // Song use cases
    @Provides
    fun provideAddSongUseCase(songRepository: SongRepository): AddSongUseCase =
        AddSongUseCase(songRepository)

    @Provides
    fun provideUploadSongUseCase(songRepository: SongRepository): UploadSongUseCase =
        UploadSongUseCase(songRepository)

    @Provides
    fun provideSearchSongsUseCase(songRepository: SongRepository): SearchSongsUseCase =
        SearchSongsUseCase(songRepository)

    @Provides
    fun provideGetPublicSongsUseCase(songRepository: SongRepository): GetPublicSongsUseCase =
        GetPublicSongsUseCase(songRepository)

    @Provides
    fun provideGetSongsByGenreUseCase(songRepository: SongRepository): GetSongsByGenreUseCase =
        GetSongsByGenreUseCase(songRepository)

    @Provides
    fun provideGetSongsByIdsUseCase(songRepository: SongRepository): GetSongsByIdsUseCase =
        GetSongsByIdsUseCase(songRepository)

    // Playlist use cases
    @Provides
    fun provideCreatePlaylistUseCase(playlistRepository: PlaylistRepository): CreatePlaylistUseCase =
        CreatePlaylistUseCase(playlistRepository)

    @Provides
    fun provideUpdatePlaylistUseCase(playlistRepository: PlaylistRepository): UpdatePlaylistUseCase =
        UpdatePlaylistUseCase(playlistRepository)

    @Provides
    fun provideGetPublicPlaylistsUseCase(playlistRepository: PlaylistRepository): GetPublicPlaylistsUseCase =
        GetPublicPlaylistsUseCase(playlistRepository)

    @Provides
    fun provideGetUserPlaylistsUseCase(playlistRepository: PlaylistRepository): GetUserPlaylistsUseCase =
        GetUserPlaylistsUseCase(playlistRepository)

    @Provides
    fun provideGetPlaylistByIdUseCase(playlistRepository: PlaylistRepository): GetPlaylistByIdUseCase =
        GetPlaylistByIdUseCase(playlistRepository)

    @Provides
    fun provideAddTrackToPlaylistUseCase(playlistRepository: PlaylistRepository): AddTrackToPlaylistUseCase =
        AddTrackToPlaylistUseCase(playlistRepository)

    @Provides
    fun provideRemoveTrackFromPlaylistUseCase(playlistRepository: PlaylistRepository): RemoveTrackFromPlaylistUseCase =
        RemoveTrackFromPlaylistUseCase(playlistRepository)

    @Provides
    fun provideGetPlaylistTracksUseCase(playlistRepository: PlaylistRepository): GetPlaylistTracksUseCase =
        GetPlaylistTracksUseCase(playlistRepository)

    @Provides
    fun provideDeletePlaylistUseCase(playlistRepository: PlaylistRepository): DeletePlaylistUseCase =
        DeletePlaylistUseCase(playlistRepository)

    @Provides
    fun provideUploadPlaylistCoverUseCase(playlistRepository: PlaylistRepository): UploadPlaylistCoverUseCase =
        UploadPlaylistCoverUseCase(playlistRepository)

    @Provides
    fun provideLikePlaylistUseCase(playlistRepository: PlaylistRepository): LikePlaylistUseCase =
        LikePlaylistUseCase(playlistRepository)

    @Provides
    fun provideUnlikePlaylistUseCase(playlistRepository: PlaylistRepository): UnlikePlaylistUseCase =
        UnlikePlaylistUseCase(playlistRepository)

    @Provides
    fun provideIsPlaylistLikedUseCase(playlistRepository: PlaylistRepository): IsPlaylistLikedUseCase =
        IsPlaylistLikedUseCase(playlistRepository)

    // Search History use cases
    @Provides
    fun provideSaveSearchHistoryUseCase(repo: SearchHistoryRepository): SaveSearchHistoryUseCase =
        SaveSearchHistoryUseCase(repo)

    @Provides
    fun provideGetRecentSearchesUseCase(repo: SearchHistoryRepository): GetRecentSearchesUseCase =
        GetRecentSearchesUseCase(repo)

    @Provides
    fun provideDeleteSearchHistoryUseCase(repo: SearchHistoryRepository): DeleteSearchHistoryUseCase =
        DeleteSearchHistoryUseCase(repo)

    @Provides
    fun provideClearSearchHistoryUseCase(repo: SearchHistoryRepository): ClearSearchHistoryUseCase =
        ClearSearchHistoryUseCase(repo)

    // Likes use cases
    @Provides
    fun provideLikeSongUseCase(repo: LikesRepository): LikeSongUseCase =
        LikeSongUseCase(repo)

    @Provides
    fun provideUnlikeSongUseCase(repo: LikesRepository): UnlikeSongUseCase =
        UnlikeSongUseCase(repo)

    @Provides
    fun provideIsLikedUseCase(repo: LikesRepository): IsLikedUseCase =
        IsLikedUseCase(repo)

    @Provides
    fun provideGetLikedSongIdsUseCase(repo: LikesRepository): GetLikedSongIdsUseCase =
        GetLikedSongIdsUseCase(repo)

    @Provides
    fun provideLogListeningHistoryUseCase(repo: HistoryRepository): LogListeningHistoryUseCase =
        LogListeningHistoryUseCase(repo)

    @Provides
    @Singleton
    fun provideGetListeningHistoryUseCase(repo: HistoryRepository): GetListeningHistoryUseCase =
        GetListeningHistoryUseCase(repo)

    @Provides
    @Singleton
    fun provideObserveListeningHistoryUseCase(repo: HistoryRepository): com.example.blesstify.domain.usecase.ObserveListeningHistoryUseCase =
        com.example.blesstify.domain.usecase.ObserveListeningHistoryUseCase(repo)

    @Provides
    fun provideClearListeningHistoryUseCase(repo: HistoryRepository): ClearListeningHistoryUseCase =
        ClearListeningHistoryUseCase(repo)

    @Provides
    fun provideGetLatestRecommendationUseCase(repo: RecommendationRepository): GetLatestRecommendationUseCase =
        GetLatestRecommendationUseCase(repo)

    @Provides
    fun provideGetRecentPlaylistsUseCase(
        historyRepo: HistoryRepository,
        playlistRepo: PlaylistRepository
    ): GetRecentPlaylistsUseCase = GetRecentPlaylistsUseCase(historyRepo, playlistRepo)

    @Provides
    fun provideGenerateMoodPlaylistUseCase(
        songRepository: SongRepository,
        recommendationRepository: RecommendationRepository
    ): GenerateMoodPlaylistUseCase = GenerateMoodPlaylistUseCase(songRepository, recommendationRepository)

    @Provides
    fun provideGetEqualizerSettingsUseCase(userRepository: UserRepository): GetEqualizerSettingsUseCase =
        GetEqualizerSettingsUseCase(userRepository)

    @Provides
    fun provideSaveEqualizerSettingsUseCase(userRepository: UserRepository): SaveEqualizerSettingsUseCase =
        SaveEqualizerSettingsUseCase(userRepository)

    @Provides
    fun provideGetNotificationsUseCase(repo: NotificationRepository): GetNotificationsUseCase =
        GetNotificationsUseCase(repo)

    @Provides
    fun provideMarkNotificationAsReadUseCase(repo: NotificationRepository): MarkNotificationAsReadUseCase =
        MarkNotificationAsReadUseCase(repo)

    @Provides
    fun provideGetUnreadCountUseCase(repo: NotificationRepository): GetUnreadCountUseCase =
        GetUnreadCountUseCase(repo)

    @Provides
    fun provideGetSubscriptionUseCase(repo: SubscriptionRepository): GetSubscriptionUseCase =
        GetSubscriptionUseCase(repo)
}
