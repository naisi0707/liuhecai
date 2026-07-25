#!/usr/bin/env python3
"""把「干净背景 + 可改文案」合成动态 GIF。

用法:
  # 生成 brands.json 里全部品牌
  .venv-banner/bin/python tools/banner-template/make_gif.py --all

  # 单个品牌
  .venv-banner/bin/python tools/banner-template/make_gif.py --id liubowen

  # 临时改文案
  .venv-banner/bin/python tools/banner-template/make_gif.py \\
      --title 刘伯温 --domain 585520.xyz --sub1 帝皇之师刘伯温 --sub2 "神机妙算 解密财富"
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
BG = ROOT / "background.png"
CFG = ROOT / "brands.json"
OUT_DIR = ROOT / "out"
FONT_SONG = Path("/System/Library/Fonts/Supplemental/Songti.ttc")
FONT_HEI = Path("/System/Library/Fonts/Hiragino Sans GB.ttc")


def load_font(size: int, prefer_hei: bool = False) -> ImageFont.FreeTypeFont:
    path = FONT_HEI if prefer_hei and FONT_HEI.exists() else FONT_SONG
    if not path.exists():
        return ImageFont.load_default()
    try:
        return ImageFont.truetype(str(path), size=size, index=0)
    except OSError:
        return ImageFont.truetype(str(path), size=size)


def draw_gradient_text(
    base: Image.Image,
    text: str,
    xy: tuple[float, float],
    font: ImageFont.ImageFont,
    colors: list[tuple[int, int, int]],
    stroke: tuple[int, int, int],
    stroke_w: int,
    letter_spacing: int = 0,
) -> None:
    """Draw gold-style stroked text centered at xy."""
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)

    if letter_spacing and len(text) > 1:
        widths = [d.textlength(ch, font=font) for ch in text]
        total = sum(widths) + letter_spacing * (len(text) - 1)
        x = xy[0] - total / 2
        y = xy[1]
        positions = []
        for ch, w in zip(text, widths):
            positions.append((ch, x + w / 2, y))
            x += w + letter_spacing
    else:
        bbox = d.textbbox((0, 0), text, font=font, stroke_width=stroke_w)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        positions = [(text, xy[0], xy[1])]
        # single blob — measure for mask height
        _ = (tw, th)

    # stroke + fill per chunk
    for chunk, cx, cy in positions:
        d.text(
            (cx, cy),
            chunk,
            font=font,
            fill=colors[len(colors) // 2] + (255,),
            stroke_width=stroke_w,
            stroke_fill=stroke + (255,),
            anchor="mm",
        )

    # gold gradient remap via luminance mask on alpha of text
    alpha = overlay.split()[-1]
    grad = Image.new("RGBA", base.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    # approximate vertical gradient over text bbox
    # rebuild with top highlight
    for chunk, cx, cy in positions:
        # sample vertical gradient by drawing twice
        top = colors[0]
        mid = colors[1] if len(colors) > 1 else colors[0]
        bot = colors[-1]
        gd.text((cx, cy), chunk, font=font, fill=mid + (255,), anchor="mm")
        # highlight
        hi = Image.new("RGBA", base.size, (0, 0, 0, 0))
        hd = ImageDraw.Draw(hi)
        hd.text((cx, cy - 1), chunk, font=font, fill=top + (160,), anchor="mm")
        grad = Image.alpha_composite(grad, hi)

    # use stroked overlay alpha with gold mid color, then composite highlight
    gold = Image.new("RGBA", base.size, colors[1] + (0,))
    gold.putalpha(alpha)
    # darken bottom: multiply soft
    base.alpha_composite(Image.alpha_composite(gold, grad))


def render_static(brand: dict, layout: dict, size: tuple[int, int], t: float = 0.0) -> Image.Image:
    W, H = size
    bg = Image.open(BG).convert("RGBA").resize((W, H), Image.Resampling.LANCZOS)
    frame = bg.copy()

    title = brand["title"]
    title_size = int(H * layout["title"]["fontSize"] * (0.82 if len(title) > 3 else 1))
    spacing = int(title_size * 0.12) if len(title) <= 4 else 0

    draw_gradient_text(
        frame,
        title,
        (W * layout["title"]["x"], H * layout["title"]["y"]),
        load_font(title_size),
        colors=[(255, 248, 224), (255, 210, 100), (240, 168, 40), (196, 90, 24)],
        stroke=(90, 26, 8),
        stroke_w=max(4, title_size // 14),
        letter_spacing=spacing,
    )
    draw_gradient_text(
        frame,
        brand["sub1"],
        (W * layout["sub1"]["x"], H * layout["sub1"]["y"]),
        load_font(int(H * layout["sub1"]["fontSize"])),
        colors=[(255, 233, 168), (212, 136, 24)],
        stroke=(74, 16, 16),
        stroke_w=2,
    )
    draw_gradient_text(
        frame,
        brand["sub2"],
        (W * layout["sub2"]["x"], H * layout["sub2"]["y"]),
        load_font(int(H * layout["sub2"]["fontSize"])),
        colors=[(255, 242, 200), (224, 168, 56)],
        stroke=(58, 8, 8),
        stroke_w=2,
    )
    draw_gradient_text(
        frame,
        brand["domain"],
        (W * layout["domain"]["x"], H * layout["domain"]["y"]),
        load_font(int(H * layout["domain"]["fontSize"]), prefer_hei=True),
        colors=[(255, 216, 152), (255, 154, 48), (224, 88, 16)],
        stroke=(42, 16, 5),
        stroke_w=4,
    )

    # animation overlays
    breath = 1.0 + 0.03 * math.sin(t * 2 * math.pi)
    frame = ImageEnhance.Brightness(frame).enhance(breath)

    # shimmer
    sweep_x = int(-W * 0.35 + W * 1.7 * t)
    shimmer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    band = 48
    for x in range(band):
        a = int(50 * math.sin(math.pi * x / band))
        if a <= 0:
            continue
        xx = sweep_x + x
        for yy in range(0, int(H * 0.55)):
            px = xx + int((yy - H / 2) * 0.35)
            if 0 <= px < W:
                shimmer.putpixel((px, yy), (255, 230, 150, a))
    frame = Image.alpha_composite(frame, shimmer.filter(ImageFilter.GaussianBlur(4)))

    # domain pulse
    pulse = 0.5 + 0.5 * math.sin(t * 2 * math.pi)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy = int(W * 0.5), int(H * 0.88)
    gd.ellipse([cx - 170, cy - 28, cx + 170, cy + 28], fill=(255, 80, 40, int(24 + 36 * pulse)))
    frame = Image.alpha_composite(frame, glow.filter(ImageFilter.GaussianBlur(12)))

    # sparkles near title
    spark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spark)
    for si, (sx, sy) in enumerate(
        [(180, 40), (260, 28), (360, 50), (450, 30), (300, 70), (220, 75), (400, 65)]
    ):
        a = max(0.0, math.sin(((t + si * 0.13) % 1.0) * math.pi))
        r = 1 + int(2.5 * a)
        aa = int(170 * a)
        if aa > 25:
            sd.ellipse([sx - r, sy - r, sx + r, sy + r], fill=(255, 245, 200, aa))
    frame = Image.alpha_composite(frame, spark)
    return frame.convert("RGB")


def save_gif(brand: dict, layout: dict, size: tuple[int, int], out: Path, frames: int = 16) -> None:
    rgb_frames = [render_static(brand, layout, size, i / frames) for i in range(frames)]
    pal = rgb_frames[0].quantize(colors=96, method=Image.Quantize.MEDIANCUT)
    q = [f.quantize(palette=pal, dither=Image.Dither.NONE) for f in rgb_frames]
    out.parent.mkdir(parents=True, exist_ok=True)
    q[0].save(
        out,
        save_all=True,
        append_images=q[1:],
        duration=100,
        loop=0,
        optimize=True,
        disposal=2,
    )
    rgb_frames[4].save(out.with_suffix(".png"))
    print(f"OK  {brand['title']} {brand['domain']} -> {out} ({out.stat().st_size/1024:.0f}KB)")


def main() -> None:
    ap = argparse.ArgumentParser(description="可编辑文案横幅 GIF 生成器")
    ap.add_argument("--all", action="store_true", help="生成 brands.json 全部品牌")
    ap.add_argument("--id", help="品牌 id，如 liubowen")
    ap.add_argument("--title")
    ap.add_argument("--sub1")
    ap.add_argument("--sub2")
    ap.add_argument("--domain")
    ap.add_argument("--out", type=Path, help="输出 gif 路径")
    args = ap.parse_args()

    cfg = json.loads(CFG.read_text(encoding="utf-8"))
    layout = cfg["layout"]
    size = tuple(cfg["size"])

    if args.all:
        for brand in cfg["brands"]:
            save_gif(brand, layout, size, OUT_DIR / f"{brand['id']}-banner.gif")
        return

    brand = next((b for b in cfg["brands"] if b["id"] == (args.id or "liubowen")), cfg["brands"][0]).copy()
    if args.title:
        brand["title"] = args.title
    if args.sub1:
        brand["sub1"] = args.sub1
    if args.sub2:
        brand["sub2"] = args.sub2
    if args.domain:
        brand["domain"] = args.domain

    out = args.out or (OUT_DIR / f"{brand.get('id', 'custom')}-banner.gif")
    save_gif(brand, layout, size, out)


if __name__ == "__main__":
    main()
