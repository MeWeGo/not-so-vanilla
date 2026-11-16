# Eklenecekler Listesi (Grounded 26.2)

- [x] **1. Ahşap Kase (`minecraft:bowl`) ile Su Taşıma / İçme**
  - **Durum (2026-11-16):** Grounded Survival 1.0.0 ile yapıldı. Boş ahşap kaseyle suya sağ tıkla: `Kase Su` olur, kalitesi TWT2'nin su örneklemesiyle belirlenir (kirli/bulanık/temiz/saf). İçince düz su kadar susuzluk giderir, kaliteye göre quenched ve hastalık riski TWT2 ile aynıdır, kase geri kalır. Deniz suyu kaseyle alınamaz. Kamp ateşinde, fırında ve tütsülükte kaynatılır. Sunucuda test edildi (kamp ateşinde kaynama doğrulandı), oyun içinde içme henüz denenmedi.
  - **Ne yapılacak:** Çorba tutabilen normal ahşap kasenin su kaynağından su doldurabilmesi, kirli/temiz su olarak içilebilmesi ve fırında/kamp ateşinde kaynatılabilmesi (`Thirst Was Taken 2` ile uyumlu şekilde `grounded_inventory` veya `grounded_tweaks` üzerinden eklenecek).

- [x] **2. Daha Uzun Günler (`Better Days` - `betterdays`)**
  - **Durum (2026-11-16):** Pakete eklendi (stage 2). Ayar: `speedMethod = SEASON`, `seasonDayMinutes = 40` yani bir gün-gece 40 dakika (vanilla 20), mevsime göre değişiyor: yazın gündüz uzun, kışın gece uzun (enlem 48, Serene Seasons ile). Sunucuda zamanın yaklaşık yarı hızda aktığı doğrulandı.
  - **Modrinth:** `betterdays` (`26.2-4.1.1.7-NEOFORGE`, `release`, zorunlu bağımlılığı yok)
  - **Ne yapılacak:** `build_pack.py` listesine ve sunucuya eklenecek; gündüz/gece süreleri (ör. 2x veya 3x daha uzun günler) paketin hayatta kalma temposuna göre config dosyasından ayarlanacak ve `Serene Seasons` ile uyumu kontrol edilecek.

- [x] **3. Meyveler ve Meyveden Az Miktarda Su / Meyve Suyu Elde Etme**
  - **Durum (2026-11-16):** Saro's Fruit Trees eklendi (elma, portakal, armut). Modun dünya üretimi dosyaları NeoForge 26'nın okumadığı eski `forge/` klasöründeydi; ağaçlar doğada çıkmayacaktı. Grounded Survival bunları `neoforge/biome_modifier` ile yeniden bağlıyor (orman/ova/çayır/savan, nadir koruluklar). Susuzluk: portakal ve armut 2, elma ve kavun 2, meyveler 1, quenched 0. **Meyve Suyu**: cam şişe + 3 meyve → 3 susuzluk, 0 quenched. Sprout & Soil seçilmedi (jar'da meyve ağacı yok). Crop & Kettle üzümleri datapack eşyası olduğu için değer verilemedi.
  - **Modrinth Seçenekleri (26.2 NeoForge):** `saros-fruit-trees` (Saro's Fruit Trees) veya `sprout-and-soil` (Sprout & Soil)
  - **Denge Kuralı:** Meyveler ve meyve suları yalnızca ilk günlerde idarelik olacak şekilde **düşük susuzluk** gidermeli (ör. `1–2` susuzluk, `0` quenched); sadece meyve yiyerek su ihtiyacı karşılanamamalı, `Waterskin` / `Copper Canteen` ve su kaynatma her türlü zorunlu kalmalı.
  - **Ne yapılacak:** Yeni meyve ağaçları eklenecek ve meyvelerin `Thirst Was Taken 2` (`thirstwastaken2.json`) susuzluk/quenched değerleri bu dengeye göre düşük tutulacak.

- [x] **4. Kamp Ateşinin (`Campfire`) Yanına / Üstüne Su Şişesi Koyarak Kaynatma**
  - **Durum (2026-11-16):** Yapıldı. Su şişesi, TWT2 terracotta kase ve bizim ahşap kase lit kamp ateşine konunca 30 saniyede kaynar (kirli/bulanık → temiz). Sunucuda üçü de doğrulandı. Not: TWT2 zaten kamp ateşi üstüne asılan kazan (hanging pot) ve mataraya kamp ateşine tıklayarak kaynatmayı destekliyordu.
  - **Ne yapılacak:** Kirli/arıtılmamış su şişelerinin (`Water Bottle`) ve su kaplarının doğrudan yanan bir kamp ateşinin (`Campfire`) yanına bırakılarak (veya üzerine yerleştirilerek) zamanla kaynatılıp arıtılabilmesi (`Thirst Was Taken 2` ile uyumlu şekilde `grounded_tweaks` / özel mod mekaniği olarak eklenecek).

- [x] **5. Vanilla ve Mod Eşyalarına Ağırlık (`tarkovcraft_core:weight`) Eklenmesi & `Strength` Yeteneğinin Çalışması**
  - **Durum (2026-11-16):** Grounded Survival tüm ağırlıksız eşyalara gram ağırlığı veriyor (Medical System eşyaları kendi değerini korur). Örnekler: demir göğüslük 4 kg, tam demir zırh ~9.7 kg, demir kazma 1.3 kg, kırıktaş 150 g (64'lü deste ~9.6 kg), maden 300 g, su kovası 8 kg, şişe su 450 g. Sunucuda doğrulandı. Strength kasmanın oyuncuyla denenmesi gerekiyor (aşırı yük + koşma).
  - **Sorun:** `TarkovCraft Core` varsayılan olarak sadece Medical System eşyalarına gram cinsinden ağırlık veriyor; vanilla eşyalar (`0 g`) ağırlıksız göründüğü için toplam ağırlık `20 kg` (`20000 g`) `weight_limit` sınırını aşmıyor ve `overweight + sprinting` şartına bağlı olan `Strength` yeteneği hiç XP kazanmıyor.
  - **Ne yapılacak:** `grounded_inventory` (veya `grounded_tweaks`) üzerinden `ModifyDefaultComponentsEvent` ile zırhlar, aletler, silahlar, bloklar, madenler, yiyecekler ve su kaplarına dengeli gram ağırlıkları (`tarkovcraft_core:weight`) atanacak; böylece ağırlık barı, eşya tooltip ağırlıkları ve `Strength` kasma düzgün çalışacak.
