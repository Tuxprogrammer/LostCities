#!/usr/bin/env python3
"""Offline tests for measure.py: label expansion and the color measure. Run: python3 -I tools/block-colors/test_measure.py"""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from PIL import Image  # noqa: E402

import measure  # noqa: E402


class ExpandTest(unittest.TestCase):

    def test_single_meta(self):
        self.assertEqual(measure.expand("minecraft:stone:0"), ["minecraft:stone:0"])

    def test_range(self):
        self.assertEqual(measure.expand("mod:block:2-4"), ["mod:block:2", "mod:block:3", "mod:block:4"])

    def test_no_meta_is_meta_zero(self):
        self.assertEqual(measure.expand("mod:block"), ["mod:block:0"])

    def test_free_text_is_skipped(self):
        self.assertEqual(measure.expand("ExtraTrees:log meta=39"), [])
        self.assertEqual(measure.expand("ForgeMultipart part pr_cagelamp2"), [])


class MeasureTest(unittest.TestCase):

    def test_mean_in_linear_light(self):
        image = Image.new("RGBA", (2, 1))
        image.putpixel((0, 0), (0, 0, 0, 255))
        image.putpixel((1, 0), (255, 255, 255, 255))
        r, g, b, coverage = measure.measure(image)
        # The mean of black and white in linear light is 0.5, which is sRGB 188, not 128.
        self.assertEqual((r, g, b), (188, 188, 188))
        self.assertEqual(coverage, 100)

    def test_transparent_pixels_are_left_out(self):
        image = Image.new("RGBA", (2, 2), (0, 0, 0, 0))
        image.putpixel((0, 0), (200, 100, 50, 255))
        r, g, b, coverage = measure.measure(image)
        self.assertEqual((r, g, b), (200, 100, 50))
        self.assertEqual(coverage, 25)

    def test_fully_transparent_is_none(self):
        self.assertIsNone(measure.measure(Image.new("RGBA", (2, 2), (0, 0, 0, 0))))


if __name__ == "__main__":
    unittest.main()
