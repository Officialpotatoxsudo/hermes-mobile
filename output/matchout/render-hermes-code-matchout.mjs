import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const outDir = path.join(root, "output", "matchout");
const framesDir = path.join(outDir, "frames");
fs.mkdirSync(framesDir, { recursive: true });

const width = 720;
const height = 1280;
const fps = 30;
const seconds = 2.4;
const totalFrames = Math.round(fps * seconds);
const framesPerScene = 8;
const lineHeight = 36;
const fontSize = 26;
const charWidth = 15.35;
const anchorX = 314;
const anchorY = 650;
const chipPadX = 8;
const chipWidth = 6 * charWidth + chipPadX * 2;
const chipHeight = 35;

const scenes = [
  {
    file: "app/src/main/java/com/hermes/mobile/MainActivity.kt",
    start: 44,
    end: 60,
    focusLine: 48,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/navigation/HermesNavGraph.kt",
    start: 104,
    end: 124,
    focusLine: 108,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/ui/theme/HermesTheme.kt",
    start: 302,
    end: 326,
    focusLine: 306,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/core/data/HermesRepository.kt",
    start: 25,
    end: 44,
    focusLine: 29,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/core/data/local/HermesDatabase.kt",
    start: 8,
    end: 29,
    focusLine: 13,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/ui/components/HermesChip.kt",
    start: 24,
    end: 48,
    focusLine: 27,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/feature/lock/AppLockScreen.kt",
    start: 106,
    end: 122,
    focusLine: 112,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/core/error/ErrorMapper.kt",
    start: 7,
    end: 18,
    focusLine: 12,
  },
  {
    file: "app/src/main/java/com/hermes/mobile/core/settings/AppPreferences.kt",
    start: 180,
    end: 190,
    focusLine: 184,
  },
].map(loadScene);

function loadScene(scene) {
  const fullPath = path.join(root, scene.file);
  const lines = fs.readFileSync(fullPath, "utf8").split(/\r?\n/);
  const clipped = lines.slice(scene.start - 1, scene.end);
  const focusIndex = scene.focusLine - scene.start;
  const focus = clipped[focusIndex] ?? "";
  const tokenIndex = focus.indexOf("Hermes");

  return {
    ...scene,
    label: scene.file.replace("app/src/main/java/com/hermes/mobile/", ""),
    lines: clipped,
    focusIndex,
    tokenIndex: Math.max(tokenIndex, 0),
  };
}

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function tokenColor(token) {
  if (/^\s*\/\//.test(token)) return "#667084";
  if (/^".*"$/.test(token)) return "#f1b36b";
  if (/^@[A-Za-z]/.test(token)) return "#c792ea";
  if (/^(class|fun|private|val|var|return|if|else|when|object|override|abstract|import|package|true|false|null)$/.test(token)) {
    return "#7dd3fc";
  }
  if (/^[A-Z][A-Za-z0-9_]*$/.test(token)) return "#f6d365";
  if (/^[0-9]+$/.test(token)) return "#a7f3d0";
  return "#d9e2f1";
}

function renderCodeLine(line, x, y, isFocus) {
  const parts = [];
  const tokenAt = isFocus ? line.indexOf("Hermes") : -1;
  let cursor = x;

  function writeSegment(text, color = null) {
    if (!text) return;
    const pieces = text.match(/(\s+|\/\/.*|"(?:[^"\\]|\\.)*"|@[A-Za-z0-9_]+|[A-Za-z_][A-Za-z0-9_]*|\d+|[^\sA-Za-z0-9_]+)/g) ?? [text];
    for (const piece of pieces) {
      const escaped = escapeXml(piece);
      const fill = color ?? tokenColor(piece);
      parts.push(`<text x="${cursor.toFixed(1)}" y="${y.toFixed(1)}" fill="${fill}">${escaped}</text>`);
      cursor += piece.length * charWidth;
    }
  }

  if (tokenAt >= 0) {
    writeSegment(line.slice(0, tokenAt));
    cursor += "Hermes".length * charWidth;
    writeSegment(line.slice(tokenAt + "Hermes".length));
  } else {
    writeSegment(line);
  }

  return parts.join("\n");
}

function renderSceneBody(scene) {
  const x = anchorX - scene.tokenIndex * charWidth;
  const y = anchorY - scene.focusIndex * lineHeight;
  const body = scene.lines
    .map((line, index) => renderCodeLine(line, x, y + index * lineHeight, index === scene.focusIndex))
    .join("\n");

  return `
    ${body}
  `;
}

function renderScene(scene, filterId, blur, opacity, extra = "") {
  return `
    <filter id="${filterId}">
      <feGaussianBlur stdDeviation="${blur}" />
    </filter>
    <g opacity="${opacity.toFixed(3)}"
       filter="url(#${filterId})" ${extra}>
      ${renderSceneBody(scene)}
    </g>
  `;
}

function renderBackgroundEcho(scene) {
  const text = scene.lines.concat(scene.lines).slice(0, 24);
  return `
    <filter id="ghostBlur"><feGaussianBlur stdDeviation="13" /></filter>
    <g opacity="0.14" filter="url(#ghostBlur)" transform="translate(-190 175)">
      ${text.map((line, i) => `<text x="0" y="${i * 35}" fill="#b9c4d8">${escapeXml(line)}</text>`).join("\n")}
    </g>
    <g opacity="0.10" filter="url(#ghostBlur)" transform="translate(420 560)">
      ${text.map((line, i) => `<text x="0" y="${i * 35}" fill="#b9c4d8">${escapeXml(line)}</text>`).join("\n")}
    </g>
  `;
}

function renderTexture() {
  const strokes = [];
  for (let i = 0; i < 95; i += 1) {
    const y = i * 14;
    const dx = Math.sin(i * 1.7) * 46;
    const alpha = 0.018 + (i % 7) * 0.002;
    strokes.push(`<path d="M -40 ${y} C 180 ${y + dx}, 420 ${y - dx * 0.5}, 760 ${y + dx * 0.2}" stroke="#ffffff" stroke-opacity="${alpha.toFixed(3)}" stroke-width="1" fill="none"/>`);
  }
  return strokes.join("\n");
}

function renderSvg(frame) {
  const sceneIndex = Math.floor(frame / framesPerScene) % scenes.length;
  const scene = scenes[sceneIndex];
  const pulse = 1;
  const chipX = anchorX - chipPadX;
  const chipY = anchorY - chipHeight + 8;

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <defs>
    <linearGradient id="backdrop" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0%" stop-color="#090a0d"/>
      <stop offset="48%" stop-color="#11151b"/>
      <stop offset="100%" stop-color="#050607"/>
    </linearGradient>
    <radialGradient id="vignette" cx="50%" cy="43%" r="72%">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.035"/>
      <stop offset="62%" stop-color="#000000" stop-opacity="0.05"/>
      <stop offset="100%" stop-color="#000000" stop-opacity="0.74"/>
    </radialGradient>
    <filter id="paperNoise">
      <feTurbulence type="fractalNoise" baseFrequency="0.78" numOctaves="3" seed="17"/>
      <feColorMatrix type="saturate" values="0"/>
      <feComponentTransfer>
        <feFuncA type="table" tableValues="0 0.12"/>
      </feComponentTransfer>
    </filter>
  </defs>
  <rect width="720" height="1280" fill="url(#backdrop)"/>
  <rect width="720" height="1280" filter="url(#paperNoise)" opacity="0.55"/>
  ${renderTexture()}
  <rect width="720" height="1280" fill="url(#vignette)"/>
  <g font-family="'SF Mono', Menlo, Monaco, Consolas, monospace" font-size="${fontSize}" font-weight="650">
    ${renderBackgroundEcho(scene)}
    <g opacity="0.94">
      ${renderSceneBody(scene)}
    </g>
  </g>
  <g transform="translate(${(chipX + chipWidth / 2).toFixed(1)} ${(chipY + chipHeight / 2).toFixed(1)}) scale(${pulse.toFixed(3)}) translate(${(-(chipX + chipWidth / 2)).toFixed(1)} ${(-(chipY + chipHeight / 2)).toFixed(1)})">
    <rect x="${chipX.toFixed(1)}" y="${chipY.toFixed(1)}" width="${chipWidth.toFixed(1)}" height="${chipHeight}" rx="4" fill="#9f2f27"/>
    <rect x="${chipX.toFixed(1)}" y="${chipY.toFixed(1)}" width="${chipWidth.toFixed(1)}" height="${chipHeight}" rx="4" fill="#ffffff" fill-opacity="0.08"/>
    <text x="${anchorX.toFixed(1)}" y="${anchorY.toFixed(1)}"
      fill="#fff8f0"
      font-family="'SF Mono', Menlo, Monaco, Consolas, monospace"
      font-size="${fontSize}"
      font-weight="800">Hermes</text>
  </g>
</svg>`;
}

for (let frame = 0; frame < totalFrames; frame += 1) {
  const file = path.join(framesDir, `frame_${String(frame).padStart(4, "0")}.svg`);
  fs.writeFileSync(file, renderSvg(frame));
}

console.log(`Wrote ${totalFrames} SVG frames to ${framesDir}`);
