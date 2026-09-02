package com.omsharma.iykyk.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Intentionally empty: every class in the video-processing pipeline uses an @Inject
// constructor with only no-arg or @ApplicationContext dependencies, so Hilt builds the
// whole VideoProcessingRepository graph on its own. The ML Kit client and the TFLite
// Interpreter are private `by lazy` fields inside FaceDetector/FaceEmbedder, not
// separately injected - there's nothing here to provide unless that changes.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
