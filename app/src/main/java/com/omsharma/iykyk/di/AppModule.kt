package com.omsharma.iykyk.di

import android.content.Context
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.omsharma.iykyk.constants.PipelineConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val EMBEDDING_MODEL = "MobileFaceNet.tflite"

    // ML Kit client
    @Singleton
    @Provides
    fun provideFaceDetector(): FaceDetector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // fast mode misses the small split-screen faces
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(PipelineConfig.MIN_FACE_WIDTH_RATIO)
                .build()
        )

    // TFLite embedding model
    @Singleton
    @Provides
    fun provideEmbeddingInterpreter(@ApplicationContext context: Context): Interpreter =
        context.assets.openFd(EMBEDDING_MODEL).use { fd ->
            val model = FileInputStream(fd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            Interpreter(model)
        }
}
