#!/usr/bin/env python3
"""
Якутский интерфейс: таблица ⇄ файл надписей.

  python3 tools/strings_csv.py to-csv   — обновить content/interface-sah.csv (новые надписи добавятся,
                                           уже вписанные якутские переводы сохранятся)
  python3 tools/strings_csv.py to-xml   — собрать res/values-sah/strings.xml из таблицы
                                           и включить якутский в списке языков приложения

Якутский включается, когда заполнены все строки (или с флагом --force: пустые останутся на русском).
"""
import csv, re, sys
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
CSV_PATH = ROOT / "content/interface-sah.csv"
COLUMNS = ["ключ", "English", "Русский", "Якутский"]


def open_table(path):
    """Читает таблицу CSV: понимает и запятую, и точку с запятой (так сохраняет русский Excel)."""
    try:
        text = path.read_text(encoding="utf-8-sig")
    except UnicodeDecodeError:
        sys.exit(f"Ошибка: {path.name} сохранён не в UTF-8. В Excel выберите «CSV UTF-8», в Numbers — «Юникод (UTF-8)».")
    header = text.splitlines()[0] if text else ""
    delimiter = ";" if header.count(";") > header.count(",") else ","
    return csv.DictReader(text.splitlines(), delimiter=delimiter)


def read(path):
    """name -> текст; у множественных форм берём форму «other» (в якутском она одна)."""
    out = {}
    for el in ET.parse(path).getroot():
        name = el.get("name")
        if el.tag == "string":
            out[name] = el.text or ""
        elif el.tag == "plurals":
            forms = {i.get("quantity"): i.text or "" for i in el}
            out[name] = forms.get("other", next(iter(forms.values()), ""))
    return out


def plural_names(path):
    return {el.get("name") for el in ET.parse(path).getroot() if el.tag == "plurals"}


def to_csv():
    en, ru = read(RES / "values/strings.xml"), read(RES / "values-ru/strings.xml")
    old = {}
    if CSV_PATH.exists():
        old = {r["ключ"]: r.get("Якутский", "") for r in open_table(CSV_PATH)}
    with CSV_PATH.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(COLUMNS)
        for key in en:
            w.writerow([key, en[key], ru.get(key, ""), old.get(key, "")])
    print(f"Готово: {CSV_PATH.relative_to(ROOT)}")


def to_xml():
    ru = read(RES / "values-ru/strings.xml")
    plurals = plural_names(RES / "values/strings.xml")
    rows = {r["ключ"]: (r.get("Якутский") or "").strip() for r in open_table(CSV_PATH)}
    done = sum(1 for key in ru if rows.get(key))
    if done < len(ru) and "--force" not in sys.argv:
        print(f"Якутский интерфейс пока не включён: переведено {done} из {len(ru)} надписей.")
        return
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             "<!-- Якутский интерфейс. Не править вручную: собирается из content/interface-sah.csv",
             "     командой python3 tools/strings_csv.py to-xml -->",
             "<resources>"]
    for key, ru_text in ru.items():
        text = rows.get(key) or ru_text
        text = escape(text).replace("'", "\\'")
        if key in plurals:
            lines.append(f'    <plurals name="{key}">\n        <item quantity="other">{text}</item>\n    </plurals>')
        else:
            lines.append(f'    <string name="{key}">{text}</string>')
    lines.append("</resources>\n")
    out = RES / "values-sah/strings.xml"
    out.parent.mkdir(exist_ok=True)
    out.write_text("\n".join(lines), encoding="utf-8")
    cfg = RES / "xml/locales_config.xml"
    text = cfg.read_text(encoding="utf-8")
    if 'android:name="sah"' not in text:
        cfg.write_text(text.replace("</locale-config>", '    <locale android:name="sah" />\n</locale-config>'), encoding="utf-8")
    print(f"Готово: переведено {done} из {len(ru)} надписей → {out.relative_to(ROOT)}")


if __name__ == "__main__":
    {"to-csv": to_csv, "to-xml": to_xml}.get(sys.argv[1] if len(sys.argv) > 1 else "", lambda: print(__doc__))()
