**English** · [Español](README.es.md)

<div align="center">

![HoneyDelight — Your Samsung Keyboard, with a touch of Delight.](assets/banner-en.svg)

**Looks like Samsung. Predicts with Delight.**

![Status: experimental alpha](https://img.shields.io/badge/status-experimental_alpha-e9b45d?style=flat-square)
![Android tested: 16](https://img.shields.io/badge/Android_tested-16-b9e7dd?style=flat-square)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)
![Root required](https://img.shields.io/badge/root-required-d5dae0?style=flat-square)

[Features](#features) · [Compatibility](#compatibility) · [Testing](#testing) · [Roadmap](#roadmap)

</div>

## Why it exists

I like Samsung Keyboard: its design, gestures and the way it fits into the rest of the phone. What kept bothering me was the typing underneath: suggestions that missed the context, corrections that arrived late or never arrived, and joined words I had to separate by hand.

HoneyDelight grew out of that. It connects **Delight5, Gboard's native prediction engine**, to the Samsung Keyboard interface, keeping the keyboard I enjoy using while drawing on a different prediction engine. The bridge adds its own rules, so its behavior still differs from the Gboard app. Along the way, I also expanded the Galaxy AI writing tools and worked through the rough edges of bringing the pieces together: joined words, repeated suggestions, fields that would not correct text, and waits that slowed down the rewrite editor.

It is an alpha that I use on my own phone. Testing and everyday use guide the next changes. When an adjustment makes suggestions worse, I return to the previous revision and keep the example as a regression case.

### Why “HoneyDelight”?

**Honey** comes from **HoneyBoard**, Samsung Keyboard's internal name. **Delight** comes from the prediction engine. The name brings together the two pieces I want to work well together.

## Features

| Area | What HoneyDelight adds |
| :--- | :--- |
| **Prediction** | Delight5 suggestions that consider the preceding text, with correction rules and the keyboard's actual geometry. |
| **Joined words** | Combines Delight's word splitting with a local table of 16 common expressions. Filters repetitions and joined alternatives when it accepts a split. |
| **Touch and gestures** | Passes actual touch coordinates to the engine and uses Delight5's gesture decoder for swipe input. |
| **Learning** | Uses available local history, with filters that reduce learning from typos and pasted text. Does not import the developer's learned data. |
| **Difficult fields** | Restores correction in supported Google fields where it was disabled, while preserving browser address-bar behavior. |
| **Galaxy AI** | Additional styles and an instruction field inside Edit, with improvements to confirming the rewritten result. |
| **Responsiveness** | Addresses reproduced waits between the editor and prediction threads; user testing confirmed less lag. |
| **Startup** | Waits for the current profile's directory before starting Delight and retries temporary failures instead of disabling it for the whole process. |

Verified **in the suggestion bar and when pressing space in Google**, on November 4, 2026:

| Input | Suggestion and committed text |
| :--- | :--- |
| `buenosdias` | buenos días |
| `buenastardes` | buenas tardes |
| `buenasnoches` | buenas noches |
| `hastamanana` | hasta mañana |
| `tequiero` | te quiero |
| `porfavor` | por favor |

The 16 local expressions are resolved without using the engine's optional search budget. Other expressions still use the general splitter, which needs more work. `gustamucho → gusta mucho` also passed the Google check; the latest revision adds conservative one-letter correction in the right-hand part of a split and prevents composition duplication after backspace.

Touch coordinates already reach the engine. Personalized calibration exists, but remains disabled until it demonstrates better results.

## Compatibility

> **This first alpha is for experienced root users who can prepare their setup and recover if something goes wrong. It is not yet a general-purpose installer.**

| Component | Tested environment |
| :--- | :--- |
| Device | Samsung Galaxy S25+ · SM-S936B |
| System | Android 16 · One UI 8.5 |
| Samsung Keyboard | 5.9.30.97 |
| Gboard JNI interface | 18.5.6 |
| Architecture | arm64-v8a |
| Development root environment | KernelSU 3.3.0; module activation still unvalidated |
| ZIP firmware build | `S936BXXSBCZG3_OWOBCZG3` |
| Compatibility ID | `1a9a24823d75` |

The module keeps Samsung's original signed APK intact. During development, compiled code and libraries are mounted over the original paths reversibly. The artifacts must match the exact APK and ART environment on the device. Matching the version number alone is not enough.

[Theoretical Android 13–16 compatibility, operation without AI and a proposed system-mounted keyboard variant →](docs/COMPATIBILITY_RESEARCH.md) *(Spanish)*

That research separates porting candidates from current support: the released ZIP still requires its exact target environment. Core and System Base editions remain proposals. Four real APKs have now been compared and host regressions run on the Mac: [portability test results](docs/COMPATIBILITY_TESTS.md) *(Spanish)*.

### Requirements and what the module installs

The recommended environment for **testing this alpha** is exactly the one in the table. No other Android, One UI or Samsung Keyboard version has been validated. The ZIP checks the full firmware fingerprint and APK hash; sharing a model or version number is not enough. The packaging supports a Magisk installation route, but no Magisk version has been validated in this project.

**The module includes the complete mod: bridge, patches, Delight5 library and Spanish/English base models. You do not need to install Gboard, import its data or download those models separately.** It requires root and an existing compatible Samsung Keyboard installation. It does not install Samsung Keyboard from scratch or add Galaxy AI to unsupported devices. AI features still depend on Samsung's services and requirements.

Download the matching ZIP from [Releases](https://github.com/SuperProgramer64/HoneyDelight/releases), install through the root manager and keep another keyboard available for recovery. An OTA or keyboard update may require a new package; do not force a ZIP that the installer rejects. The guide explains the installation methods and validation status.

### Main installation method: the module ZIP

The main package is a **root module** in KernelSU/Magisk format. The companion app is optional. Module-manager activation and this alpha's first boot still need validation. Each ZIP targets a specific firmware build and checks the APK, Android version and file integrity before installation.

For release notifications, **HoneyDelight Updates** is an optional companion. It can notify you and download a compatible ZIP over Wi-Fi or other unmetered networks. It verifies SHA-256, saves it to `Downloads/HoneyDelight` and leaves installation to you. It does not request root or access learned history or models.

| Method | How it works |
| :--- | :--- |
| **KernelSU / Magisk** | Download the ZIP and select “Install from storage” in the root manager. |
| **HoneyDelight Updates** | Checks releases and downloads a matching ZIP, which you open with the root manager. Optional. |
| **Termux with root** | Invoke the root manager's module installer through `su`. |
| **ADB with root** | Copy the ZIP to the phone and invoke the same installer through ADB. |

The Updates APK lives in `/data/app` and can be updated like other apps with the same signing certificate. With the module, the keyboard remains Samsung's original signed APK.

> **Current status · November 4, 2026:** the keyboard revision is installed through development mounts. The ZIP passed compatibility, checksum and local ART-preparation checks on the phone; module activation, first boot and recovery are still pending. Updates builds and installs; an end-to-end download and notifications from a public release still need testing.

The repository is **public**, with an experimental alpha. The updater only checks public releases and contains no GitHub credentials.

### Three packages, three roles

| File | Purpose | Status |
| :--- | :--- | :--- |
| **Module ZIP** | Apply HoneyDelight while retaining Samsung's signed APK. | Main package; preparation and integrity verified. Module-manager activation and first boot remain unvalidated. |
| **HoneyDelight Updates APK** | Find and download compatible modules, with the refreshed interface, adaptive icons and notifications. | Optional app, without root access or learned models. `0.1.0-alpha.2`, signed with the same certificate as earlier test versions. |
| **Experimental Keyboard APK** | Complete APK containing modified code, the native library and base models. | Signed with a project key; cannot directly update Samsung's official keyboard. Installation and Samsung permissions/services remain unvalidated. |

[What the complete APK contains and why it is experimental →](docs/FULL_APK.md) *(Spanish)*

[Installation commands and recovery →](docs/INSTALLATION.md) *(Spanish)*

## Testing

The current revision has three levels of testing:

| Check | Result and scope |
| :--- | :--- |
| **JVM regressions** | 19 groups passed, covering profile startup, privacy, editor behavior, candidates, stale responses and splitting, among other cases. |
| **Full prediction path with real Delight** | Latest repeat: **90/92**; an earlier run returned 92/92. These are 23 inputs, two rounds and two coordinate variants. `holaxomo` and `holacomo` each failed once with coordinates and a 55 ms budget; splitting remains intermittent in that path. |
| **Suggestion bar and space in Google** | All six expressions above appeared separated in the bar and were committed with spaces. |
| **Additional Google checks** | `gustamucho → gusta mucho`, `programacion → programación` and `cocodrilo → cocodrilo` passed. `holacomo` had previously become `holacómo`. The later revision improved those cases; separated suggestions, space commits and backspace were verified in Google for both `holacomo` and `holaxomo`. |
| **Broader decision comparison** | 82/84 expected decisions. Both failures were `erroes → errors` with simulated coordinates; Spanish/English ranking still needs work. |
| **Packaging and updater** | Checks passed for personal-data exclusion, compatibility, patching, CRC and download selection/verification. |

These figures describe specific regressions, not an overall accuracy percentage or a general comparison with Gboard. Native tests use base dictionaries; test containers and learned data are not packaged in the keyboard.

The editor change was also informed by traces: they showed a Samsung circular wait of roughly 500 ms before the fix. The user then confirmed less lag in both fields. No exact final latency is claimed without a measurement.

A separate touch evaluation held out 100 words: nominal key centers, actual coordinates and personalized centers each scored 80 correct results. Personalized calibration stays disabled because it has not shown an advantage.

### Known limitations

- General splitting still has intermittent failures within the short 55 ms budget; the latest native repeat returned 90/92. The `holaxomo` revision also checks space and backspace; the local expression table does not solve every possible combination.
- Mixed Spanish/English input can select a correction from the other language.
- Effective loading in cloned-app profiles still needs in-use verification, although the profile-startup regression passed.
- Samsung places a single remaining suggestion on the left; that alignment has not been changed.
- Module installation, first boot and recovery still need complete testing.

[Detailed status and known limitations →](docs/STATUS.md) *(Spanish)*

## Build and test

You need **JDK 17**, Android SDK Platform 34 and Build Tools 34.0.0.

```sh
export ANDROID_HOME=/path/to/android-sdk
bash port/tests/run.sh
python3 scripts/tests/test_hooks.py
python3 scripts/tests/test_module.py
bash manager/tests/run.sh
python3 module-tools/tests/test_crc.py
```

The full prediction-path regression requires a selected device and verified base components:

```sh
export ANDROID_SERIAL=your-device-serial
export BASE_COMPONENTS_DIR=/path/to/local-base-components
bash scripts/test-native-predictions.sh
```

That script uses a temporary test directory. It does not install the keyboard, open apps or read the user's learned data.

To prepare a local copy of the compatible APK:

```sh
export APKTOOL_JAR=/path/to/apktool_2.12.1.jar
bash scripts/prepare-samsung.sh /path/to/base.apk
```

The script checks the hash before applying patches. It does not touch the installed APK or install anything on the phone.

[Build guide →](docs/BUILD.md) *(Spanish)*

## Roadmap

- [x] Module packager that checks the APK, Android build, architecture and integrity.
- [x] Optional app for release notifications and compatible downloads.
- [x] Optional detailed logging, disabled by default in release preparation.
- [x] Local handling of 16 common expressions and a full prediction-path native regression.
- [ ] Improve general splitting beyond the expression list.
- [ ] Confirm effective Delight startup in cloned apps.
- [ ] Verify module installation, activation and first boot end to end.
- [ ] Test deactivation and recovery across keyboard updates.
- [ ] Test startup and other compatible devices.
- [ ] Reduce unwanted corrections when mixing Spanish and English.
- [ ] Learn from corrections the user undoes.
- [ ] Enable personalized touch calibration only after a held-out evaluation shows an improvement.

**Correction from the text-selection menu** did not make this release: the prototype did not meet expectations in testing and was removed.

## Code, data and third-party components

This repository contains the project's own code: the bridge, JNI declarations and stubs, test harnesses, integration patches and documentation. **The source tree does not include Samsung or Gboard APKs, native libraries, models, history, contacts, conversations, keys or tokens from my phone.** The local module package includes the base components it needs, with pinned hashes, but none of the developer's learned keyboard data.

Detailed logging is disabled by default. If enabled, logs can contain personal text; review them before sharing. Galaxy AI features use Samsung's services and device settings, so some may require a network connection.

[Privacy and diagnostics →](docs/PRIVACY.md) *(Spanish)*

## Contributing

Found a problem? Describe the field, your steps, the expected behavior and what happened. Include the device model, Android/One UI version and Samsung Keyboard version. For prediction issues, an invented sentence that reproduces the problem is more useful than a real conversation and protects your privacy.

For a proposed change, run the regressions first and explain which case it improves. Please do not attach proprietary components or personal data.

---

Created by **[SuperProgramer64](https://github.com/SuperProgramer64)**, one test phrase at a time. HoneyDelight is an independent project, unaffiliated with Samsung, Google or Microsoft.

Original HoneyDelight code is distributed under the [MIT license](LICENSE), © 2026 SuperProgramer64. Scope and third-party exclusions are detailed in [Third-party notices](THIRD_PARTY_NOTICES.md) *(Spanish)*.
