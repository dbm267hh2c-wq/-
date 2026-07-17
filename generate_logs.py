#!/usr/bin/env python3
"""Generate service_info logs for 2026-03-06 to 2026-03-12 based on sample format."""

import random
from datetime import datetime, timedelta
from pathlib import Path

OUTPUT_DIR = Path("/workspace/generated-logs")

FEIGN_RESPONSES = [
    {
        "user_id": "UJa8867b9cb7ba48b4bc41b6faa9cd7a",
        "full_name": "ZKFUWU",
        "login_name": "ZKFUWU",
        "created_time": "2025-11-14 15:49:58",
    },
    {
        "user_id": "UJb25ada83a29b4cdebd6480fb69b96f",
        "full_name": "zhongkuang",
        "login_name": "zhongkuang",
        "created_time": "2025-09-06 20:58:18",
    },
]

RESULT_ASPECT_TEMPLATE = {
    "code": 200,
    "data": [
        {
            "accessCode": "191133100MABRLCFR5Q1100283418450",
            "applyId": "TJ062a556919c3498eaa879120165648",
            "authorStatus": 0,
            "catalogId": "TJ0199d1de4e3d4704b13900e71719db",
            "configStatus": 1,
            "createdTime": "2025-11-23T10:22:31.854444000",
            "creatorId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "dataField": "全部",
            "dataId": "TJa1279bbe3b6c46de9a25f3b075abe0",
            "dataProvide": 1,
            "dataResourceName": "测试业务演示",
            "endTime": "2025-12-18T08:00:00",
            "fieldValues": [
                {"dataType": "VARCHAR", "fieldComment": "ID", "fieldName": "id", "isNull": 0, "isPk": 1},
                {"dataType": "VARCHAR", "fieldComment": "张三", "fieldName": "name", "isNull": 1, "isPk": 0},
                {"dataType": "VARCHAR", "fieldComment": "李四", "fieldName": "age", "isNull": 1, "isPk": 0},
            ],
            "id": "TJ062a556919c3498eaa879120165648_TJa1279bbe3b6c46de9a25f3b075abe0",
            "params": "2025-11-01 00:00:00至2025-12-18 00:00:00",
            "policies": [],
            "projectId": "TJ0764bc18978f4ebcb5aef6010327a5",
            "projectName": "测试项目",
            "recycle": 0,
            "registerTenantId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "resourceState": 1,
            "stageStatus": 5,
            "startTime": "2025-11-01T08:00:00",
            "supplyValue": {"dataProvideMode": "DATABASE", "dataValue": 1, "dataValueUnit": "KB"},
            "tenantId": "UJa8867b9cb7ba48b4bc41b6faa9cd7a",
            "updateId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "updateTime": "2025-11-23T10:23:02.441209000",
            "useSpecificat": "访问时间控制",
        },
        {
            "accessCode": "191133100MABRLCFR5Q1100283418450",
            "applyId": "TJ084f4277336a4008b3a57ee9819e36",
            "authorStatus": 0,
            "catalogId": "TJc9743f6c8a004f5cbc4667de07522c",
            "configStatus": 1,
            "createdTime": "2025-11-23T09:59:52.420574000",
            "creatorId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "dataField": "全部",
            "dataId": "TJ86f330c967df4b379510d0126d9594",
            "dataProvide": 1,
            "dataResourceName": "XX市科技公司job信息数据集",
            "endTime": "2025-12-09T08:00:00",
            "fieldValues": [
                {"dataType": "VARCHAR", "fieldComment": "ID", "fieldName": "ID", "isNull": 0, "isPk": 1},
                {"dataType": "VARCHAR", "fieldComment": "ALARM_RULE_ID", "fieldName": "ALARM_RULE_ID", "isNull": 1, "isPk": 0},
                {"dataType": "VARCHAR", "fieldComment": "TABLE_NAME", "fieldName": "TABLE_NAME", "isNull": 1, "isPk": 0},
            ],
            "id": "TJ084f4277336a4008b3a57ee9819e36_TJ86f330c967df4b379510d0126d9594",
            "params": "2025-11-08 00:00:00至2025-12-09 00:00:00",
            "policies": [],
            "projectId": "TJ0764bc18978f4ebcb5aef6010327a5",
            "projectName": "测试项目",
            "recycle": 0,
            "registerTenantId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "resourceState": 1,
            "stageStatus": 5,
            "startTime": "2025-11-08T08:00:00",
            "supplyValue": {"dataProvideMode": "DATABASE", "dataValue": 20, "dataValueUnit": "MB"},
            "tenantId": "UJa8867b9cb7ba48b4bc41b6faa9cd7a",
            "updateId": "UJb25ada83a29b4cdebd6480fb69b96f",
            "updateTime": "2025-11-23T10:03:18.242400000",
            "useSpecificat": "访问时间控制",
        },
    ],
    "message": "操作成功",
    "status": "success",
}

REQUEST_TOKEN = (
    "snjshw0Ru/1MQuJ3WndcrcZZK3QdBlCEh4gjzfIx2pPFOuFW9uSMOYcNkFJT2iBs802NVTWeWwNXEjdB30l6hAfoSeiTc2dXZf5VnkQ9F1Xm0otLwlbcN0CRE6K3LlTyskK8hvBoaH79Z2IdXYxD5T7sqW1RZpPPMoBWEkiwGbYHjAfOHTbeRM25i3+SAqoIJL4bhGDUAd5CjXfvvRhwWTSIABckfCi8WYDAR8CkrlgZFbALO2RWA1xAnKxWXBxWhcrpNM4wQ90uTYwBOAYhwIpVUsmgaTZJKOIhEeYsCAZHLzIl60m1fAWrljpx11+poFHOQyeb+/8aeCEmbLfyLg=="
    "::Iw0F6idW+f6KNck7LMgjUPv0nxE+wTrVYwIhfW0hqZnniqMwwFrcd0GiTEAn+Y/w"
)
ACCESS_CODE = "191133100MABRLCFR5Q1100283418450"


def json_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"')


def feign_user_response(user: dict) -> str:
    return (
        '{\\"message\\":\\"操作成功\\",\\"code\\":200,\\"data\\":{'
        f'\\"creatorId\\":\\"{user["user_id"]}\\",'
        f'\\"createdTime\\":\\"{user["created_time"]}\\",'
        '\\"updateId\\":null,'
        f'\\"updateTime\\":\\"{user["created_time"]}\\",'
        '\\"recycle\\":0,'
        f'\\"id\\":\\"{user["user_id"]}\\",'
        f'\\"fullName\\":\\"{user["full_name"]}\\",'
        '\\"englishName\\":null,'
        f'\\"loginName\\":\\"{user["login_name"]}\\",'
        '\\"email\\":null,\\"sex\\":0,\\"telephone\\":null,\\"cellphone\\":null,'
        '\\"telephoneHome\\":null,\\"certType\\":null,\\"certNo\\":null,'
        '\\"postCode\\":null,\\"officeAddress\\":null,\\"homeAddress\\":null,'
        '\\"fax\\":null,\\"birthDate\\":null,\\"userNum\\":null,'
        '\\"occupation\\":null,\\"workDesc\\":null,\\"active\\":1,'
        '\\"avatar\\":null,\\"remark\\":null,\\"orderNo\\":1'
        '},\\"status\\":\\"success\\"}'
    )


def build_feign_line(ts: datetime, trace_id: str, thread: str, user: dict) -> str:
    payload = (
        f'{{"traceId":"{trace_id}","method":"GET",'
        f'"response":"{feign_user_response(user)}",'
        f'"message":"feign拦截器",'
        f'"url":"http://hetu/ds/system/user/info?id={user["user_id"]}"}}'
    )
    return (
        f"{ts.strftime('%Y-%m-%d %H:%M:%S')}.{ts.microsecond // 1000:03d} "
        f"{trace_id} [{thread}] INFO  "
        f"c.c.t.d.service.entry.aspect.FeignLogDecoder.decode [43] - {payload}"
    )


def build_result_data_json(trace_id: str) -> str:
    import json

    body = dict(RESULT_ASPECT_TEMPLATE)
    body["requestId"] = trace_id
    return json.dumps(body, ensure_ascii=False, separators=(",", ":"))


def build_result_line(ts: datetime, trace_id: str, thread: str, elapsed_ms: int) -> str:
    response_json = build_result_data_json(trace_id)
    payload = (
        f'{{"traceId":"{trace_id}","request":["{REQUEST_TOKEN}","{ACCESS_CODE}"],'
        f'"method":"findByProjectIdAndDataProvides",'
        f'"response":{response_json},'
        f'"time":{elapsed_ms},'
        f'"message":"统一请求拦截",'
        f'"url":"/ds/api/data-resource/findByProjectIdAndDataProvide"}}'
    )
    return (
        f"{ts.strftime('%Y-%m-%d %H:%M:%S')}.{ts.microsecond // 1000:03d} "
        f"{trace_id} [{thread}] INFO  "
        f"c.c.t.datasphere.service.entry.aspect.ResultAspect.around [147] - {payload}"
    )


def gen_trace_id(base: int, offset: int) -> str:
    return str(base + offset)


def generate_day_logs(day: datetime, day_index: int) -> list[str]:
    random.seed(day.strftime("%Y%m%d"))
    lines: list[str] = []
    base_trace = 1780300000000000000 + day_index * 10_000_000_000

    # Simulate periodic requests throughout the day
    slots = []
    for hour in range(24):
        count = 2 if 0 <= hour <= 6 else random.randint(3, 8)
        for _ in range(count):
            minute = random.randint(0, 59)
            second = random.randint(0, 59)
            ms = random.randint(0, 999)
            slots.append((hour, minute, second, ms))

    slots.sort()

    trace_offset = 0
    for hour, minute, second, ms in slots:
        ts = day.replace(hour=hour, minute=minute, second=second, microsecond=ms * 1000)
        thread1 = f"http-nio-8080-exec-{random.randint(1, 20)}"
        thread2 = f"http-nio-8080-exec-{random.randint(1, 20)}"

        tid1 = gen_trace_id(base_trace, trace_offset)
        trace_offset += random.randint(100_000_000, 500_000_000)
        tid2 = gen_trace_id(base_trace, trace_offset)
        trace_offset += random.randint(100_000_000, 500_000_000)
        tid3 = gen_trace_id(base_trace, trace_offset)
        trace_offset += random.randint(100_000_000, 500_000_000)

        user1 = FEIGN_RESPONSES[0]
        user2 = FEIGN_RESPONSES[1]

        lines.append(build_feign_line(ts, tid1, thread1, user1))
        ts2 = ts + timedelta(milliseconds=random.randint(30, 120))
        lines.append(build_feign_line(ts2, tid2, thread2, user2))
        ts3 = ts2 + timedelta(milliseconds=random.randint(80, 250))
        elapsed = random.randint(35, 120)
        lines.append(build_result_line(ts3, tid3, thread2, elapsed))

    return lines


def main() -> None:
    start = datetime(2026, 3, 6)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for i in range(7):
        day = start + timedelta(days=i)
        lines = generate_day_logs(day, i)
        filename = OUTPUT_DIR / f"service_info-{day.strftime('%Y-%m-%d')}.0.log"
        filename.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"Generated {filename.name}: {len(lines)} lines")


if __name__ == "__main__":
    main()
