#!/usr/bin/env python3
"""Converts the lichess cburnett piece SVGs (GPLv2+, Colin M.L. Burnett)
into Android VectorDrawables: app/src/main/res/drawable/piece_<color><kind>.xml.

Usage: scripts/svg_pieces_to_vectors.py <dir with wK.svg ... bP.svg>
"""
import os
import sys
import xml.etree.ElementTree as ET

NS = "{http://www.w3.org/2000/svg}"
INHERITED = ["fill", "fill-rule", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin"]
DEFAULTS = {"fill": "#000", "fill-rule": "nonzero", "stroke": "none", "stroke-width": "1",
            "stroke-linecap": "butt", "stroke-linejoin": "miter"}


def color(c):
    if c in (None, "none"):
        return None
    c = c.strip()
    if c.startswith("#") and len(c) == 4:
        c = "#" + "".join(ch * 2 for ch in c[1:])
    return {"#000000": "#FF000000", "#ffffff": "#FFFFFFFF"}.get(c.lower(), "#FF" + c[1:].upper())


def walk(el, inherited, out):
    attrs = dict(inherited)
    for k in INHERITED:
        if k in el.attrib:
            attrs[k] = el.attrib[k]
    tag = el.tag.replace(NS, "")
    if tag == "path":
        out.append((el.attrib["d"], attrs))
    elif tag == "circle":
        cx, cy, r = (float(el.attrib[k]) for k in ("cx", "cy", "r"))
        d = f"M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z"
        out.append((d, attrs))
    for child in el:
        walk(child, attrs, out)


def convert(svg_path):
    root = ET.parse(svg_path).getroot()
    paths = []
    walk(root, DEFAULTS, paths)
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<!-- Generated from lichess cburnett set (GPLv2+, Colin M.L. Burnett). -->',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="45dp" android:height="45dp"',
             '    android:viewportWidth="45" android:viewportHeight="45">']
    for d, a in paths:
        p = [f'android:pathData="{d}"']
        fill, stroke = color(a["fill"]), color(a["stroke"])
        if fill:
            p.append(f'android:fillColor="{fill}"')
            if a["fill-rule"] == "evenodd":
                p.append('android:fillType="evenOdd"')
        if stroke:
            p += [f'android:strokeColor="{stroke}"',
                  f'android:strokeWidth="{a["stroke-width"]}"',
                  f'android:strokeLineCap="{a["stroke-linecap"]}"',
                  f'android:strokeLineJoin="{a["stroke-linejoin"]}"']
        lines.append("    <path\n        " + "\n        ".join(p) + " />")
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def main():
    src = sys.argv[1]
    out_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable")
    for c in "wb":
        for k in "KQRBNP":
            xml = convert(os.path.join(src, f"{c}{k}.svg"))
            with open(os.path.join(out_dir, f"piece_{c}{k.lower()}.xml"), "w") as f:
                f.write(xml)
    print("ok")


if __name__ == "__main__":
    main()
