"""Print generatorName, RandomSeed and LevelName from a 1.7.10 level.dat (gzip NBT). Usage: lc-leveldat.py <level.dat>"""
import gzip, struct, sys
d = gzip.open(sys.argv[1]).read()
def find(name, kind):
    key = struct.pack(">BH", kind, len(name)) + name.encode()
    i = d.find(key) + len(key)
    if kind == 8:
        n = struct.unpack(">H", d[i:i + 2])[0]; return d[i + 2:i + 2 + n].decode()
    return struct.unpack(">q", d[i:i + 8])[0]
print("generatorName=%s RandomSeed=%s LevelName=%s" % (find("generatorName", 8), find("RandomSeed", 4), find("LevelName", 8)))
