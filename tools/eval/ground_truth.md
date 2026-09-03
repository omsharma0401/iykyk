# Ground truth for the three sample clips

All three clips are 30 s, 1080x1920, 25 fps, and share one cut structure: five solo shots,
then returns, with two split-screen shots (two people side by side, each squeezed to half
width) at about 10.3–11.3 s and 20.3–21.3 s, and blurred whip-pan transitions between shots.
Each clip has **5 people x 4 appearances = 20 appearances**.

Sample 1 is the assignment's own ground truth. Samples 2 and 3 were read off 2 fps contact
sheets (`ffmpeg -vf fps=2,tile=10x6`, one row = 5 s) and cross-checked against the pipeline's
segmentation; every window below was confirmed visually. Letters are per clip, in order of
first appearance, and mean nothing across clips.

## Sample 2

| Person | Description | Appearances (s) |
|---|---|---|
| A | beige hijab, checkered jacket | 0.0–1.3, 13.7–14.7, 18.7–19.7, 27.0–28.0 |
| B | dark suit, clipboard | 1.7–3.0, 10.3–11.3 (with D), 15.3–16.3, 23.7–24.7 |
| C | glasses and headset | 3.7–4.7, 8.7–9.7, 12.0–13.0, 20.3–21.3 (with E) |
| D | white hijab | 5.3–6.3, 10.3–11.3 (with B), 22.0–23.0, 28.7–30.0 |
| E | long dark hair, white top | 7.0–8.0, 17.0–18.0, 20.3–21.3 (with C), 25.3–26.3 |

## Sample 3

| Person | Description | Appearances (s) |
|---|---|---|
| A | long dark hair, white top | 0.0–1.3, 12.0–13.0, 20.3–21.3 (with D), 25.3–26.3 |
| B | glasses and headset | 1.7–3.0, 8.7–9.7, 17.0–18.0, 27.0–28.0 |
| C | dark suit, clipboard | 3.7–4.7, 10.3–11.3 (with E), 15.3–16.3, 22.0–23.0 |
| D | white hijab | 5.3–6.3, 13.7–14.7, 20.3–21.3 (with A), 28.7–30.0 |
| E | beige hijab, checkered jacket | 7.0–8.0, 10.3–11.3 (with C), 18.7–19.7, 23.7–24.7 |

## Sample 1 (from the assignment)

| Person | Description | Appearances (s) |
|---|---|---|
| A | dark suit, clipboard | 0.0–1.0, 10.5–11.5 (with D), 12.0–13.0, 22.0–23.0 |
| B | white hijab | 2.0–3.0, 9.0–9.5, 18.5–19.5, 28.5–29.5 |
| C | beige hijab, checkered jacket | 3.5–4.5, 13.5–14.5, 20.5–21.5 (with E), 25.5–26.0 |
| D | long dark hair, white top | 5.5–6.0, 10.5–11.5 (with A), 17.0–18.0, 24.0–24.5 |
| E | glasses and headset | 7.0–8.0, 15.5–16.0, 20.5–21.5 (with C), 27.0–28.0 |

Check a run with `python3 tools/eval/analyze.py <observations.jsonl> --gt sample2` (or
`sample1` / `sample3`).
