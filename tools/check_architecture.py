#!/usr/bin/env python3
"""Check Foldball import boundaries. Run from any directory; no dependencies needed."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
source = root / "shared/src/commonMain/kotlin/com/yogi/foldball"
violations = []
checked = 0
for path in sorted(source.rglob("*.kt")):
    relative = path.relative_to(source).as_posix()
    checked += 1
    for line_number, line in enumerate(path.read_text().splitlines(), 1):
        match = re.match(r"import ([\w.]+)", line)
        if not match:
            continue
        imported = match.group(1)
        reason = None
        if relative.startswith("domain/") and imported.startswith((
            "android.", "androidx.", "platform.", "java.", "org.koin.",
            "com.yogi.foldball.feature.", "com.yogi.foldball.data.", "com.yogi.foldball.core.",
        )):
            reason = "Domain must remain pure Kotlin and independent of presentation/data/platform/DI"
        elif relative.startswith("data/") and imported.startswith((
            "androidx.compose.", "com.yogi.foldball.feature.", "com.yogi.foldball.gamekit.",
        )):
            reason = "Data must not import UI"
        elif relative.startswith("gamekit/") and imported.startswith((
            "com.yogi.foldball.feature.", "com.yogi.foldball.data.",
        )):
            reason = "Reusable GameKit rendering must not depend on feature/data"
        elif relative.startswith("feature/") and "/viewmodel/" not in relative and imported.startswith((
            "com.yogi.foldball.data.", "com.yogi.foldball.domain.repository.",
        )):
            reason = "Screens must access persistence through their ViewModel"
        if reason:
            violations.append(f"{relative}:{line_number}: {reason}: {imported}")
if violations:
    print("\n".join(violations), file=sys.stderr)
    sys.exit(1)
print(f"Architecture boundaries passed ({checked} Kotlin source files).")
