"""Exercise the installed Android app through its public UI using adb."""
from pathlib import Path
import os, subprocess, re, time, sys, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
adb = Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe'
device = os.environ.get('SPORTSWAVE_DEVICE', 'emulator-5556')
artifacts = root / 'artifacts'

def run(*args):
    return subprocess.run([str(adb), '-s', device, *args], capture_output=True, text=True, check=True).stdout

def nodes():
    for attempt in range(5):
        output=run('shell', 'uiautomator', 'dump', '/sdcard/sportswave-qa.xml')
        if 'dumped to' in output:
            run('pull', '/sdcard/sportswave-qa.xml', str(artifacts / 'qa-current.xml'))
            return list(ET.parse(artifacts / 'qa-current.xml').iter('node'))
        time.sleep(.5)
    raise AssertionError('Android UI hierarchy did not become ready')

def tap(value, desc=False):
    attribute = 'content-desc' if desc else 'text'
    matches=[]
    for attempt in range(4):
        matches = [n for n in nodes() if n.get(attribute) == value]
        if matches: break
        time.sleep(.5)
    if not matches:
        raise AssertionError(f'Control not found: {value}')
    bounds = [int(x) for x in re.findall(r'\d+', matches[0].get('bounds'))]
    run('shell', 'input', 'tap', str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))

def contains(value):
    return any(value in n.get('text','') for n in nodes())

def capture(name):
    run('shell', 'screencap', '-p', '/sdcard/sportswave-qa.png')
    run('pull', '/sdcard/sportswave-qa.png', str(artifacts / name))

def reveal(value):
    for attempt in range(8):
        if contains(value): return
        run('shell','input','swipe','540','1800','540','700','500')
    raise AssertionError(f'Content not found: {value}')

def dash_check():
    for attempt in range(4):
        if any(n.get('text')=='DASH playback test' for n in nodes()): break
        run('shell','input','swipe','540','1800','540','700','700')
    tap('DASH playback test')
    deadline=time.time()+45
    while time.time()<deadline:
        if contains('Playing inside SportsWave'): break
        time.sleep(1)
    else: raise AssertionError('DASH playback did not reach playing state')
    capture('player-dash.png')
    print('PASS: internal DASH playback', flush=True)
    run('shell','input','keyevent','4')

if '--current-dash' in sys.argv:
    dash_check()
    sys.exit(0)

if '--dash-only' in sys.argv:
    run('shell', 'am', 'force-stop', 'com.sportswave.app')
    run('shell', 'am', 'start', '-n', 'com.sportswave.app/.MainActivity')
    tap('Channels', desc=True)
    dash_check()
    sys.exit(0)

run('shell', 'am', 'force-stop', 'com.sportswave.app')
run('shell', 'am', 'start', '-n', 'com.sportswave.app/.MainActivity')
assert contains('Every sport.')
capture('discover.png')
tap('Schedule', desc=True)
assert contains('The schedule')
assert contains('PROVIDER FIXTURES')
reveal('Explore illustrative calendar')
tap('Explore illustrative calendar')
assert contains('DEMO SCHEDULE')
capture('schedule.png')
# Save the first visible event, then verify it appears on the Saved page.
hearts = [n.get('content-desc') for n in nodes() if n.get('content-desc','').startswith('Save ')]
assert hearts
event = hearts[0][5:]
tap(hearts[0], desc=True)
tap('Saved', desc=True)
if not contains(event):
    # The same test may run with an already-saved event: toggle it back on.
    tap('Schedule', desc=True)
    tap('Save '+event, desc=True)
    tap('Saved', desc=True)
assert contains(event)
capture('saved.png')
print('PASS: discovery, schedule, and persistent favorites', flush=True)
tap('Channels', desc=True)
reveal('HLS playback test')
assert contains('HLS playback test')
capture('channels.png')
tap('HLS playback test')
deadline = time.time()+45
while time.time()<deadline:
    current = nodes()
    if any(n.get('text') == 'Playing inside SportsWave' for n in current):
        break
    if any('Playback unavailable' in n.get('text','') for n in current):
        raise AssertionError('The HLS source failed to play')
    time.sleep(1)
else:
    raise AssertionError('Playback did not reach playing state')
capture('player.png')
tap('Toggle fullscreen', desc=True)
for attempt in range(5):
    tree=nodes()
    bounds=[int(x) for x in re.findall(r'\d+',tree[0].get('bounds',''))]
    # The XML hierarchy node has no bounds; find its first window child.
    if not bounds:
        bounds=[int(x) for x in re.findall(r'\d+',tree[1].get('bounds',''))] if len(tree)>1 else []
    if len(bounds)==4 and bounds[2]-bounds[0]>bounds[3]-bounds[1]: break
    time.sleep(.5)
else: raise AssertionError('Fullscreen did not rotate to landscape')
capture('player-fullscreen.png')
activity = run('shell','dumpsys','activity','activities')
assert 'com.sportswave.app/.PlayerActivity' in activity or 'com.sportswave.app/com.sportswave.app.PlayerActivity' in activity
print('PASS: internal HLS playback and fullscreen', flush=True)
run('shell','input','keyevent','4')
assert contains('Your channels')
dash_check()
