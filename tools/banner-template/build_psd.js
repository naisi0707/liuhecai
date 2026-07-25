#!/usr/bin/env node
/**
 * 生成可编辑 PSD（背景 + 文字层）
 * 用法:
 *   node build_psd.js                    # 默认刘伯温
 *   node build_psd.js zhizun             # 指定品牌 id
 *   node build_psd.js --title 刘伯温 --domain 585520.xyz
 */
const fs = require('fs');
const path = require('path');
const { writePsd, initializeCanvas } = require('ag-psd');
const { createCanvas, loadImage, registerFont } = require('canvas');

initializeCanvas(createCanvas, loadImage);

const song = '/System/Library/Fonts/Supplemental/Songti.ttc';
const hei = '/System/Library/Fonts/Hiragino Sans GB.ttc';
try { registerFont(song, { family: 'SongtiSC' }); } catch (_) {}
try { registerFont(hei, { family: 'HiraginoSansGB' }); } catch (_) {}

function parseArgs(argv) {
  const out = { id: null, title: null, sub1: null, sub2: null, domain: null };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--title') out.title = argv[++i];
    else if (a === '--sub1') out.sub1 = argv[++i];
    else if (a === '--sub2') out.sub2 = argv[++i];
    else if (a === '--domain') out.domain = argv[++i];
    else if (!a.startsWith('-')) out.id = a;
  }
  return out;
}

function drawStyledText(W, H, text, opts) {
  const c = createCanvas(W, H);
  const ctx = c.getContext('2d');
  ctx.clearRect(0, 0, W, H);
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.font = opts.font;
  ctx.lineJoin = 'round';
  if (opts.letterSpacing && text.length > 1) {
    // approximate tracking for CJK title
    const chars = [...text];
    const total = chars.reduce((s, ch) => s + ctx.measureText(ch).width, 0)
      + opts.letterSpacing * (chars.length - 1);
    let x = opts.x - total / 2;
    for (const ch of chars) {
      const w = ctx.measureText(ch).width;
      const cx = x + w / 2;
      strokeFill(ctx, ch, cx, opts.y, opts);
      x += w + opts.letterSpacing;
    }
    return c;
  }
  strokeFill(ctx, text, opts.x, opts.y, opts);
  return c;
}

function strokeFill(ctx, text, x, y, opts) {
  if (opts.strokeWidth) {
    ctx.lineWidth = opts.strokeWidth;
    ctx.strokeStyle = opts.stroke;
    ctx.strokeText(text, x, y);
  }
  if (opts.gradient) {
    const g = ctx.createLinearGradient(x, y - opts.gradH, x, y + opts.gradH);
    for (const [s, col] of opts.gradient) g.addColorStop(s, col);
    ctx.fillStyle = g;
  } else {
    ctx.fillStyle = opts.fill || '#fff';
  }
  ctx.fillText(text, x, y);
  if (opts.highlight) {
    ctx.globalAlpha = 0.5;
    const g2 = ctx.createLinearGradient(x, y - opts.gradH, x, y);
    g2.addColorStop(0, '#ffffff');
    g2.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g2;
    ctx.fillText(text, x, y - 1);
    ctx.globalAlpha = 1;
  }
}

async function main() {
  const args = parseArgs(process.argv);
  const cfg = JSON.parse(fs.readFileSync(path.join(__dirname, 'brands.json'), 'utf8'));
  let brand = cfg.brands.find((b) => b.id === (args.id || 'liubowen')) || cfg.brands[0];
  brand = {
    ...brand,
    title: args.title || brand.title,
    sub1: args.sub1 || brand.sub1,
    sub2: args.sub2 || brand.sub2,
    domain: args.domain || brand.domain,
  };

  const bg = await loadImage(path.join(__dirname, 'background.png'));
  const W = bg.width;
  const H = bg.height;
  const L = cfg.layout;

  const titleSize = Math.round(H * L.title.fontSize * (brand.title.length > 3 ? 0.82 : 1));
  const titleCanvas = drawStyledText(W, H, brand.title, {
    x: Math.round(W * L.title.x),
    y: Math.round(H * L.title.y),
    font: `bold ${titleSize}px SongtiSC, HiraginoSansGB, sans-serif`,
    stroke: '#5a1a08',
    strokeWidth: Math.round(H * 0.035),
    gradient: [[0, '#fff8e0'], [0.3, '#ffe59a'], [0.65, '#f0a828'], [1, '#c45a18']],
    gradH: Math.round(H * 0.16),
    highlight: true,
    letterSpacing: brand.title.length <= 4 ? Math.round(titleSize * 0.12) : 0,
  });
  const sub1Canvas = drawStyledText(W, H, brand.sub1, {
    x: Math.round(W * L.sub1.x),
    y: Math.round(H * L.sub1.y),
    font: `bold ${Math.round(H * L.sub1.fontSize)}px SongtiSC, HiraginoSansGB, sans-serif`,
    stroke: '#4a1010',
    strokeWidth: 3,
    gradient: [[0, '#ffe9a8'], [1, '#d48818']],
    gradH: 14,
  });
  const sub2Canvas = drawStyledText(W, H, brand.sub2, {
    x: Math.round(W * L.sub2.x),
    y: Math.round(H * L.sub2.y),
    font: `bold ${Math.round(H * L.sub2.fontSize)}px SongtiSC, HiraginoSansGB, sans-serif`,
    stroke: '#3a0808',
    strokeWidth: 3,
    gradient: [[0, '#fff2c8'], [1, '#e0a838']],
    gradH: 12,
  });
  const domainCanvas = drawStyledText(W, H, brand.domain, {
    x: Math.round(W * L.domain.x),
    y: Math.round(H * L.domain.y),
    font: `bold ${Math.round(H * L.domain.fontSize)}px HiraginoSansGB, SongtiSC, sans-serif`,
    stroke: '#2a1005',
    strokeWidth: 5,
    gradient: [[0, '#ffd898'], [0.45, '#ff9a30'], [1, '#e05810']],
    gradH: 22,
  });

  const bgCanvas = createCanvas(W, H);
  bgCanvas.getContext('2d').drawImage(bg, 0, 0);

  const psd = {
    width: W,
    height: H,
    children: [
      { name: '00-背景（勿改）', canvas: bgCanvas },
      {
        name: '文字层',
        opened: true,
        children: [
          {
            name: '主标题',
            canvas: titleCanvas,
            text: {
              text: brand.title,
              transform: [1, 0, 0, 1, W * L.title.x, H * L.title.y],
              style: {
                font: { name: 'Songti SC Bold' },
                fontSize: titleSize,
                fauxBold: true,
                fillColor: { r: 255, g: 210, b: 100 },
              },
            },
          },
          {
            name: '副标题1',
            canvas: sub1Canvas,
            text: {
              text: brand.sub1,
              transform: [1, 0, 0, 1, W * L.sub1.x, H * L.sub1.y],
              style: {
                font: { name: 'Songti SC Bold' },
                fontSize: Math.round(H * L.sub1.fontSize),
                fauxBold: true,
                fillColor: { r: 232, g: 168, b: 40 },
              },
            },
          },
          {
            name: '副标题2',
            canvas: sub2Canvas,
            text: {
              text: brand.sub2,
              transform: [1, 0, 0, 1, W * L.sub2.x, H * L.sub2.y],
              style: {
                font: { name: 'Songti SC Bold' },
                fontSize: Math.round(H * L.sub2.fontSize),
                fauxBold: true,
                fillColor: { r: 255, g: 230, b: 160 },
              },
            },
          },
          {
            name: '域名',
            canvas: domainCanvas,
            text: {
              text: brand.domain,
              transform: [1, 0, 0, 1, W * L.domain.x, H * L.domain.y],
              style: {
                font: { name: 'Hiragino Sans GB W6' },
                fontSize: Math.round(H * L.domain.fontSize),
                fauxBold: true,
                fillColor: { r: 255, g: 154, b: 40 },
              },
            },
          },
        ],
      },
    ],
  };

  const outPsd = path.join(__dirname, `横幅-${brand.id}.psd`);
  const buffer = writePsd(psd, { generateThumbnail: true, generatePreview: true });
  fs.writeFileSync(outPsd, Buffer.from(buffer));

  const preview = createCanvas(W, H);
  const ctx = preview.getContext('2d');
  ctx.drawImage(bgCanvas, 0, 0);
  ctx.drawImage(titleCanvas, 0, 0);
  ctx.drawImage(sub1Canvas, 0, 0);
  ctx.drawImage(sub2Canvas, 0, 0);
  ctx.drawImage(domainCanvas, 0, 0);
  const outPng = path.join(__dirname, `preview-${brand.id}.png`);
  fs.writeFileSync(outPng, preview.toBuffer('image/png'));

  // keep default names for liubowen
  if (brand.id === 'liubowen') {
    fs.copyFileSync(outPsd, path.join(__dirname, '横幅可编辑模板.psd'));
    fs.copyFileSync(outPng, path.join(__dirname, 'preview-with-text.png'));
  }

  console.log(`OK  ${brand.title} / ${brand.domain}`);
  console.log(`PSD ${outPsd}`);
  console.log(`PNG ${outPng}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
