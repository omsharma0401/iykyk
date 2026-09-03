# iykyk - Technical Implementation

This Android app takes a short portrait video and generates a shareable collage with one photo per person. The pipeline detects faces, groups observations into continuous appearances, identifies which appearances belong to the same person, selects the best frame for each person, and renders the final collage.

Everything runs on-device and there is no backend.

## 1. Video Processing Pipeline

The video is decoded sequentially with Android `MediaCodec`, sampling a frame roughly every 333 ms (~3 FPS). Each YUV frame is converted to an upright RGB bitmap and downscaled to a maximum of 720 px on its longer side for analysis.

Sequential decoding is faster than seeking to individual timestamps because seeking repeatedly re-decodes from the previous keyframe. If `MediaCodec` fails, `MediaMetadataRetriever` is used as a fallback.

**Frame extraction -> face detection -> filtering -> alignment -> embedding -> appearance tracking -> identity clustering -> representative-frame selection -> collage generation**

## 2. Face Detection and Filtering

Google ML Kit runs in accurate mode with landmarks and classification enabled, providing each face's bounding box, landmarks, head rotation, eye-open probability, and smile probability.

Before embedding, detections pass four filters:

* **Size:** face height ≥ 8% of frame height.
* **Position:** ≥ 60% of the bounding box must lie inside the frame.
* **Duplicates:** a box mostly contained within a larger box is discarded to handle occasional duplicate ML Kit detections.
* **Sharpness:** Laplacian variance is measured on a `64 × 64` face crop, which indicates the sharpness. The threshold is `60`; blurred motion frames typically score 5–11, while usable faces score around 590+.

This removes detections that are too small, partially outside the frame, duplicated, or too blurred for reliable recognition.

## 3. Face Alignment and Embeddings

ML Kit determines **where a face is**; the embedding model determines **how similar two faces are**.

Each accepted face is aligned using the centres of both eyes and the mouth. Eye-only alignment handles rotation and scale but not changes in width-to-height ratio, which becomes important in horizontally squeezed split-screen footage.

With eye-only alignment, the same person scored as low as `0.41`, while different people reached `0.67`. Adding the mouth as a third point enables an affine transform that corrects this distortion. Across the three sample clips, this raised same-person similarity to at least `0.63` while different-person similarity remained at most `0.58`.

The aligned `112 × 112` RGB crop is passed to `MobileFaceNet.tflite`, trained with ArcFace loss on MS1M-refine-v2. The model produces a **192-dimensional embedding**, which is L2-normalized before cosine similarity is calculated.

The TFLite export has a fixed batch size of `2`. Since only one face is processed at a time, the same crop is placed in both batch slots and the first output is used.

## 4. Tracking Individual Appearances

Multiple consecutive detections of the same person should count as one **appearance**. Each new face is matched against open tracks. A track continues when the best candidate has:

* embedding similarity ≥ `0.50`
* movement ≤ `1.5` bounding-box widths

For head turns, if movement is < `0.5` box widths and either face is rotated > `25°`, the similarity threshold can drop to `0.30`. This handles the sharp similarity drop caused by turning while avoiding false matches after a cut.

A track closes after `0.45` seconds without a matching detection. This allows a missed sample without extending an appearance across a real transition. A track also needs at least **two observations** to count, preventing isolated blurred detections from becoming appearances.

## 5. Grouping Appearances into People

After tracking, appearances are grouped into people using **average-linkage agglomerative clustering**. Each appearance starts as its own group; the two groups with the highest average pairwise similarity are repeatedly merged until the best remaining similarity falls below the threshold.

A temporal constraint overrides similarity: **appearances that overlap in time can never be merged**. Two faces visible simultaneously must represent different people, regardless of embedding similarity.

The clustering threshold is `0.60`, calibrated from the three sample clips:

* Lowest same-person similarity: `0.83`, `0.81`, `0.63`
* Highest different-person similarity: `0.50`, `0.56`, `0.58`

This gives a valid separation between `0.58` and `0.63`, with `0.60` chosen as the midpoint. The value is specific to the current model and alignment approach and should be recalibrated if either changes or the input footage differs substantially.

## 6. Selecting the Representative Photo

Each person can have multiple candidate frames. Rather than selecting purely by face size or sharpness, each candidate is scored using:

* head orientation
* face sharpness
* eye openness
* smile probability
* whether the person is alone
* naturalness of face proportions after alignment
* available space around the head for cropping

The highest-scoring frame becomes the representative image. It is then re-decoded from the original video at higher resolution and cropped with additional space around the head for a more natural collage tile.

## 7. Processing Architecture

All expensive processing runs off the main thread.

`VideoProcessingRepo` exposes the pipeline as a cold Kotlin `Flow`, with stages overlapping through buffering:

* `FrameExtractor` - sequential decoding on `Dispatchers.IO`
* face detection - `Dispatchers.Default`
* embedding, filtering, tracking, and clustering - `Dispatchers.Default`

This allows decoding, detection, and downstream processing to progress concurrently rather than processing one frame end-to-end before starting the next.

Progress is exposed through `UiState.Loading` and drives the UI progress indicator. If the collage screen is left during processing, the collecting coroutine is cancelled. `CancellationException` is rethrown instead of being converted into an error, ensuring cancellation actually terminates the pipeline.

## 8. Core Technologies

* **Kotlin + Jetpack Compose** - application and UI
* **Kotlin Coroutines + Flow** - asynchronous processing and progress
* **Hilt** - dependency injection
* **CameraX** - recording and camera preview
* **Google ML Kit** - face detection, landmarks, and classification
* **TensorFlow Lite + MobileFaceNet** - on-device face embeddings
* **MediaCodec** - sequential video decoding
* **MediaMetadataRetriever** - decoding fallback
* **MediaStore + FileProvider** - gallery saving and sharing

## 9. Project Structure

`ml/` - face detection, alignment, and embeddings

`processing/` - frame sampling, filtering, tracking, clustering, and photo scoring

`data/repo/` - video processing, capture, gallery saving, and sharing

`ui/` - Compose screens, controls, theme, and layout

`vm/` - screen state and user actions

`constants/` - pipeline and collage configuration

## 10. Build and Run

Requirements: Android Studio with SDK 35, JDK 11 or newer, and a device or emulator running Android 8.0 (API 26) or newer.

```bash
./gradlew assembleDebug
```

Install the generated APK with `adb`, or run the `app` configuration directly from Android Studio. The app supports picking a video from the gallery; camera recording is included but not required to process a supplied video.

## 11. Testing and Evaluation

The pipeline can be checked against ground truth without eyeballing collages. A debug build logs every detected face to `observations.jsonl`; `tools/eval/analyze.py` replays tracking and clustering from that log and verifies the result against known appearance windows for each sample clip.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
./tools/eval/run_on_device.sh /path/to/sample1.mp4 sample1
python3 tools/eval/analyze.py tools/eval/out/sample1/observations.jsonl --gt sample1
```

Full process details, including how the ground truth for each sample clip was established, are in [docs/TESTING_AND_EVALUATION.md](docs/TESTING_AND_EVALUATION.md) and [tools/eval/ground_truth.md](tools/eval/ground_truth.md).
