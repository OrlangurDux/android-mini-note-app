package app.mininote.mininote.di

import app.mininote.mininote.data.remote.AuthInterceptor
import app.mininote.mininote.data.remote.RoutingInterceptor
import app.mininote.mininote.data.remote.api.AuthApi
import app.mininote.mininote.data.remote.api.CategoriesApi
import app.mininote.mininote.data.remote.api.NotesApi
import app.mininote.mininote.data.remote.api.SupportApi
import app.mininote.mininote.data.remote.api.VersionApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

// Retrofit требует валидный baseUrl на этапе конструирования, но реальный сервер выбирается
// в рантайме через ActiveServerHolder — RoutingInterceptor переписывает каждый запрос
// на актуальный активный сервер, поэтому здесь достаточно синтаксического плейсхолдера.
private const val PLACEHOLDER_BASE_URL = "http://mininote.invalid/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideErrorEnvelopeAdapter(moshi: Moshi): JsonAdapter<ErrorEnvelopeDto> =
        moshi.adapter(ErrorEnvelopeDto::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        routingInterceptor: RoutingInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(routingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideSupportApi(retrofit: Retrofit): SupportApi = retrofit.create(SupportApi::class.java)

    @Provides
    @Singleton
    fun provideVersionApi(retrofit: Retrofit): VersionApi = retrofit.create(VersionApi::class.java)

    @Provides
    @Singleton
    fun provideNotesApi(retrofit: Retrofit): NotesApi = retrofit.create(NotesApi::class.java)

    @Provides
    @Singleton
    fun provideCategoriesApi(retrofit: Retrofit): CategoriesApi = retrofit.create(CategoriesApi::class.java)
}
