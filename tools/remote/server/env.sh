# Lost Cities: New Horizons toolchain on this box. Source me. Owned by LostCities sessions only.
# Everything points inside this directory (the remote root, LC_REMOTE_ROOT on the laptop); nothing here depends on another project's dirs.
LC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export LC
export JAVA_HOME="$LC/shared/jdk21"      # runs Gradle 9 (needs 17+); RFG toolchains jdk8 to compile
export GRADLE_USER_HOME="$LC/gradle"     # its gradle.properties lists our JDKs and disables auto-download
XV="$LC/shared/xvfb"
export PATH="$JAVA_HOME/bin:$XV/usr/bin:$XV/usr/bin/X11:$LC/bin:$PATH"
export LD_LIBRARY_PATH="$XV/usr/lib/x86_64-linux-gnu:$XV/usr/lib:${LD_LIBRARY_PATH:-}"
export XKB_DIR=/usr/share/X11/xkb
export LC_JDK8="$LC/shared/jdk8" LC_JDK17="$LC/shared/jdk17" LC_JDK21="$LC/shared/jdk21" LC_JDK25="$LC/shared/jdk25"
# Some 1.7.10 mods and tools look under ${user.home}/.gradle (MCP mappings) or write ${user.home}/.java;
# point user.home at our shim, whose .gradle links to $GRADLE_USER_HOME.
export JAVA_TOOL_OPTIONS="-Duser.home=$LC/shared/home"
# Our ranges on this shared box: Xvfb displays :80-:89, TCP ports 25800-25899
# (25800 real-pack server, 25810+ dev servers).

# lc_xvfb N: start Xvfb on :N (80-89 only), pid in $LC/logs/xvfb-N.pid. Stop it with lc_xvfb_stop N.
lc_xvfb() {
    local n="$1"
    if ! [[ "$n" =~ ^8[0-9]$ ]]; then echo "lc_xvfb: display must be 80-89, got '$n'" >&2; return 2; fi
    if [ -e "/tmp/.X11-unix/X$n" ] || [ -e "/tmp/.X$n-lock" ]; then echo "lc_xvfb: :$n already in use" >&2; return 3; fi
    nohup "$XV/usr/bin/Xvfb" ":$n" -screen 0 1600x900x24 -xkbdir "$XKB_DIR" -nolisten tcp \
        +extension RANDR +extension GLX > "$LC/logs/xvfb-$n.log" 2>&1 &
    echo $! > "$LC/logs/xvfb-$n.pid"
    sleep 2
    if kill -0 "$(cat "$LC/logs/xvfb-$n.pid")" 2>/dev/null; then echo "Xvfb :$n up (pid $(cat "$LC/logs/xvfb-$n.pid"))"; else echo "Xvfb :$n failed, see $LC/logs/xvfb-$n.log" >&2; return 1; fi
}
# lc_xvfb_stop N: stop only the Xvfb we started on :N (by its pid file).
lc_xvfb_stop() {
    local f="$LC/logs/xvfb-$1.pid"
    [ -f "$f" ] || { echo "no pid file $f" >&2; return 1; }
    kill "$(cat "$f")" 2>/dev/null; rm -f "$f"
}
