#!/usr/bin/env python3
"""Generate Little Mandarin Classics brand icon assets."""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
IOS_ASSETS = ROOT / "iosApp" / "LittleMandarinClassics" / "Assets.xcassets"

COLORS = {
    "background": (255, 248, 236, 255),
    "primary": (184, 69, 53, 255),
    "primary_container": (255, 224, 214, 255),
    "secondary": (18, 107, 104, 255),
    "tertiary": (138, 97, 0, 255),
    "tertiary_container": (255, 244, 216, 255),
    "surface": (255, 255, 255, 255),
    "ink": (32, 37, 35, 255),
}


def rounded_rectangle(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def draw_mark(size: int, include_background: bool = True) -> Image.Image:
    scale = size / 1024
    image = Image.new("RGBA", (size, size), COLORS["background"] if include_background else (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    if include_background:
        rounded_rectangle(
            draw,
            (54 * scale, 54 * scale, 970 * scale, 970 * scale),
            220 * scale,
            COLORS["background"],
        )
        draw.ellipse(
            (640 * scale, 88 * scale, 912 * scale, 360 * scale),
            fill=COLORS["primary_container"],
        )
        draw.ellipse(
            (94 * scale, 680 * scale, 288 * scale, 874 * scale),
            fill=COLORS["tertiary_container"],
        )

    shadow = (32, 37, 35, 28)
    rounded_rectangle(
        draw,
        (216 * scale, 270 * scale, 826 * scale, 738 * scale),
        78 * scale,
        shadow,
    )

    # Open book, built from two warm pages with a vermilion spine.
    left_page = [
        (214 * scale, 252 * scale),
        (502 * scale, 322 * scale),
        (502 * scale, 782 * scale),
        (214 * scale, 704 * scale),
    ]
    right_page = [
        (522 * scale, 322 * scale),
        (810 * scale, 252 * scale),
        (810 * scale, 704 * scale),
        (522 * scale, 782 * scale),
    ]
    draw.polygon(left_page, fill=COLORS["surface"])
    draw.polygon(right_page, fill=COLORS["surface"])
    draw.line(
        left_page + [left_page[0]],
        fill=COLORS["primary"],
        width=max(4, round(20 * scale)),
        joint="curve",
    )
    draw.line(
        right_page + [right_page[0]],
        fill=COLORS["primary"],
        width=max(4, round(20 * scale)),
        joint="curve",
    )
    draw.line(
        (512 * scale, 326 * scale, 512 * scale, 774 * scale),
        fill=COLORS["secondary"],
        width=max(4, round(18 * scale)),
    )

    for offset in (0, 82, 164):
        draw.arc(
            (
                (276 + offset * 0.14) * scale,
                (366 + offset) * scale,
                (466 + offset * 0.08) * scale,
                (486 + offset) * scale,
            ),
            start=200,
            end=342,
            fill=(138, 97, 0, 120),
            width=max(2, round(9 * scale)),
        )
        draw.arc(
            (
                (558 - offset * 0.08) * scale,
                (366 + offset) * scale,
                (748 - offset * 0.14) * scale,
                (486 + offset) * scale,
            ),
            start=198,
            end=340,
            fill=(138, 97, 0, 120),
            width=max(2, round(9 * scale)),
        )

    # Peach-shaped seal: friendly Chinese-classics cue without relying on tiny text.
    peach_box = (608 * scale, 612 * scale, 838 * scale, 842 * scale)
    draw.ellipse(peach_box, fill=COLORS["primary"])
    draw.pieslice(
        (588 * scale, 560 * scale, 734 * scale, 706 * scale),
        280,
        80,
        fill=COLORS["primary"],
    )
    draw.pieslice(
        (712 * scale, 560 * scale, 858 * scale, 706 * scale),
        100,
        260,
        fill=COLORS["primary"],
    )
    draw.ellipse(
        (672 * scale, 672 * scale, 732 * scale, 732 * scale),
        fill=COLORS["primary_container"],
    )
    draw.arc(
        (672 * scale, 628 * scale, 780 * scale, 774 * scale),
        start=120,
        end=250,
        fill=COLORS["primary_container"],
        width=max(3, round(14 * scale)),
    )
    draw.ellipse(
        (736 * scale, 568 * scale, 854 * scale, 650 * scale),
        fill=COLORS["secondary"],
    )
    draw.arc(
        (748 * scale, 588 * scale, 840 * scale, 646 * scale),
        start=198,
        end=334,
        fill=(255, 255, 255, 130),
        width=max(2, round(7 * scale)),
    )

    return image


def resize_icon(base: Image.Image, size: int) -> Image.Image:
    return base.resize((size, size), Image.Resampling.LANCZOS).convert("RGBA")


def save_opaque_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    background = Image.new("RGBA", image.size, COLORS["background"])
    background.alpha_composite(image)
    background.convert("RGB").save(path, "PNG", optimize=True)


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def generate_android(base: Image.Image) -> None:
    transparent_mark = draw_mark(1024, include_background=False)
    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432),
    }
    for folder, (legacy_pixels, foreground_pixels) in densities.items():
        icon = resize_icon(base, legacy_pixels)
        save_opaque_png(icon, ANDROID_RES / folder / "ic_launcher.png")
        save_opaque_png(icon, ANDROID_RES / folder / "ic_launcher_round.png")
        save_png(
            resize_icon(transparent_mark, foreground_pixels),
            ANDROID_RES / folder / "ic_launcher_foreground.png",
        )


def generate_ios(base: Image.Image) -> None:
    appicon_dir = IOS_ASSETS / "AppIcon.appiconset"
    entries = [
        ("iphone", "20x20", "2x", 40, "AppIcon-20x20@2x.png"),
        ("iphone", "20x20", "3x", 60, "AppIcon-20x20@3x.png"),
        ("iphone", "29x29", "2x", 58, "AppIcon-29x29@2x.png"),
        ("iphone", "29x29", "3x", 87, "AppIcon-29x29@3x.png"),
        ("iphone", "40x40", "2x", 80, "AppIcon-40x40@2x.png"),
        ("iphone", "40x40", "3x", 120, "AppIcon-40x40@3x.png"),
        ("iphone", "60x60", "2x", 120, "AppIcon-60x60@2x.png"),
        ("iphone", "60x60", "3x", 180, "AppIcon-60x60@3x.png"),
        ("ipad", "20x20", "1x", 20, "AppIcon-20x20@1x-ipad.png"),
        ("ipad", "20x20", "2x", 40, "AppIcon-20x20@2x-ipad.png"),
        ("ipad", "29x29", "1x", 29, "AppIcon-29x29@1x-ipad.png"),
        ("ipad", "29x29", "2x", 58, "AppIcon-29x29@2x-ipad.png"),
        ("ipad", "40x40", "1x", 40, "AppIcon-40x40@1x-ipad.png"),
        ("ipad", "40x40", "2x", 80, "AppIcon-40x40@2x-ipad.png"),
        ("ipad", "76x76", "1x", 76, "AppIcon-76x76@1x-ipad.png"),
        ("ipad", "76x76", "2x", 152, "AppIcon-76x76@2x-ipad.png"),
        ("ipad", "83.5x83.5", "2x", 167, "AppIcon-83.5x83.5@2x-ipad.png"),
        ("ios-marketing", "1024x1024", "1x", 1024, "AppIcon-1024x1024@1x.png"),
    ]
    images = []
    for idiom, size, scale, pixels, filename in entries:
        save_opaque_png(resize_icon(base, pixels), appicon_dir / filename)
        images.append(
            {
                "filename": filename,
                "idiom": idiom,
                "scale": scale,
                "size": size,
            }
        )
    (appicon_dir / "Contents.json").write_text(
        json.dumps({"images": images, "info": {"author": "xcode", "version": 1}}, indent=2) + "\n",
        encoding="utf-8",
    )

    launch_dir = IOS_ASSETS / "LaunchMark.imageset"
    launch_entries = [
        ("1x", 128, "LaunchMark@1x.png"),
        ("2x", 256, "LaunchMark@2x.png"),
        ("3x", 384, "LaunchMark@3x.png"),
    ]
    transparent_mark = draw_mark(1024, include_background=False)
    launch_images = []
    for scale, pixels, filename in launch_entries:
        save_png(resize_icon(transparent_mark, pixels), launch_dir / filename)
        launch_images.append(
            {
                "filename": filename,
                "idiom": "universal",
                "scale": scale,
            }
        )
    (launch_dir / "Contents.json").write_text(
        json.dumps({"images": launch_images, "info": {"author": "xcode", "version": 1}}, indent=2) + "\n",
        encoding="utf-8",
    )

    launch_background_dir = IOS_ASSETS / "LaunchBackground.colorset"
    (launch_background_dir / "Contents.json").write_text(
        json.dumps(
            {
                "colors": [
                    {
                        "idiom": "universal",
                        "color": {
                            "color-space": "srgb",
                            "components": {
                                "red": "1.000",
                                "green": "0.973",
                                "blue": "0.925",
                                "alpha": "1.000",
                            },
                        },
                    }
                ],
                "info": {"author": "xcode", "version": 1},
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def main() -> None:
    base = draw_mark(1024, include_background=True)
    generate_android(base)
    generate_ios(base)


if __name__ == "__main__":
    main()
