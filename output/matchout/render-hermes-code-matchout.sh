#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

rm -rf output/matchout/frames output/matchout/png
mkdir -p output/matchout/png

node output/matchout/render-hermes-code-matchout.mjs

for frame in output/matchout/frames/frame_*.svg; do
  base=${frame##*/}
  sips -s format png "$frame" --out "output/matchout/png/${base%.svg}.png" >/dev/null
done

ffmpeg -y \
  -framerate 30 \
  -i output/matchout/png/frame_%04d.png \
  -filter_complex "[0:v]split=2[sharp][blurbase];[blurbase]gblur=sigma=6:steps=3[blur];[sharp]crop=108:35:306:623[cutout];[blur][cutout]overlay=306:623,format=yuv420p" \
  -c:v libx264 \
  -pix_fmt yuv420p \
  -movflags +faststart \
  output/matchout/hermes-code-matchout.mp4

ffmpeg -y \
  -i output/matchout/hermes-code-matchout.mp4 \
  -vf "fps=2,scale=360:-1,tile=2x5" \
  -frames:v 1 \
  output/matchout/hermes-code-matchout-preview.jpg

cp output/matchout/hermes-code-matchout.mp4 /Users/krishnabahadurbasnet/Downloads/hermes-code-matchout.mp4

rm -rf output/matchout/frames output/matchout/png
