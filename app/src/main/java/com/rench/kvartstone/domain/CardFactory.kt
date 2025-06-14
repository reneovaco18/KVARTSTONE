package com.rench.kvartstone.domain

import com.rench.kvartstone.R

object CardFactory {

    /* ───────────────────────  BASIC MINIONS  ─────────────────────── */

    fun createBasicMinion(
        id: Int,
        name: String,
        manaCost: Int,
        attack: Int,
        health: Int,
        imageResName: String = "ic_card_minion_generic"
    ): MinionCard = MinionCard(
        id           = id,
        name         = name,
        manaCost     = manaCost,
        imageResName = imageResName,
        attack       = attack,
        maxHealth    = health
    )

    /* ───────────────────────  BATTLECRY MINIONS  ─────────────────── */

    fun createFireElemental() = MinionCard(
        id           = 101,
        name         = "Fire Elemental",
        manaCost     = 6,
        imageResName = "ic_card_minion_generic",
        attack       = 6,
        maxHealth    = 5,
        battlecryEffect = { _, targets ->
            targets.firstOrNull()?.let { tgt ->
                when (tgt) {
                    is MinionCard -> tgt.takeDamage(3)
                    is Hero       -> tgt.takeDamage(3)
                }
            }
        }
    )

    fun createElvenArcher() = MinionCard(
        id           = 102,
        name         = "Elven Archer",
        manaCost     = 1,
        imageResName = "ic_card_minion_generic",
        attack       = 1,
        maxHealth    = 1,
        battlecryEffect = { _, targets ->
            targets.firstOrNull()?.let { tgt ->
                when (tgt) {
                    is MinionCard -> tgt.takeDamage(1)
                    is Hero       -> tgt.takeDamage(1)
                }
            }
        }
    )

    fun createNoviceEngineer() = MinionCard(
        id           = 103,
        name         = "Novice Engineer",
        manaCost     = 2,
        imageResName = "ic_card_minion_generic",
        attack       = 1,
        maxHealth    = 1,
        battlecryEffect = { engine, _ ->
            if (engine.currentTurn == Turn.PLAYER)
                engine.drawCardForPlayer()
            else
                engine.drawCardForBot()
        }
    )

    fun createShatteredSunCleric() = MinionCard(
        id           = 104,
        name         = "Shattered Sun Cleric",
        manaCost     = 3,
        imageResName = "ic_card_minion_generic",
        attack       = 3,
        maxHealth    = 2,
        battlecryEffect = { engine, targets ->
            val target = targets.firstOrNull() as? MinionCard ?: return@MinionCard
            val friendlyBoard =
                if (engine.currentTurn == Turn.PLAYER) engine.playerBoard else engine.botBoard
            if (target in friendlyBoard) {
                target.buffAttack(1)
                target.buffHealth(1)
            }
        }
    )

    /* ───────────────────────  DEATHRATTLE MINIONS  ───────────────── */

    fun createLootHoarder() = MinionCard(
        id           = 201,
        name         = "Loot Hoarder",
        manaCost     = 2,
        imageResName = "ic_card_minion_generic",
        attack       = 2,
        maxHealth    = 1,
        deathrattleEffect = { engine, _ ->
            if (engine.currentTurn == Turn.PLAYER)
                engine.drawCardForPlayer()
            else
                engine.drawCardForBot()
        }
    )

    fun createHarvestGolem() = MinionCard(
        id           = 202,
        name         = "Harvest Golem",
        manaCost     = 3,
        imageResName = "ic_card_minion_generic",
        attack       = 2,
        maxHealth    = 3,
        deathrattleEffect = { engine, _ ->
            val damagedGolem = MinionCard(
                id           = 9999,
                name         = "Damaged Golem",
                manaCost     = 1,
                imageResName = "ic_card_minion_generic",
                attack       = 2,
                maxHealth    = 1
            )
            val board =
                if (engine.playerBoard.any { it.name == "Harvest Golem" })
                    engine.playerBoard else engine.botBoard
            if (board.size < 7) board.add(damagedGolem)
        }
    )

    fun createAbomination() = MinionCard(
        id           = 203,
        name         = "Abomination",
        manaCost     = 5,
        imageResName = "ic_card_minion_generic",
        attack       = 4,
        maxHealth    = 4,
        deathrattleEffect = { engine, _ ->
            val everyone = engine.playerBoard + engine.botBoard +
                    listOf(engine.playerHero, engine.botHero)
            everyone.forEach { c ->
                when (c) {
                    is MinionCard -> c.takeDamage(2)
                    is Hero       -> c.takeDamage(2)
                }
            }
        }
    )

    /* ───────────────────────  DIVINE-SHIELD MINIONS  ─────────────── */

    fun createArgentSquire() = MinionCard(
        id           = 301,
        name         = "Argent Squire",
        manaCost     = 1,
        imageResName = "ic_card_minion_generic",
        attack       = 1,
        maxHealth    = 1,
        hasDivineShield = true
    )

    fun createSilvermoonGuardian() = MinionCard(
        id           = 302,
        name         = "Silvermoon Guardian",
        manaCost     = 4,
        imageResName = "ic_card_minion_generic",
        attack       = 3,
        maxHealth    = 3,
        hasDivineShield = true
    )

    fun createArgentCommander() = MinionCard(
        id           = 303,
        name         = "Argent Commander",
        manaCost     = 6,
        imageResName = "ic_card_minion_generic",
        attack       = 4,
        maxHealth    = 2,
        hasDivineShield = true,
        battlecryEffect = { _, targets ->
            targets.firstOrNull()?.let { tgt ->
                when (tgt) {
                    is MinionCard -> tgt.takeDamage(2)
                    is Hero       -> tgt.takeDamage(2)
                }
            }
        }
    )

    /* ───────────────────────  SPELLS  ────────────────────────────── */

    fun createFireball() = SpellCard(
        id           = 401,
        name         = "Fireball",
        manaCost     = 4,
        imageResName = "ic_card_spell_generic",
        targetingType = TargetingType.SINGLE_CHARACTER,
        description  = "Deal 6 damage",
        effect       = { _, targets ->
            targets.firstOrNull()?.let { tgt ->
                when (tgt) {
                    is MinionCard -> tgt.takeDamage(6)
                    is Hero       -> tgt.takeDamage(6)
                }
            }
        }
    )

    fun createHealingPotion() = SpellCard(
        id           = 402,
        name         = "Healing Potion",
        manaCost     = 1,
        imageResName = "ic_card_spell_generic",
        targetingType = TargetingType.SINGLE_CHARACTER,
        description  = "Restore 3 health",
        effect       = { _, targets ->
            targets.firstOrNull()?.let { tgt ->
                when (tgt) {
                    is MinionCard -> tgt.heal(3)
                    is Hero       -> tgt.heal(3)
                }
            }
        }
    )

    fun createConsecration() = SpellCard(
        id           = 403,
        name         = "Consecration",
        manaCost     = 4,
        imageResName = "ic_card_spell_generic",
        targetingType = TargetingType.ALL_ENEMY_MINIONS,
        description  = "Deal 2 damage to all enemies",
        effect       = { engine, _ ->
            val enemies =
                if (engine.currentTurn == Turn.PLAYER)
                    engine.botBoard + listOf(engine.botHero)
                else
                    engine.playerBoard + listOf(engine.playerHero)

            enemies.forEach { enemy ->
                when (enemy) {
                    is MinionCard -> enemy.takeDamage(2)
                    is Hero       -> enemy.takeDamage(2)
                }
            }
        }
    )

    fun createDivineShieldBuff() = SpellCard(
        id           = 404,
        name         = "Divine Favor",
        manaCost     = 2,
        imageResName = "ic_card_spell_generic",
        targetingType = TargetingType.SINGLE_MINION,
        description  = "Give a minion Divine Shield",
        effect       = { _, targets ->
            val target = targets.firstOrNull() as? MinionCard ?: return@SpellCard
            target.gainDivineShield()
        }
    )

    /* ───────────────────────  SAMPLE DECKS  ─────────────────────── */

    fun createPlayerDeck(): List<Card> = listOf(
        // Basic minions
        createBasicMinion(1,  "River Crocolisk",   2, 2, 3),
        createBasicMinion(2,  "Chillwind Yeti",    4, 4, 5),
        createBasicMinion(3,  "Boulderfist Ogre",  6, 6, 7),

        // Battlecry minions
        createElvenArcher(),
        createNoviceEngineer(),
        createShatteredSunCleric(),
        createFireElemental(),

        // Deathrattle minions
        createLootHoarder(),
        createHarvestGolem(),
        createAbomination(),

        // Divine-Shield minions
        createArgentSquire(),
        createSilvermoonGuardian(),
        createArgentCommander(),

        // Spells
        createFireball(),
        createHealingPotion(),
        createConsecration(),
        createDivineShieldBuff(),

        // Extra minions to fill the deck
        createBasicMinion(10, "Bloodfen Raptor",       2, 3, 2),
        createBasicMinion(11, "Sen'jin Shieldmasta",   4, 3, 5),
        createBasicMinion(12, "Magma Rager",           3, 5, 1),
        createBasicMinion(13, "Frostwolf Grunt",       2, 2, 2),
        createBasicMinion(14, "Ironfur Grizzly",       3, 3, 3),
        createBasicMinion(15, "Raid Leader",           3, 2, 2)
    )

    fun createBotDeck(): List<Card> = createPlayerDeck() // identical for now
}