"""Not So Vanilla modpack icon: 64x64 pixel art, scaled to 512x512 with nearest-neighbour.

A kid with a torch on the first night. A village glows on the far hill, eyes watch from the trees,
and the stars above form a 9.
"""
import math, os, random
from PIL import Image

N = 64
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)))
img = Image.new("RGB", (N, N))
px = img.load()


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# Sky: banded gradient with ordered dithering between bands (pixel-art look).
SKY = [(8, 10, 32), (16, 18, 52), (30, 26, 72), (52, 36, 92), (84, 50, 104)]
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
for y in range(N):
    f = y / 44 * (len(SKY) - 1)
    i = min(int(f), len(SKY) - 2)
    frac = f - i
    for x in range(N):
        px[x, y] = SKY[i + 1] if frac * 16 > BAYER[y % 4][x % 4] + 0.5 else SKY[i]

# Background stars.
rnd = random.Random(9)
for _ in range(34):
    x, y = rnd.randrange(N), rnd.randrange(34)
    px[x, y] = mix(px[x, y], (200, 205, 255), rnd.choice([0.45, 0.6, 0.85]))

# Constellation "9": a loop with a tail, bright stars joined by faint lines.
NINE = [(13, 5), (17, 4), (20, 7), (20, 11), (17, 13), (13, 12), (11, 8), (13, 5), None,
        (20, 11), (20, 16), (18, 20), (14, 21)]


def line(a, b, col, t):
    steps = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
    for s in range(steps + 1):
        x = round(a[0] + (b[0] - a[0]) * s / steps)
        y = round(a[1] + (b[1] - a[1]) * s / steps)
        px[x, y] = mix(px[x, y], col, t)


prev = None
for p in NINE:
    if p and prev:
        line(prev, p, (150, 160, 230), 0.35)
    prev = p
for p in {p for p in NINE if p}:
    x, y = p
    px[x, y] = (255, 250, 220)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        px[x + dx, y + dy] = mix(px[x + dx, y + dy], (210, 210, 255), 0.5)

# Crescent moon with a soft halo.
mx, my, r = 49, 12, 6.5
for y in range(N):
    for x in range(N):
        d = math.hypot(x - mx, y - my)
        if d < r and math.hypot(x - mx + 3.2, y - my + 1.8) > r - 0.6:
            px[x, y] = (246, 236, 196) if d < r - 1.2 else (214, 200, 160)
        elif r <= d < r + 5:
            px[x, y] = mix(px[x, y], (120, 110, 160), 0.22 * (1 - (d - r) / 5))

# Far hill with the village.
FAR = (24, 22, 48)
far_h = [round(41 - 4 * math.sin((x - 8) / 11) - 2 * math.sin(x / 4.3)) for x in range(N)]
for x in range(N):
    for y in range(far_h[x], N):
        px[x, y] = FAR
LIT = (255, 196, 92)


def house(x0, w, h, roof=True, windows=()):
    base = far_h[x0 + w // 2] + 1
    for x in range(x0, x0 + w):
        for y in range(base - h, base + 1):
            px[x, y] = (34, 30, 58)
    if roof:
        for k in range(w // 2 + 1):
            for x in range(x0 + k, x0 + w - k):
                px[x, base - h - 1 - k] = (34, 30, 58)
    for wx, wy in windows:
        px[x0 + wx, base - h + wy] = LIT


house(31, 5, 3, windows=[(2, 1)])
house(37, 3, 5, roof=False, windows=[(1, 1), (1, 3)])  # tower
px[38, far_h[38] + 1 - 6] = (34, 30, 58)
house(41, 5, 2, windows=[(1, 1), (3, 1)])
house(26, 4, 2, windows=[(1, 1)])
# Warm glow above the village.
for y in range(N):
    for x in range(N):
        d = math.hypot((x - 37) / 1.6, y - (far_h[37] - 2))
        if d < 9 and y < far_h[x]:
            px[x, y] = mix(px[x, y], (120, 70, 70), 0.25 * (1 - d / 9))

# Pine forest on the right, near.
TREE = (10, 12, 24)


def pine(cx, base, h):
    for k in range(h):
        half = 1 + k * 4 // h if k % 3 else k * 4 // h
        for x in range(cx - half, cx + half + 1):
            if 0 <= x < N:
                px[x, base - h + k] = TREE
    px[cx, base] = TREE


for cx, h in [(52, 22), (58, 26), (63, 20), (47, 15)]:
    pine(cx, 52, h)

# Near ground.
GROUND, EDGE = (12, 11, 22), (26, 32, 40)
near_h = [round(51 + 3 * math.sin((x + 6) / 9)) for x in range(N)]
for x in range(N):
    for y in range(near_h[x], N):
        px[x, y] = GROUND
    px[x, near_h[x]] = EDGE

# Eyes in the trees.
for ex, ey, col in [(53, 44, (255, 60, 50)), (55, 44, (255, 60, 50)), (59, 39, (150, 255, 110)), (61, 39, (150, 255, 110))]:
    px[ex, ey] = col

# The kid with a torch, standing on the near ground.
kx = 17
feet = near_h[kx] - 1
KID = (6, 6, 14)
kid = [  # rows top to bottom, '#' = body
    " ### ",
    " ### ",
    " ### ",
    "  #  ",
    "#####",
    "#####",
    "#####",
    " ### ",
    " # # ",
    " # # ",
    " # # ",
]
top = feet - len(kid) + 1
for j, row in enumerate(kid):
    for i, c in enumerate(row):
        if c == "#":
            px[kx - 2 + i, top + j] = KID
# Arm raised to the torch.
fx, fy = kx + 4, top - 1
px[kx + 3, top + 4] = KID
px[kx + 4, top + 3] = KID
for y in range(fy + 1, fy + 4):
    px[fx, y] = (92, 60, 34)  # stick

# Torch light: warm falloff over everything, strongest on the ground nearby.
for y in range(N):
    for x in range(N):
        d = math.hypot(x - fx, (y - fy) * 1.1)
        if d < 16:
            t = (1 - d / 16) ** 2
            if px[x, y] in (KID,):
                continue
            px[x, y] = mix(px[x, y], (255, 150, 60), 0.55 * t)
# Flame.
# Teardrop flame leaning slightly left.
for (x, y, c) in [(fx, fy - 3, (230, 80, 30)), (fx - 1, fy - 2, (230, 80, 30)), (fx, fy - 2, (255, 140, 44)),
                  (fx - 1, fy - 1, (255, 130, 40)), (fx, fy - 1, (255, 210, 90)), (fx + 1, fy - 1, (240, 100, 34)),
                  (fx - 1, fy, (255, 150, 50)), (fx, fy, (255, 246, 190)), (fx + 1, fy, (255, 150, 50))]:
    px[x, y] = c

img.save(os.path.join(OUT, "icon-64.png"))
big = img.resize((512, 512), Image.NEAREST)
big.save(os.path.join(OUT, "icon.png"), optimize=True)
print("ok", os.path.getsize(os.path.join(OUT, "icon.png")), "bytes")
