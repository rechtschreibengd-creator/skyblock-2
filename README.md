# SkyblockTest (Paper 26.2, Java 25)

Hypixel-SkyBlock-aehnliches Testplugin: Stats, 8 Skills, 20 Enchantments, 17 Abilities, 13 Pets, 4 Minibosse, ~315 Items, 226 Rezepte.

## Bauen
Voraussetzung: JDK 25 und Internet (fuer paper-api von repo.papermc.io).

    gradle build        # oder ./gradlew build, falls du den Wrapper erzeugt hast
    -> build/libs/SkyblockTest-0.1.0.jar in den plugins-Ordner deines Paper-26.2-Servers legen

## Befehle (Admin: Permission skyblocktest.admin / OP)
- /menu (oder Nether-Stern in Hotbar-Slot 9, Rechtsklick), /menu get, /stats, /skills
- /sb items            Item-Browser (mit Seiten), Klick = Item nehmen
- /sb give <item> [n]
- /sb enchant <name> <level>, /sb enchantall, /sb enchants
- /sb setskill <skill> <lvl>, /sb addxp <skill> <xp>, /sb maxskills
- /sb heal, /sb reload (items.yml neu laden)

## Erweitern
- Items: plugins/SkyblockTest/items.yml, dann /sb reload
- Neue Abilities: AbilityRegistry.java (reg(...)-Zeile ergaenzen)
- Neue Enchantments: Eintrag in SkyEnchant.java + Wirkung in CombatListener/BlockListener
- Neue Skills: Eintrag in SkillType.java + XP-Quelle in einem Listener

## Schadensmodell
1 Skyblock-HP = 0.2 Vanilla-HP (100 HP = 20 Vanilla-HP, 10 Herzen skaliert).
Damage = (5 + Waffe + Strength/5) * (1 + Strength/100) * (1 + Enchants) * (1 + Combat-Lvl*1%) * Crit.
Eingehend: Schaden * 100/(100+Defense).

## Neu: Crafting, Pets, Minibosse
- /craft (oder Menue -> SkyBlock Werkbank): 3x3-Raster, Zutaten haben MENGEN PRO SLOT (z.B. 64x Coal in einem Slot). /recipes = Rezeptbuch.
- recipes.yml: lowercase = eigenes Item (items.yml-ID), UPPERCASE = Vanilla-Material. "TOKEN*N" = N Stueck pro Slot (formlos: insgesamt).
  Beispiel: Enchanted Coal = 160 Coal (formlos), Wither Catalyst = 2x2 Enchanted Coal.
- Pets: Pet-Ei rechtsklicken schaltet es frei, /pets zum Ausruesten. Level 1-100, XP kommt aus dem passenden Skill.
- Minibosse (bosses.yml): Revenant Warlord (Zombie), Void Stalker (Enderman), Bone Reaper (Skelett), Venom Queen (Spinne).
  Spawnen selten natuerlich oder per Beschwoerungs-Item (/sb give summon_revenant ...). Test: /sb boss <id>.
  Drops mit Seltenheit, Magic Find erhoeht die Chancen. Uebersicht im Menue -> Minibosse.
- Talismane wirken aus dem Inventar (jeder nur 1x). Enchant-Buecher: im Inventar auf ein passendes Item klicken.
- WICHTIG beim Update: items.yml, recipes.yml und bosses.yml im Ordner plugins/SkyblockTest/ loeschen, damit die neuen Versionen erzeugt werden (oder die Dateien ersetzen), dann /sb reload bzw. Neustart.
