# Testing & Evaluation

This document explains how we test and evaluate the video processing pipeline.

---

## Overview

Evaluation consists of two main parts:
1. **On-Device Pipeline Evaluation**: Pushing test videos directly to a physical device or emulator using `adb` and inspecting tracking logs.
2. **Offline Log Analysis & Ground Truth Scoring**: Replaying face detection logs to verify face clustering and identity matching accuracy.

---

## 1. On-Device Pipeline Evaluation

To test video processing without manually using the camera UI every time, the debug build contains a specialized intent receiver (`com.omsharma.iykyk.DEBUG_PROCESS_VIDEO`).

We provide a helper script at `tools/eval/run_on_device.sh`:

```bash
# Usage:
./tools/eval/run_on_device.sh <path_to_video.mp4> [sample_name]
```

### What `run_on_device.sh` does:
1. Copies the video file to the app's internal storage via `adb`.
2. Starts `MainActivity` with the debug intent, launching `CollageScreen` directly.
3. Streams logs, captures a screenshot when processing finishes, and saves a face observation log (`observations.jsonl`).
4. Stores output files in `tools/eval/out/<sample_name>/`.

---

## 2. Offline Analysis & Ground Truth Scoring

Once `observations.jsonl` is captured from a test run, you can analyze face tracking stability and clustering accuracy using `tools/eval/analyze.py`.

```bash
# Analyze a captured run against ground truth:
python3 tools/eval/analyze.py tools/eval/out/sample1/observations.jsonl --gt sample1
```

### How `analyze.py` works:
- **Replays Tracking & Clustering**: Runs the same tracking logic and clustering algorithms as `VideoProcessingRepo` on the logged face vectors.
- **Validates Ground Truth**: Checks if the 5 people with 4 appearances each (20 total appearances) are correctly identified and grouped.
- **Calculates Vector Similarity Margins**: Computes the minimum similarity between appearances of the same person versus different people to verify that our `0.60` merge threshold is solid.

Ground truth details for test clips (`sample1`, `sample2`, and `sample3`) are documented in [`tools/eval/ground_truth.md`](../tools/eval/ground_truth.md).