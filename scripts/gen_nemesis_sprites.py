#!/usr/bin/env python3
"""Generate Nemesis tank + boss-programmer block sprites with PIL.
Outputs to ../sprites/."""
from PIL import Image, ImageDraw, ImageFilter
import os, sys, math

OUT = os.path.join(os.path.dirname(__file__), "..", "sprites")
os.makedirs(OUT, exist_ok=True)

RED_DARK = (110, 12, 18, 255)
RED      = (190, 28, 32, 255)
RED_HI   = (235, 60, 60, 255)
BLACK    = (12, 12, 14, 255)
DARK     = (30, 28, 30, 255)
STEEL    = (70, 60, 64, 255)
STEEL_HI = (130, 110, 116, 255)
GLOW     = (255, 110, 80, 255)
WHITE_HI = (240, 230, 220, 255)

def make_nemesis():
    # 128x128 top-down tank. Up = forward (-Y direction in image).
    S = 128
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = S // 2

    # Treads (left + right) — long dark bars along the sides
    for tx in (cx - 56, cx + 38):
        d.rounded_rectangle((tx, 28, tx + 18, S - 16), radius=3, fill=BLACK, outline=DARK, width=1)
        for ty in range(32, S - 18, 5):
            d.line((tx + 2, ty, tx + 16, ty), fill=STEEL, width=1)
        # End bolts
        for ty in (32, S - 22):
            d.ellipse((tx + 3, ty, tx + 7, ty + 4), fill=STEEL_HI)
            d.ellipse((tx + 11, ty, tx + 15, ty + 4), fill=STEEL_HI)

    # Hull (red-black, taller than wide, like a real tank)
    d.rounded_rectangle((cx - 36, 30, cx + 36, S - 20), radius=6, fill=RED_DARK, outline=BLACK, width=2)
    d.rounded_rectangle((cx - 32, 36, cx + 32, S - 26), radius=4, fill=RED)
    # Armor seams
    d.line((cx - 28, 60, cx + 28, 60), fill=BLACK, width=1)
    d.line((cx - 28, S - 50, cx + 28, S - 50), fill=BLACK, width=1)
    # Center hull stripe (dark accent)
    d.rectangle((cx - 3, 36, cx + 3, S - 26), fill=RED_DARK)

    # Rear exhaust vents (bottom of hull)
    for ox in (-14, 14):
        d.rectangle((cx + ox - 4, S - 26, cx + ox + 4, S - 14), fill=BLACK, outline=DARK)
        d.line((cx + ox - 3, S - 23, cx + ox + 3, S - 23), fill=GLOW, width=1)
        d.line((cx + ox - 3, S - 19, cx + ox + 3, S - 19), fill=GLOW, width=1)

    # Dual heavy plasma cannons — drawn BEFORE turret base so turret covers their roots
    cannon_top = 2
    cannon_bot = cy + 6
    for ox in (-10, 10):
        # Outer barrel (thick)
        d.rectangle((cx + ox - 5, cannon_top, cx + ox + 5, cannon_bot), fill=BLACK)
        d.rectangle((cx + ox - 4, cannon_top + 1, cx + ox + 4, cannon_bot), fill=DARK)
        d.rectangle((cx + ox - 2, cannon_top + 2, cx + ox + 2, cannon_bot), fill=STEEL)
        # Muzzle housing
        d.rounded_rectangle((cx + ox - 6, cannon_top, cx + ox + 6, cannon_top + 6), radius=1, fill=BLACK, outline=RED_HI)
        # Muzzle glow
        d.ellipse((cx + ox - 3, cannon_top + 1, cx + ox + 3, cannon_top + 5), fill=GLOW)
        # Cooling fins along barrel
        for ry in range(cannon_top + 12, cannon_bot - 4, 7):
            d.rectangle((cx + ox - 6, ry, cx + ox + 6, ry + 2), fill=BLACK)

    # Shoulder missile pods — on the FRONT of the hull, flanking the cannons (front shoulders)
    for sx in (cx - 44, cx + 32):
        d.rounded_rectangle((sx, 32, sx + 12, 58), radius=2, fill=DARK, outline=BLACK, width=1)
        # 3 stacked missile tubes
        for i in range(3):
            ty0 = 35 + i * 8
            d.rectangle((sx + 2, ty0, sx + 10, ty0 + 5), fill=BLACK, outline=STEEL)
            d.ellipse((sx + 4, ty0 + 1, sx + 8, ty0 + 4), fill=RED_HI)

    # Central turret (sits on hull, covers cannon roots)
    d.ellipse((cx - 24, cy - 18, cx + 24, cy + 26), fill=BLACK)
    d.ellipse((cx - 22, cy - 16, cx + 22, cy + 24), fill=DARK, outline=BLACK)
    d.ellipse((cx - 18, cy - 12, cx + 18, cy + 20), fill=RED_DARK)
    d.ellipse((cx - 14, cy - 8, cx + 14, cy + 16), fill=RED)
    # Turret seam
    d.line((cx - 18, cy + 4, cx + 18, cy + 4), fill=BLACK, width=1)
    # Center cell socket (where -cell.png renders)
    d.ellipse((cx - 6, cy - 2, cx + 6, cy + 10), fill=BLACK, outline=RED_HI, width=1)

    # Edge red highlight
    edge = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ed = ImageDraw.Draw(edge)
    ed.rounded_rectangle((cx - 36, 30, cx + 36, S - 20), radius=6, outline=(255, 90, 70, 90), width=1)
    img = Image.alpha_composite(img, edge)

    img.save(os.path.join(OUT, "nemesis.png"))
    print("wrote nemesis.png")

def make_nemesis_cell():
    # 12x12 team-color cell shown center-of-turret
    S = 12
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((1, 1, S - 2, S - 2), fill=(255, 255, 255, 255))
    img.save(os.path.join(OUT, "nemesis-cell.png"))
    print("wrote nemesis-cell.png")

def make_boss_programmer():
    # 96x96 = 3x3 block (32 px/tile)
    S = 96
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Outer dark plate
    d.rounded_rectangle((2, 2, S - 3, S - 3), radius=4, fill=DARK, outline=BLACK, width=2)
    # Inner panel (red-black)
    d.rounded_rectangle((8, 8, S - 9, S - 9), radius=3, fill=RED_DARK, outline=BLACK, width=1)
    # Grid of "code lines" — horizontal bars representing program lines
    for i, y in enumerate(range(16, S - 14, 8)):
        w = [40, 28, 36, 20, 32, 26, 38, 24, 30][i % 9]
        d.rectangle((16, y, 16 + w, y + 3), fill=BLACK)
        d.rectangle((16, y, 16 + 3, y + 3), fill=GLOW)
    # Corner LEDs
    for (lx, ly) in ((10, 10), (S - 14, 10), (10, S - 14), (S - 14, S - 14)):
        d.ellipse((lx, ly, lx + 4, ly + 4), fill=GLOW)
    # Center "play" symbol
    d.polygon([(S // 2 - 4, S - 26), (S // 2 - 4, S - 14), (S // 2 + 6, S - 20)], fill=GLOW)
    img.save(os.path.join(OUT, "boss-programmer.png"))
    print("wrote boss-programmer.png")


def make_celestial():
    S = 144
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = S // 2

    GOLD       = (240, 200, 100, 255)
    GOLD_HI    = (255, 240, 180, 255)
    GOLD_DEEP  = (160, 120, 50, 255)
    WHITE      = (245, 245, 250, 255)
    WHITE_DIM  = (200, 210, 230, 255)
    CYAN       = (140, 220, 255, 255)
    CYAN_HI    = (220, 245, 255, 255)
    BLK        = (12, 14, 22, 255)
    SHADOW     = (40, 50, 80, 180)

    def wing(side):
        for layer, (length, ystart, col_in, col_out) in enumerate([
            (66, -20, WHITE_DIM, WHITE),
            (54, -8,  WHITE,     CYAN),
            (40, 4,   CYAN,      CYAN_HI),
        ]):
            tipx = cx + side * length
            tipy = cy + ystart + 18
            d.polygon([
                (cx + side * 6, cy + ystart),
                (tipx, tipy - 6),
                (tipx, tipy + 4),
                (cx + side * (length - 6), cy + ystart + 22),
                (cx + side * 4, cy + ystart + 16),
            ], fill=col_in, outline=BLK)
            for f in range(6):
                fx0 = cx + side * (6 + f * (length - 12) / 6)
                fy0 = cy + ystart + 2 + f * 2
                fx1 = cx + side * (6 + (f + 1) * (length - 12) / 6)
                fy1 = cy + ystart + 12 + f * 2
                d.line((fx0, fy0, fx1, fy1), fill=col_out, width=1)
    wing(-1); wing(1)

    # Halo
    halo_y = cy - 38
    for r in (28, 26):
        d.ellipse((cx - r, halo_y - r // 2, cx + r, halo_y + r // 2), outline=GOLD_DEEP, width=2)
    d.ellipse((cx - 24, halo_y - 10, cx + 24, halo_y + 10), outline=GOLD, width=2)
    d.ellipse((cx - 22, halo_y - 8, cx + 22, halo_y + 8), outline=GOLD_HI, width=1)

    # Body shadow under
    d.ellipse((cx - 16, cy - 14, cx + 16, cy + 30), fill=SHADOW)

    # Torso
    d.rounded_rectangle((cx - 14, cy - 18, cx + 14, cy + 26), radius=8, fill=WHITE_DIM, outline=BLK, width=1)
    d.rounded_rectangle((cx - 11, cy - 15, cx + 11, cy + 22), radius=6, fill=WHITE)
    d.polygon([(cx - 9, cy - 12), (cx + 9, cy - 12), (cx + 7, cy + 6), (cx, cy + 12), (cx - 7, cy + 6)],
              fill=GOLD, outline=GOLD_DEEP)
    # Chest gem
    d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=BLK, outline=CYAN_HI, width=1)
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=CYAN_HI)

    # Twin starlight emitters
    for ox in (-12, 12):
        d.rounded_rectangle((cx + ox - 5, cy - 28, cx + ox + 5, cy - 14), radius=2, fill=WHITE_DIM, outline=BLK, width=1)
        d.rounded_rectangle((cx + ox - 4, cy - 27, cx + ox + 4, cy - 15), radius=2, fill=WHITE)
        d.ellipse((cx + ox - 5, cy - 30, cx + ox + 5, cy - 22), outline=GOLD, width=1)
        d.ellipse((cx + ox - 4, cy - 29, cx + ox + 4, cy - 23), fill=BLK)
        d.ellipse((cx + ox - 3, cy - 28, cx + ox + 3, cy - 24), fill=CYAN_HI)
        d.ellipse((cx + ox - 1, cy - 27, cx + ox + 1, cy - 25), fill=WHITE)

    # Head
    d.ellipse((cx - 8, cy - 36, cx + 8, cy - 20), fill=WHITE_DIM, outline=BLK)
    d.ellipse((cx - 6, cy - 34, cx + 6, cy - 22), fill=WHITE)
    d.rectangle((cx - 6, cy - 30, cx + 6, cy - 26), fill=GOLD, outline=GOLD_DEEP)
    d.line((cx - 5, cy - 28, cx + 5, cy - 28), fill=CYAN_HI, width=1)

    # Robe taper
    d.polygon([(cx - 12, cy + 22), (cx + 12, cy + 22), (cx + 8, cy + 36), (cx - 8, cy + 36)],
              fill=WHITE_DIM, outline=BLK)
    d.polygon([(cx - 10, cy + 24), (cx + 10, cy + 24), (cx + 6, cy + 34), (cx - 6, cy + 34)], fill=WHITE)
    d.line((cx - 8, cy + 36, cx + 8, cy + 36), fill=GOLD, width=1)

    # Cosmic micro-stars
    for (sx, sy) in [(cx - 50, cy - 28), (cx + 48, cy - 24), (cx - 40, cy + 18), (cx + 38, cy + 20),
                     (cx - 58, cy + 4), (cx + 56, cy + 2), (cx - 30, cy - 36), (cx + 28, cy - 38)]:
        d.line((sx - 2, sy, sx + 2, sy), fill=CYAN_HI, width=1)
        d.line((sx, sy - 2, sx, sy + 2), fill=CYAN_HI, width=1)

    # Halo glow halo (composite under)
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for rr, alpha in ((34, 30), (28, 50), (22, 70)):
        gd.ellipse((cx - rr, halo_y - rr // 2 - 2, cx + rr, halo_y + rr // 2 + 2),
                   fill=(255, 240, 200, alpha))
    img = Image.alpha_composite(glow, img)
    img.save(os.path.join(OUT, "celestial.png"))
    print("wrote celestial.png")

    cell = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cell)
    cd.ellipse((1, 1, 8, 8), fill=(255, 255, 255, 255))
    cell.save(os.path.join(OUT, "celestial-cell.png"))
    print("wrote celestial-cell.png")


def make_abyssal():
    # 180x180 top-down sea monster. 5x5 unit hit area in center, tentacles spread further.
    S = 180
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = S // 2

    DEEP   = (8, 18, 32, 255)
    HULL   = (18, 32, 52, 255)
    HULL_HI= (40, 72, 100, 255)
    TEAL   = (30, 110, 130, 255)
    TEAL_HI= (90, 200, 200, 255)
    GLOW   = (140, 240, 230, 255)
    PURPLE = (60, 20, 80, 255)
    PURPLE_HI = (180, 80, 220, 255)
    TOOTH  = (220, 220, 230, 255)
    BLK    = (4, 6, 12, 255)

    # Tentacles — 6 tentacles, each curved outward
    def tentacle(angle_deg, base_r, length, segs=8, base_w=10, tip_w=2):
        ang = math.radians(angle_deg)
        # Curve: each segment rotates a bit
        x, y = cx + math.cos(ang) * base_r, cy + math.sin(ang) * base_r
        cur_ang = ang
        twist = math.radians(8)  # per-segment twist
        for i in range(segs):
            t = i / max(1, segs - 1)
            w = base_w + (tip_w - base_w) * t
            step = length / segs
            nx = x + math.cos(cur_ang) * step
            ny = y + math.sin(cur_ang) * step
            # Draw segment as a thick line
            d.line((x, y, nx, ny), fill=HULL, width=int(w))
            d.line((x, y, nx, ny), fill=HULL_HI, width=max(1, int(w * 0.5)))
            # Suckers (cyan dots on underside)
            if i % 2 == 0 and w > 3:
                sx = (x + nx) / 2
                sy = (y + ny) / 2
                d.ellipse((sx - 2, sy - 2, sx + 2, sy + 2), fill=TEAL)
                d.ellipse((sx - 1, sy - 1, sx + 1, sy + 1), fill=TEAL_HI)
            x, y = nx, ny
            cur_ang += twist * (1 if i % 2 == 0 else -1) * 0.6
        # Tip (small glow)
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=GLOW)

    # 6 tentacles, evenly spread, rear/sides
    for i, ang in enumerate((40, 80, 140, 220, 280, 320)):
        tentacle(ang, base_r=44, length=58, segs=8, base_w=14, tip_w=3)

    # Body shadow (large dark disc)
    d.ellipse((cx - 60, cy - 56, cx + 60, cy + 60), fill=DEEP, outline=BLK, width=2)

    # Main body (dome) — front-leaning oblong
    d.ellipse((cx - 52, cy - 56, cx + 52, cy + 48), fill=HULL, outline=BLK, width=2)
    d.ellipse((cx - 46, cy - 50, cx + 46, cy + 42), fill=HULL_HI)
    # Inner darker dome to suggest curvature
    d.ellipse((cx - 38, cy - 44, cx + 38, cy + 30), fill=HULL, outline=DEEP)

    # Bioluminescent dorsal ridge spots
    for ox, oy in ((0, -32), (-10, -20), (10, -20), (-14, -4), (14, -4), (0, 8)):
        d.ellipse((cx + ox - 3, cy + oy - 3, cx + ox + 3, cy + oy + 3), fill=TEAL_HI)
        d.ellipse((cx + ox - 1, cy + oy - 1, cx + ox + 1, cy + oy + 1), fill=GLOW)

    # Maw at the front (bottom in image since up = forward)
    # Actually let's put the maw at the TOP since Mindustry units face up by default.
    # Top of sprite = forward.
    maw_y = cy - 40
    d.polygon([(cx - 22, maw_y - 4), (cx + 22, maw_y - 4),
               (cx + 16, maw_y + 8), (cx - 16, maw_y + 8)],
              fill=BLK, outline=PURPLE)
    # Teeth
    for tx in range(-18, 19, 6):
        d.polygon([(cx + tx, maw_y - 2), (cx + tx + 3, maw_y - 2), (cx + tx + 1, maw_y + 5)], fill=TOOTH)
    for tx in range(-15, 16, 6):
        d.polygon([(cx + tx, maw_y + 7), (cx + tx + 3, maw_y + 7), (cx + tx + 1, maw_y + 1)], fill=TOOTH)

    # Central eye (large, glowing)
    eye_y = cy - 8
    d.ellipse((cx - 16, eye_y - 14, cx + 16, eye_y + 14), fill=BLK, outline=PURPLE_HI, width=2)
    d.ellipse((cx - 12, eye_y - 10, cx + 12, eye_y + 10), fill=PURPLE)
    d.ellipse((cx - 8, eye_y - 6, cx + 8, eye_y + 6), fill=PURPLE_HI)
    d.ellipse((cx - 4, eye_y - 3, cx + 4, eye_y + 3), fill=GLOW)
    # Slit pupil
    d.line((cx, eye_y - 6, cx, eye_y + 6), fill=BLK, width=1)

    # Forward cannon vents (2) on each side of maw — where Abyss Cannon shells emerge
    for ox in (-26, 26):
        d.rectangle((cx + ox - 4, maw_y + 2, cx + ox + 4, maw_y + 14), fill=DEEP, outline=BLK)
        d.ellipse((cx + ox - 3, maw_y + 3, cx + ox + 3, maw_y + 9), fill=BLK, outline=TEAL_HI)
        d.ellipse((cx + ox - 1, maw_y + 5, cx + ox + 1, maw_y + 7), fill=GLOW)

    # Rear ridge plates
    for i, oy in enumerate((20, 30, 40)):
        wf = 30 - i * 6
        d.rounded_rectangle((cx - wf, cy + oy, cx + wf, cy + oy + 6), radius=2, fill=HULL_HI, outline=BLK)

    # Outer edge highlight glow
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((cx - 56, cy - 60, cx + 56, cy + 52), outline=(80, 200, 200, 90), width=2)
    img = Image.alpha_composite(img, glow)

    img.save(os.path.join(OUT, "abyssal.png"))
    print("wrote abyssal.png")

    # cell — small team-tint dot on top of eye iris
    cell = Image.new("RGBA", (14, 14), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cell)
    cd.ellipse((2, 2, 11, 11), fill=(255, 255, 255, 255))
    cell.save(os.path.join(OUT, "abyssal-cell.png"))
    print("wrote abyssal-cell.png")


if __name__ == "__main__":
    make_nemesis()
    make_nemesis_cell()
    make_boss_programmer()
    make_celestial()
    make_abyssal()
