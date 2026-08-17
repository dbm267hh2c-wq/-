#!/usr/bin/env python3
"""Generate UPDATE statements that write English dictionary names into REMARK."""

import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCE = HERE / "tdp_sys_dictionary_id_name_remark.json"
MAPPING = HERE / "dict_name_en_mapping.json"
OUTPUT = HERE / "update_tdp_sys_dictionary_remark_en.sql"
REMARK_MAX_BYTES = 255


def sql_escape(value):
    return value.replace("\\", "\\\\").replace("'", "''")


def build_remark(existing, english):
    if existing not in (None, ""):
        try:
            obj = json.loads(existing)
            if isinstance(obj, dict):
                obj["en"] = english
                return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
        except (json.JSONDecodeError, TypeError):
            pass
    return json.dumps({"en": english}, ensure_ascii=False, separators=(",", ":"))


def load_rows():
    payload = json.loads(SOURCE.read_text(encoding="utf-8"))
    return [{"id": row["id"], "name": row["name"], "remark": row.get("remark")} for row in payload]


def load_translations():
    return json.loads(MAPPING.read_text(encoding="utf-8"))


def main():
    rows = load_rows()
    translations = load_translations()
    missing = sorted({row["name"] for row in rows if row["name"] not in translations})
    if missing:
        raise SystemExit("Missing translations:\n" + "\n".join(missing))

    lines = [
        "-- Update TDP_SYS_DICTIONARY.REMARK with English labels from DICT_NAME",
        "-- Format: {\"en\":\"English Name\"}",
        "-- Existing JSON remarks keep original keys and add en",
        "SET NAMES utf8mb4;",
        "",
    ]
    for row in rows:
        english = translations[row["name"]]
        remark = build_remark(row["remark"], english)
        size = len(remark.encode("utf-8"))
        if size > REMARK_MAX_BYTES:
            raise SystemExit(f"REMARK exceeds {REMARK_MAX_BYTES} bytes for {row['id']}: {remark}")
        lines.append(
            f"UPDATE `TDP_SYS_DICTIONARY` SET `REMARK` = '{sql_escape(remark)}' WHERE `ID` = '{sql_escape(row['id'])}';"
        )
    lines.append("")
    OUTPUT.write_text("\n".join(lines), encoding="utf-8")

    mapping_tsv = HERE / "dict_name_en_mapping.tsv"
    items = sorted(translations.items(), key=lambda item: item[0])
    mapping_tsv.write_text(
        "dict_name\ten\n" + "".join(f"{name}\t{english}\n" for name, english in items),
        encoding="utf-8",
    )
    print(f"wrote {len(rows)} updates to {OUTPUT}")


if __name__ == "__main__":
    main()
