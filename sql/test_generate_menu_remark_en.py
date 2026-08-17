#!/usr/bin/env python3
"""Validate generated English remark UPDATE statements."""

import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import generate_menu_remark_en as gen  # noqa: E402


def test_build_remark_empty_and_merge():
    assert gen.build_remark(None, "Data Management") == '{"en":"Data Management"}'
    assert gen.build_remark("", "Data Management") == '{"en":"Data Management"}'
    assert gen.build_remark("/business/back/onlineService", "Online Customer Service") == (
        '{"en":"Online Customer Service"}'
    )
    assert gen.build_remark('{"group":1}', "Business Process") == (
        '{"group":1,"en":"Business Process"}'
    )
    assert gen.build_remark('{"group": 0}', "Work Log") == '{"group":0,"en":"Work Log"}'
    assert gen.build_remark('{"en":"Old"}', "Data Management") == '{"en":"Data Management"}'


def test_existing_sample_translations():
    samples = {
        "数据管理": "Data Management",
        "项目管理": "Project Management",
        "工作台": "Dashboard",
        "数据源": "Data Source",
        "产品管理": "Product Management",
        "数据申请": "Data Request",
        "菜单管理": "Menu Management",
    }
    for cn, en in samples.items():
        assert gen.TRANSLATIONS[cn] == en, (cn, gen.TRANSLATIONS[cn], en)


def test_generated_sql():
    subprocess.check_call([sys.executable, str(HERE / "generate_menu_remark_en.py")])
    sql = (HERE / "update_tdp_sys_menu_remark_en.sql").read_text(encoding="utf-8")
    updates = re.findall(
        r"UPDATE `TDP_SYS_MENU` SET `REMARK` = '(.*?)' WHERE `ID` = '(.*?)';",
        sql,
    )
    rows = gen.load_rows()
    assert len(updates) == len(rows) == 550
    assert {row_id for _, row_id in updates} == {row["id"] for row in rows}

    by_id = {row["id"]: row for row in rows}
    for remark, row_id in updates:
        remark = remark.replace("''", "'")
        obj = json.loads(remark)
        assert obj["en"]
        row = by_id[row_id]
        assert obj["en"] == gen.TRANSLATIONS[row["name"]]
        if row["remark"] not in (None, ""):
            try:
                old = json.loads(row["remark"])
            except json.JSONDecodeError:
                continue
            if isinstance(old, dict):
                for key, value in old.items():
                    if key != "en":
                        assert obj[key] == value


if __name__ == "__main__":
    test_build_remark_empty_and_merge()
    test_existing_sample_translations()
    test_generated_sql()
    print("ok")
