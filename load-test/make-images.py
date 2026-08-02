#!/usr/bin/env python3
"""부하 테스트용 이미지를 생성한다 (small 145KB / medium 1.0MB / large 4.5MB).

이미지 자체는 git 에 넣지 않는다. 아래로 언제든 동일하게 재생성된다.
난수 시드를 고정했으므로 몇 번을 돌려도 같은 파일이 나온다 - 측정 조건을
고정하려면 이게 중요하다.

    pip install pillow
    python3 load-test/make-images.py
"""
import random
from pathlib import Path

from PIL import Image

SPECS = {
    "small": (640, 480, 80),
    "medium": (1600, 1200, 85),
    "large": (3000, 2200, 92),
}

OUT_DIR = Path(__file__).parent / "data"


def build(width: int, height: int, quality: int, path: Path) -> None:
    image = Image.new("RGB", (width, height))
    pixels = image.load()
    random.seed(42)
    # 단색 이미지는 JPEG 이 극단적으로 압축해버려 실제 사진 크기가 안 나온다.
    # 4x4 블록 노이즈를 넣어 현실적인 파일 크기를 만든다.
    for y in range(0, height, 4):
        for x in range(0, width, 4):
            color = (random.randint(0, 255), random.randint(0, 255), random.randint(0, 255))
            for dy in range(4):
                for dx in range(4):
                    if x + dx < width and y + dy < height:
                        pixels[x + dx, y + dy] = color
    image.save(path, quality=quality)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, (width, height, quality) in SPECS.items():
        path = OUT_DIR / f"drone-{name}.jpg"
        build(width, height, quality, path)
        print(f"{path.name}: {path.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
