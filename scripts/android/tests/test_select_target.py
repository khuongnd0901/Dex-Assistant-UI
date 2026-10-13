"""Selector regressions: simulator-only must not query a connected phone."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'select-test-target.sh'
FAKE = '''#!/usr/bin/env python3
import os, sys
with open(os.environ['CALLS'], 'a') as f: f.write(' '.join(sys.argv[1:]) + '\\n')
a = sys.argv[1:]
if a[:1] == ['-P']: a = a[2:]
if a == ['devices', '-l']:
 print('List of devices attached\\nphone:123 device\\n127.0.0.1:5555 device')
elif a[:1] == ['-s']:
 command = a[2:]
 if command == ['shell', 'getprop', 'ro.product.model']: print('SM-F926B')
 elif command == ['shell', 'getprop', 'ro.build.version.sdk']: print('35')
 elif command == ['shell', 'getprop', 'ro.boot.qemu.avd_name']: print(os.environ['AVD_NAME'])
'''

class SelectTargetTest(unittest.TestCase):
    def run_case(self, *args, avd='DeX_Command_Test_API35'):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            adb = root / 'adb.exe'
            adb.write_text(FAKE)
            adb.chmod(0o755)
            script = root / 'selector.sh'
            source = SCRIPT.read_text().replace('adb="$sdk_root/platform-tools/adb.exe"', f'adb="{adb}"')
            source = source.replace('linux_adb="/home/khuongnd/Android/Sdk/platform-tools/adb"', f'linux_adb="{adb}"')
            script.write_text(source)
            calls = root / 'calls'
            result = subprocess.run(['bash', str(script), *args], capture_output=True, text=True,
                                    env=dict(os.environ, CALLS=str(calls), AVD_NAME=avd), timeout=10)
            return result, calls.read_text() if calls.exists() else ''

    def test_emulator_only_with_connected_fold3(self):
        for profile, avd in [('dex', 'DeX_Command_Test_API35'), ('foldable', 'ZFold3_API35')]:
            with self.subTest(profile=profile):
                result, calls = self.run_case(profile, '--emulator-only', avd=avd)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn('TARGET=AVD ADB=windows PORT=5038', result.stdout)
                self.assertIn('SERIAL=127.0.0.1:5555', result.stdout)
                self.assertNotIn('-s phone:123', calls)
                self.assertNotIn('ro.product.model', calls)

    def test_default_keeps_physical_preference(self):
        result, calls = self.run_case('dex')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('TARGET=Fold3 ADB=linux', result.stdout)
        self.assertIn('-s phone:123 shell getprop ro.product.model', calls)

    def test_invalid_arguments_fail_before_adb(self):
        for args in [('dex', '--unknown'), ('wrong',), ('dex', '--emulator-only', 'extra')]:
            result, calls = self.run_case(*args)
            self.assertEqual(result.returncode, 2)
            self.assertEqual(calls, '')

if __name__ == '__main__': unittest.main()
