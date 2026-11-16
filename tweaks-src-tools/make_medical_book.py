"""Generates the Field Medicine book: grounded:medical_guide function + en_us/tr_tr lang files in tweaks-src.

Recipes are drawn as 3x3 grids of item icons using 26.x text "object" components (atlas sprites);
empty cells use the GUI slot sprite. Item and block names come from the game's own translation keys.
Recipes copied from the Medical System 2.14.0 jar (data/medsystem/recipe/*.json).
"""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), "..", "tweaks-src")

# ingredient key -> (atlas, sprite, translation key)
ING = {
    "paper": ("minecraft:items", "minecraft:item/paper", "item.minecraft.paper"),
    "wool": ("minecraft:blocks", "minecraft:block/white_wool", "book.grounded.ing.wool"),
    "carpet": ("minecraft:blocks", "minecraft:block/white_wool", "book.grounded.ing.carpet"),
    "stick": ("minecraft:items", "minecraft:item/stick", "item.minecraft.stick"),
    "leather": ("minecraft:items", "minecraft:item/leather", "item.minecraft.leather"),
    "nugget": ("minecraft:items", "minecraft:item/iron_nugget", "item.minecraft.iron_nugget"),
    "ingot": ("minecraft:items", "minecraft:item/iron_ingot", "item.minecraft.iron_ingot"),
    "bottle": ("minecraft:items", "minecraft:item/glass_bottle", "item.minecraft.glass_bottle"),
    "melon": ("minecraft:items", "minecraft:item/glistering_melon_slice", "item.minecraft.glistering_melon_slice"),
    "tear": ("minecraft:items", "minecraft:item/ghast_tear", "item.minecraft.ghast_tear"),
    "shears": ("minecraft:items", "minecraft:item/shears", "item.minecraft.shears"),
    "string": ("minecraft:items", "minecraft:item/string", "item.minecraft.string"),
    "flower": ("minecraft:blocks", "minecraft:block/poppy", "book.grounded.ing.flower"),
    "bandage": ("minecraft:items", "medsystem:item/bandage", "item.medsystem.bandage"),
}
MED = {  # medsystem item -> sprite
    "bandage": "medsystem:item/bandage", "tourniquet": "medsystem:item/tourniquet",
    "pressure_dressing": "medsystem:item/pressure_dressing", "splint": "medsystem:item/splint",
    "painkillers": "medsystem:item/painkillers", "morphine_injector": "medsystem:item/morphine_injector",
    "first_aid_kit": "medsystem:item/first_aid_kit", "bloodbag": "medsystem:item/bloodbag/bloodbag_empty",
    "regenerative_injector": "medsystem:item/regenerative_injector",
    "emergency_surgery_kit": "medsystem:item/emergency_surgery_kit",
}
# item -> (rows, letter map) exactly as in the recipe JSON
RECIPES = [
    ("bandage", ["PWP", " P "], {"P": "paper", "W": "wool"}),
    ("tourniquet", [" S ", "LIL"], {"S": "stick", "L": "leather", "I": "nugget"}),
    ("pressure_dressing", ["P  ", "CCC", " IS"], {"P": "paper", "C": "carpet", "I": "nugget", "S": "string"}),
    ("splint", ["ISI", "LSL"], {"I": "nugget", "S": "stick", "L": "leather"}),
    ("painkillers", ["PFP", "FPF"], {"P": "paper", "F": "flower"}),
    ("morphine_injector", [" I ", "MBP"], {"I": "nugget", "M": "melon", "B": "bottle", "P": "paper"}),
    ("first_aid_kit", ["FPF", "BFB", "LLL"], {"F": "flower", "P": "paper", "B": "bandage", "L": "leather"}),
    ("bloodbag", [" I ", "PBP"], {"I": "nugget", "P": "paper", "B": "bottle"}),
    ("regenerative_injector", [" I ", "TBP"], {"I": "nugget", "T": "tear", "B": "bottle", "P": "paper"}),
    ("emergency_surgery_kit", [" LL", "LTS", "LSI"], {"L": "leather", "T": "shears", "S": "stick", "I": "ingot"}),
]
TRIAGE = ["bandage", "tourniquet", "pressure_dressing", "splint", "painkillers", "first_aid_kit", "bloodbag", "emergency_surgery_kit"]

TEXT = {
    "en_us": {
        "intro": "§lFIELD MEDICINE§r\n\nEvery limb has its own health. Lose your head or torso and you die.\n\nTreatment takes time: hold the item. Let go early and it only half works.\n\nHover an item for exact values.",
        "triage_title": "§lWHAT TREATS WHAT§r\n",
        "tips": "§lTIPS§r\n\nStop the bleeding first, then heal.\n\nSprinting reopens fresh wounds.\n\nBandits and survivors carry medicine. Give a survivor a meal and they may trade some.\n\n§8R / U: JEI recipes§r",
        "ing.wool": "Any wool", "ing.carpet": "Any carpet", "ing.flower": "Any small flower",
        "tri.bandage": "light bleed", "tri.tourniquet": "heavy bleed (arm/leg)", "tri.pressure_dressing": "any bleed + fracture",
        "tri.splint": "fracture", "tri.painkillers": "pain, headache", "tri.first_aid_kit": "health",
        "tri.bloodbag": "blood loss", "tri.emergency_surgery_kit": "dead limb",
        "desc.bandage": "Stops light and moderate bleeding. 2 s.",
        "desc.tourniquet": "Heavy and critical bleeding. Arms and legs only. 3 s.",
        "desc.pressure_dressing": "Stops any bleed and binds a fracture. Slow: 7 s.",
        "desc.splint": "Fixes a fracture. 5 s.",
        "desc.painkillers": "Pain gone for 10 min, kicks in after 45 s. 4 uses.",
        "desc.morphine_injector": "Fast, strong painkiller; removes shock. Weakness afterwards.",
        "desc.first_aid_kit": "2 HP per second while held. 30 uses. Stops light bleeding.",
        "desc.bloodbag": "Left-click: mode. Extraction draws blood, Transfusion gives it. Match blood types.",
        "desc.regenerative_injector": "Slow regeneration for 90 s. Hunger and concussion risk later.",
        "desc.emergency_surgery_kit": "Restores a dead limb in 15 s. -30% limb health for 15 min.",
        "received": "You received a field medicine book with every medical recipe and what it treats.",
    },
    "tr_tr": {
        "intro": "§lSAHA TIBBI§r\n\nHer uzvun ayrı canı var. Kafa ya da gövde ölürse sen de ölürsün.\n\nTedavi süreli: eşyayı basılı tut, erken bırakırsan yarım kalır.\n\nTam değerler için eşyanın üstüne gel.",
        "triage_title": "§lHANGİ YARA, NE İLAÇ§r\n",
        "tips": "§lİPUÇLARI§r\n\nÖnce kanamayı durdur, sonra can ver.\n\nKoşmak taze yarayı açar.\n\nHaydutlar ve hayatta kalanlar ilaç taşır. Hayatta kalana yemek ver, ilaç versin.\n\n§8R / U: JEI tarifleri§r",
        "ing.wool": "Herhangi bir yün", "ing.carpet": "Herhangi bir halı", "ing.flower": "Herhangi bir küçük çiçek",
        "tri.bandage": "hafif kanama", "tri.tourniquet": "ağır kanama (kol/bacak)", "tri.pressure_dressing": "her kanama + kırık",
        "tri.splint": "kırık", "tri.painkillers": "ağrı, baş ağrısı", "tri.first_aid_kit": "can",
        "tri.bloodbag": "kan kaybı", "tri.emergency_surgery_kit": "ölü uzuv",
        "desc.bandage": "Hafif ve orta kanamayı durdurur. 2 sn.",
        "desc.tourniquet": "Ağır ve kritik kanama. Sadece kol ve bacak. 3 sn.",
        "desc.pressure_dressing": "Her kanamayı durdurur, kırığı da sarar. Yavaş: 7 sn.",
        "desc.splint": "Kırığı düzeltir. 5 sn.",
        "desc.painkillers": "Ağrıyı 10 dk keser, 45 sn sonra etki eder. 4 kullanım.",
        "desc.morphine_injector": "Hızlı ve güçlü ağrı kesici, şoku alır. Sonra halsizlik yapar.",
        "desc.first_aid_kit": "Basılı tuttukça saniyede 2 can. 30 kullanım. Hafif kanamayı durdurur.",
        "desc.bloodbag": "Sol tık: mod. Extraction kan çeker, Transfusion kan verir. Kan grubu uymalı.",
        "desc.regenerative_injector": "90 sn yavaş can yeniler. Sonra açlık ve sarsıntı riski.",
        "desc.emergency_surgery_kit": "Ölü uzvu 15 sn'de geri getirir. 15 dk uzuv canı -%30.",
        "received": "Saha tıbbı kitabını aldın. İlaç tariflerini ve ne işe yaradıklarını anlatıyor.",
    },
}


def snbt(v):
    """Minimal SNBT writer for text components (dicts, lists, strings)."""
    if isinstance(v, dict):
        return "{" + ",".join(f"{k}:{snbt(x)}" for k, x in v.items()) + "}"
    if isinstance(v, list):
        return "[" + ",".join(snbt(x) for x in v) + "]"
    return json.dumps(v, ensure_ascii=False)


def icon(atlas, sprite):
    # Sprite glyphs are multiplied by the text colour; book text is black, so force white to keep real colours.
    return {"object": "atlas", "atlas": atlas, "sprite": sprite, "color": "white"}


SLOT = icon("minecraft:gui", "minecraft:container/slot")
T = lambda key: {"translate": f"book.grounded.{key}"}


def recipe_page(item, rows, keys):
    rows = rows + [" " * 3] * (3 - len(rows)) if len(rows) < 3 else rows
    page = ["", {"translate": f"item.medsystem.{item}", "bold": True}, " ", icon("minecraft:items", MED[item]), "\n\n"]
    for r, row in enumerate(rows):
        page.append(" ")
        for ch in row:
            page.append(SLOT if ch == " " else icon(*ING[keys[ch]][:2]))
        if r == 1:
            page += ["  → ", icon("minecraft:items", MED[item])]
        page.append("\n")
    page.append("\n")
    for ch in dict.fromkeys(c for row in rows for c in row if c != " "):
        atlas, sprite, name = ING[keys[ch]]
        page += [icon(atlas, sprite), " ", {"translate": name}, "\n"]
    page += ["\n", T(f"medical.desc.{item}")]
    return page


def main():
    pages = [[T("medical.intro")]]
    tri = ["", T("medical.triage_title")]
    for item in TRIAGE:
        tri += ["\n", icon("minecraft:items", MED[item]), " ", T(f"medical.tri.{item}")]
    pages.append(tri)
    pages += [recipe_page(*r) for r in RECIPES]
    pages.append([T("medical.tips")])

    lang_dir = os.path.join(ROOT, "assets", "grounded", "lang")
    os.makedirs(lang_dir, exist_ok=True)
    for lang, texts in TEXT.items():
        data = {}
        for k, v in texts.items():
            data[f"book.grounded.{k}" if k.startswith("ing.") else f"book.grounded.medical.{k}"] = v
        with open(os.path.join(lang_dir, f"{lang}.json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=1, ensure_ascii=False)

    book = f'written_book[written_book_content={{title:"Field Medicine",author:"Grounded",pages:{snbt(pages)}}}]'
    fn = ("# One-time field medicine book (generated by tweaks-src-tools/make_medical_book.py; do not edit by hand).\n"
          f"give @s {book} 1\n"
          'tellraw @s {"translate":"book.grounded.medical.received","color":"gray","italic":true}\n')
    with open(os.path.join(ROOT, "data", "grounded", "function", "medical_guide.mcfunction"), "w", encoding="utf-8") as f:
        f.write(fn)
    print(len(pages), "pages,", len(fn), "chars")


if __name__ == "__main__":
    main()
