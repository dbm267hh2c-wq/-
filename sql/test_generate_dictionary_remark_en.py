#!/usr/bin/env python3
"""Validate generated English remark UPDATE statements for TDP_SYS_DICTIONARY."""

import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import generate_dictionary_remark_en as gen  # noqa: E402


def test_build_remark_empty_and_plain_text():
    assert gen.build_remark(None, "Other") == '{"en":"Other"}'
    assert gen.build_remark("", "Other") == '{"en":"Other"}'
    assert gen.build_remark("钉钉", "DingTalk") == '{"en":"DingTalk"}'
    assert gen.build_remark('{"group":1}', "Other") == '{"group":1,"en":"Other"}'


def test_sample_translations():
    translations = gen.load_translations()
    samples = {
        "字典导航": "Dictionary Navigation",
        "数据资源": "Data Resource",
        "数据集": "Dataset",
        "面议": "Negotiable",
        "其他": "Other",
        "mysql": "MySQL",
        "client credentials模式": "Client Credentials Mode",
        "公共数据,个人隐私数据,企业数据": "Public Data, Personal Privacy Data, Enterprise Data",
    }
    for cn, en in samples.items():
        assert translations[cn] == en, (cn, translations[cn], en)


def test_generated_sql():
    subprocess.check_call([sys.executable, str(HERE / "generate_dictionary_remark_en.py")])
    sql = (HERE / "update_tdp_sys_dictionary_remark_en.sql").read_text(encoding="utf-8")
    updates = re.findall(
        r"UPDATE `TDP_SYS_DICTIONARY` SET `REMARK` = '(.*?)' WHERE `ID` = '(.*?)';",
        sql,
    )
    rows = gen.load_rows()
    translations = gen.load_translations()
    assert len(updates) == len(rows) == 432
    assert {row_id for _, row_id in updates} == {row["id"] for row in rows}

    by_id = {row["id"]: row for row in rows}
    for remark, row_id in updates:
        remark = remark.replace("''", "'")
        assert len(remark.encode("utf-8")) <= gen.REMARK_MAX_BYTES
        obj = json.loads(remark)
        assert obj["en"]
        row = by_id[row_id]
        assert obj["en"] == translations[row["name"]]


if __name__ == "__main__":
    test_build_remark_empty_and_plain_text()
    test_sample_translations()
    test_generated_sql()
    print("ok")
