#!/usr/bin/env python3
"""Replays tracking + clustering offline from an observations.jsonl dump and reports the
similarity margins. With --gt sample1 it also scores against the Sample 1 ground truth.

  python3 tools/eval/analyze.py tools/eval/out/sample1/observations.jsonl --gt sample1

Pure Python, no dependencies. The tracker/clusterer here mirror PipelineConfig; keep them in sync.
"""
import json, math, sys
from collections import defaultdict

TRACK_GAP_S = 0.45
TRACK_LINK = 0.5
STAY_MOVE = 0.5
STAY_LINK = 0.3
TURN_DEG = 25
MAX_MOVE = 1.5
MERGE_THRESHOLD = 0.60
MIN_OBS = 2

# Sample 1 windows come from the assignment's ground truth. Samples 2 and 3 were derived by
# reading 2 fps contact sheets of each clip (tools/eval/ground_truth.md has the per-person
# descriptions); all three clips share the same cut structure: 5 people x 4 appearances, with
# two-person shots at ~10.3-11.3 s and ~20.3-21.3 s.
GT = {
    'sample1': {
        'A': [(0, 1.0), (10.5, 11.5), (12, 13), (22, 23)],
        'B': [(2, 3), (9, 9.5), (18.5, 19.5), (28.5, 29.5)],
        'C': [(3.5, 4.5), (13.5, 14.5), (20.5, 21.5), (25.5, 26)],
        'D': [(5.5, 6), (10.5, 11.5), (17, 18), (24, 24.5)],
        'E': [(7, 8), (15.5, 16), (20.5, 21.5), (27, 28)]},
    'sample2': {
        'A': [(0, 1.3), (13.7, 14.7), (18.7, 19.7), (27, 28)],       # beige hijab, checkered jacket
        'B': [(1.7, 3), (10.3, 11.3), (15.3, 16.3), (23.7, 24.7)],  # dark suit, clipboard
        'C': [(3.7, 4.7), (8.7, 9.7), (12, 13), (20.3, 21.3)],      # glasses + headset
        'D': [(5.3, 6.3), (10.3, 11.3), (22, 23), (28.7, 30)],      # white hijab
        'E': [(7, 8), (17, 18), (20.3, 21.3), (25.3, 26.3)]},       # long dark hair, white top
    'sample3': {
        'A': [(0, 1.3), (12, 13), (20.3, 21.3), (25.3, 26.3)],      # long dark hair, white top
        'B': [(1.7, 3), (8.7, 9.7), (17, 18), (27, 28)],            # glasses + headset
        'C': [(3.7, 4.7), (10.3, 11.3), (15.3, 16.3), (22, 23)],    # dark suit, clipboard
        'D': [(5.3, 6.3), (13.7, 14.7), (20.3, 21.3), (28.7, 30)],  # white hijab
        'E': [(7, 8), (10.3, 11.3), (18.7, 19.7), (23.7, 24.7)]},   # beige hijab, checkered jacket
}

def cos(a, b): return sum(x * y for x, y in zip(a, b))
def unit(v):
    n = math.sqrt(sum(x * x for x in v)) or 1.0
    return [x / n for x in v]
def mean(vs, ws=None):
    ws = ws or [1.0] * len(vs)
    return unit([sum(w * v[i] for v, w in zip(vs, ws)) for i in range(len(vs[0]))])
def frontal(r): return max(0.0, math.cos(math.radians(r['yaw'])) * math.cos(math.radians(r['pitch'])))
def move(a, b):
    ab, bb = a['box'], b['box']
    return math.hypot((bb[0]+bb[2])/2 - (ab[0]+ab[2])/2, (bb[1]+bb[3])/2 - (ab[1]+ab[3])/2) / max(1, ab[2]-ab[0])
def turned(r): return abs(r['yaw']) > TURN_DEG or abs(r['pitch']) > TURN_DEG
def can_link(prev, o, s):
    m = move(prev, o)
    if s >= TRACK_LINK and m <= MAX_MOVE: return True
    return (turned(prev) or turned(o)) and s >= STAY_LINK and m <= STAY_MOVE

def track(rows):
    frames = defaultdict(list)
    for r in rows: frames[r['tUs']].append(r)
    active, done = [], []
    for t in sorted(frames):
        ts = t / 1e6
        for tr in [tr for tr in active if ts - tr['last'] > TRACK_GAP_S]:
            active.remove(tr)
            if len(tr['obs']) >= MIN_OBS: done.append(tr)
        obs = frames[t]
        pairs = sorted(((cos(o['emb'], tr['obs'][-1]['emb']), i, j) for i, o in enumerate(obs) for j, tr in enumerate(active)), reverse=True)
        used_o, used_t = set(), set()
        for s, i, j in pairs:
            if not can_link(active[j]['obs'][-1], obs[i], s): continue
            if i in used_o or j in used_t: continue
            active[j]['obs'].append(obs[i]); active[j]['last'] = ts; used_o.add(i); used_t.add(j)
        for i, o in enumerate(obs):
            if i not in used_o: active.append({'obs': [o], 'last': ts})
    done += [tr for tr in active if len(tr['obs']) >= MIN_OBS]
    for tr in done:
        tr['start'] = tr['obs'][0]['tUs']; tr['end'] = tr['obs'][-1]['tUs']; tr['m'] = mean([o['emb'] for o in tr['obs']], [0.2 + frontal(o) for o in tr['obs']])
    return sorted(done, key=lambda tr: tr['start'])

def overlap(a, b): return a['start'] <= b['end'] and b['start'] <= a['end']

def cluster(apps):
    groups = [[i] for i in range(len(apps))]
    while len(groups) > 1:
        best = (-2, -1, -1)
        for a in range(len(groups)):
            for b in range(a + 1, len(groups)):
                if any(overlap(apps[i], apps[j]) for i in groups[a] for j in groups[b]): continue
                s = sum(cos(apps[i]['m'], apps[j]['m']) for i in groups[a] for j in groups[b]) / (len(groups[a]) * len(groups[b]))
                if s > best[0]: best = (s, a, b)
        if best[0] < MERGE_THRESHOLD: break
        groups[best[1]] += groups[best[2]]; del groups[best[2]]
    return groups

def label(app, gt):
    mid = (app['start'] + app['end']) / 2e6
    return '|'.join(sorted({p for p, segs in gt.items() for a, b in segs if a - 0.4 <= mid <= b + 0.4})) or '?'

def main():
    path = sys.argv[1]
    gt = GT.get(sys.argv[sys.argv.index('--gt') + 1]) if '--gt' in sys.argv else None
    rows = [json.loads(l) for l in open(path)]
    apps = track(rows)
    print(f"{len(rows)} observations -> {len(apps)} appearances")
    for a in apps:
        print(f"  {a['start']/1e6:6.2f}-{a['end']/1e6:6.2f}s  n={len(a['obs'])}" + (f"  gt={label(a, gt)}" if gt else ''))
    groups = cluster(apps)
    print(f"{len(groups)} people, counts {[len(g) for g in groups]}")
    if gt:
        for a in apps: a['lab'] = label(a, gt)
        solo = defaultdict(list)
        for a in apps:
            if '|' not in a['lab']: solo[a['lab']].append(a['m'])
        for a in apps:
            if '|' in a['lab']:
                a['lab'] = max(a['lab'].split('|'), key=lambda p: max(cos(a['m'], s) for s in solo[p]) if solo[p] else -1)
        n = len(apps)
        same = [cos(apps[i]['m'], apps[j]['m']) for i in range(n) for j in range(i + 1, n) if apps[i]['lab'] == apps[j]['lab']]
        diff = [cos(apps[i]['m'], apps[j]['m']) for i in range(n) for j in range(i + 1, n) if apps[i]['lab'] != apps[j]['lab'] and not overlap(apps[i], apps[j])]
        print(f"same-person min {min(same):.2f} | non-co-visible different-person max {max(diff):.2f} | margin {min(same)-max(diff):+.2f} | merge threshold {MERGE_THRESHOLD}")
        for g in groups: print("  group:", ''.join(sorted(apps[i]['lab'] for i in g)))
        ok = len(groups) == 5 and all(len(set(apps[i]['lab'] for i in g)) == 1 and len(g) == 4 for g in groups)
        print("GROUND TRUTH:", "PASS (5 people x 4 appearances, all correct)" if ok else "FAIL")

if __name__ == '__main__': main()
