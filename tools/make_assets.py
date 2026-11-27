#!/usr/bin/env python3
"""Generates Void Launcher's raster artwork (TV banner, README hero image, Windows icon).

Run from the repo root:  python3 tools/make_assets.py     (needs Pillow)
The Android app icons are hand-written vector drawables and are not generated here.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BG1, BG2 = (7, 9, 26), (26, 15, 60)
PURPLE, LILAC = (124, 92, 255), (185, 168, 255)
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def gradient(w, h):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(BG1[i] + (BG2[i] - BG1[i]) * t) for i in range(3)) + (255,))
    return img


def ring(img, cx, cy, r, color=PURPLE, arrow=False):
    w, h = img.size
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([cx - r * 1.05, cy - r * 1.05, cx + r * 1.05, cy + r * 1.05],
                                 outline=color + (255,), width=max(2, int(r * 0.28)))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(r * 0.35)))
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (255,))
    ir = r * 0.74
    d.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=(0, 0, 0, 255))
    gr = r * 0.88
    d.arc([cx - gr, cy - gr, cx + gr, cy + gr], start=200, end=260, fill=LILAC + (255,), width=max(2, int(r * 0.06)))
    if arrow:  # down arrow = "install"
        s, wd = ir * 0.5, max(2, int(r * 0.12))
        d.line([(cx, cy - s), (cx, cy + s * 0.8)], fill=(255, 255, 255, 255), width=wd)
        d.line([(cx - s * 0.8, cy + s * 0.05), (cx, cy + s * 0.85), (cx + s * 0.8, cy + s * 0.05)],
               fill=(255, 255, 255, 255), width=wd, joint="curve")


def spaced(d, xy, text, font, fill, spacing):
    x, y = xy
    for ch in text:
        d.text((x, y), ch, font=font, fill=fill)
        x += d.textlength(ch, font=font) + spacing


def banner(scale):
    w, h = 640 * scale, 360 * scale
    img = gradient(w, h)
    ring(img, 150 * scale, 180 * scale, 86 * scale)
    d = ImageDraw.Draw(img)
    spaced(d, (275 * scale, 78 * scale), "VOID", ImageFont.truetype(BOLD, 96 * scale), (255, 255, 255, 255), 6 * scale)
    spaced(d, (279 * scale, 196 * scale), "LAUNCHER", ImageFont.truetype(BOLD, 44 * scale), LILAC + (255,), 7 * scale)
    d.text((280 * scale, 262 * scale), "Android TV home screen", font=ImageFont.truetype(REG, 22 * scale), fill=(200, 200, 225, 255))
    return img


def app_icon(size, arrow):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(img).rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.22, fill=(11, 14, 26, 255))
    ring(img, size / 2, size / 2, size * 0.30, arrow=arrow)
    return img


if __name__ == "__main__":
    import os
    os.makedirs("app/src/main/res/drawable-xhdpi", exist_ok=True)
    banner(2).convert("RGB").save("app/src/main/res/drawable-xhdpi/banner.png", optimize=True)   # 640x360 = 320x180dp @xhdpi
    banner(3).convert("RGB").save("docs/banner.png", optimize=True)                               # README hero
    app_icon(256, True).save("setup-windows/app.ico", sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print("assets written")
