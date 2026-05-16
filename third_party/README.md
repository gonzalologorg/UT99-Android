This folder is intentionally empty in the ZIP.

Run from the project root on Windows:

```powershell
.\tools\fetch_deps.ps1
.\tools\apply_overrides.ps1
```

That will create:

- `third_party/ut99dc` from https://github.com/maximqaxd/ut99dc
- `third_party/SDL2` from the official SDL 2.28.5 release

The source and SDL are not vendored here to keep the package small and avoid accidentally freezing the wrong upstream snapshot.
