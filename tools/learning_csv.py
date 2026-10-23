#!/usr/bin/env python3
"""
Уроки «Сахалыы саҥар»: таблица ⇄ файл приложения.

  python3 tools/learning_csv.py to-csv    — выгрузить уроки из приложения в content/learning.csv
  python3 tools/learning_csv.py to-json   — собрать файл приложения из content/learning.csv

Одна строка таблицы — одна карточка. Тема задаётся первыми колонками; строки одной темы идут подряд.
"""
import csv, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JSON_PATH = ROOT / "app/src/main/assets/demo/learning.json"
CSV_PATH = ROOT / "content/learning.csv"
LANGS = ["ru", "en"]
COLUMNS = (["тема_id"] + [f"тема_{l}" for l in LANGS] + [f"подзаголовок_{l}" for l in LANGS]
           + ["якутский"] + [f"перевод_{l}" for l in LANGS] + [f"подсказка_{l}" for l in LANGS] + ["аудио"])


def open_table(path):
    """Читает таблицу CSV: понимает и запятую, и точку с запятой (так сохраняет русский Excel)."""
    try:
        text = path.read_text(encoding="utf-8-sig")
    except UnicodeDecodeError:
        sys.exit(f"Ошибка: {path.name} сохранён не в UTF-8. В Excel выберите «CSV UTF-8», в Numbers — «Юникод (UTF-8)».")
    header = text.splitlines()[0] if text else ""
    delimiter = ";" if header.count(";") > header.count(",") else ","
    return csv.DictReader(text.splitlines(), delimiter=delimiter)


def to_csv():
    data = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    with CSV_PATH.open("w", encoding="utf-8-sig", newline="") as f:  # utf-8-sig — чтобы Excel понял якутские буквы
        w = csv.writer(f)
        w.writerow(COLUMNS)
        for s in data["sections"]:
            for item in s["items"]:
                w.writerow([s["id"]] + [s.get("title", {}).get(l, "") for l in LANGS]
                           + [s.get("subtitle", {}).get(l, "") for l in LANGS]
                           + [item["sah"]] + [item.get("tr", {}).get(l, "") for l in LANGS]
                           + [item.get("hint", {}).get(l, "") for l in LANGS] + [item.get("audio") or ""])
    print(f"Готово: {CSV_PATH.relative_to(ROOT)}")


def to_json():
    old = json.loads(JSON_PATH.read_text(encoding="utf-8")) if JSON_PATH.exists() else {}
    sections, by_id = [], {}
    for n, row in enumerate(open_table(CSV_PATH), start=2):
        row = {k: (v or "").strip() for k, v in row.items() if k}
        if not row.get("якутский"):
            continue
        sid = row.get("тема_id") or f"section{len(sections) + 1}"
        if sid not in by_id:
            s = {"id": sid,
                 "title": {l: row[f"тема_{l}"] for l in LANGS if row.get(f"тема_{l}")},
                 "items": []}
            sub = {l: row[f"подзаголовок_{l}"] for l in LANGS if row.get(f"подзаголовок_{l}")}
            if sub:
                s["subtitle"] = sub
            by_id[sid] = s
            sections.append(s)
        item = {"sah": row["якутский"],
                "tr": {l: row[f"перевод_{l}"] for l in LANGS if row.get(f"перевод_{l}")}}
        hint = {l: row[f"подсказка_{l}"] for l in LANGS if row.get(f"подсказка_{l}")}
        if hint:
            item["hint"] = hint
        if row.get("аудио"):
            item["audio"] = row["аудио"]
        if not item["tr"]:
            print(f"Внимание: строка {n} «{item['sah']}» — нет перевода")
        by_id[sid]["items"].append(item)
    out = {"version": 2, "is_demo": old.get("is_demo", True), "sections": sections}
    JSON_PATH.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Готово: {len(sections)} тем, {sum(len(s['items']) for s in sections)} карточек → {JSON_PATH.relative_to(ROOT)}")


if __name__ == "__main__":
    {"to-csv": to_csv, "to-json": to_json}.get(sys.argv[1] if len(sys.argv) > 1 else "", lambda: print(__doc__))()
