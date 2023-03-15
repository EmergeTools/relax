# Relax Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- `clickable` selector.
- `getObject` function works like `findObject` used to work (throws if the view represented by the UiObject doesn't exist).
- `findObject` now returns a `UiObject` even if the view represented by the UiObject doesn't exist.