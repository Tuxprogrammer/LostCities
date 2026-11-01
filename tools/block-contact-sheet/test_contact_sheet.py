"""Offline tests for the contact-sheet renderer: texture loading from jars, frame cropping, cube faces and sheets."""

import io
import json
import os
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image  # noqa: E402
import contact_sheet as cs  # noqa: E402


def png(color, size=(16, 16)):
    buf = io.BytesIO()
    Image.new("RGBA", size, color).save(buf, "PNG")
    return buf.getvalue()


class Fixture:
    def __enter__(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.mods = Path(self.tmp.name)
        with zipfile.ZipFile(self.mods / "demo.jar", "w") as jar:
            jar.writestr("assets/demo/textures/blocks/red.png", png((255, 0, 0, 255)))
            jar.writestr("assets/demo/textures/blocks/blue.png", png((0, 0, 255, 255)))
            jar.writestr("assets/demo/textures/blocks/anim.png", png((0, 255, 0, 255), (16, 64)))
        return self

    def __exit__(self, *exc):
        self.tmp.cleanup()


class ContactSheetTest(unittest.TestCase):
    def test_loads_jar_textures_and_reports_missing(self):
        with Fixture() as f:
            source = cs.TextureSource(f.mods)
            self.assertEqual(source.load("demo.jar!assets/demo/textures/blocks/red.png").getpixel((3, 3)), (255, 0, 0, 255))
            self.assertIsNone(source.load("demo.jar!nope.png"))
            self.assertIsNone(source.load("absent.jar!x.png"))
            source.close()

    def test_vanilla_alias_uses_the_client_jar(self):
        with Fixture() as f:
            os.environ["LC_MC_CLIENT_JAR"] = str(f.mods / "demo.jar")
            try:
                source = cs.TextureSource(f.mods / "elsewhere")
                self.assertIsNotNone(source.load("minecraft.jar!assets/demo/textures/blocks/blue.png"))
                source.close()
            finally:
                del os.environ["LC_MC_CLIENT_JAR"]

    def test_animated_strip_uses_first_frame(self):
        with Fixture() as f:
            source = cs.TextureSource(f.mods)
            self.assertEqual(source.load("demo.jar!assets/demo/textures/blocks/anim.png").size, (16, 16))
            source.close()

    def test_cube_faces_are_textured_and_shaded(self):
        red, blue = Image.new("RGBA", (16, 16), (200, 0, 0, 255)), Image.new("RGBA", (16, 16), (0, 0, 200, 255))
        image = cs.cube(red, blue, 64)
        self.assertEqual(image.getpixel((32, 16)), (200, 0, 0, 255))
        self.assertEqual(image.getpixel((12, 40)), (0, 0, int(200 * cs.LEFT_SHADE), 255))
        self.assertEqual(image.getpixel((52, 40)), (0, 0, int(200 * cs.RIGHT_SHADE), 255))
        self.assertEqual(image.getpixel((1, 1))[3], 0)

    def test_sheet_renders_every_entry(self):
        with Fixture() as f:
            sheet = {"title": "demo", "entries": [
                {"label": "demo:red:0", "all": "demo.jar!assets/demo/textures/blocks/red.png"},
                {"label": "demo:mixed:1", "top": "demo.jar!assets/demo/textures/blocks/red.png",
                 "side": "demo.jar!assets/demo/textures/blocks/blue.png"},
                {"label": "demo:gone:0", "all": "demo.jar!missing.png"},
            ]}
            path = f.mods / "sheet.json"
            path.write_text(json.dumps(sheet))
            out = f.mods / "out" / "sheet.png"
            self.assertEqual(cs.main([str(path), "--mods", str(f.mods), "--out", str(out), "--cols", "2"]), 0)
            image = Image.open(out)
            self.assertEqual(image.size[0], 2 * (96 + 2 * cs.PADDING + 40))
            source = cs.TextureSource(f.mods)
            _, missing = cs.render(sheet, source)
            source.close()
            self.assertEqual(missing, ["demo:gone:0"])


if __name__ == "__main__":
    unittest.main()
