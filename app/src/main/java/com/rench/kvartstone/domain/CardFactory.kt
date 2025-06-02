package com.rench.kvartstone.domain

import com.rench.kvartstone.R

object CardFactory {

    // Create basic minion cards
    fun createBasicMinion(
        id: Int,
        name: String,
        manaCost: Int,
        attack: Int,
        health: Int,
        imageRes: Int = R.drawable.ic_card_minion_generic
    ): MinionCard {
        return MinionCard(
            id = id,
            name = name,
            manaCost = manaCost,
            imageRes = imageRes,
            attack = attack,
            maxHealth = health
        )
    }

    // BATTLECRY CARDS
    fun createFireElemental(): MinionCard {
        return MinionCard(
            id = 101,
            name = "Fire Elemental",
            manaCost = 6,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 6,
            maxHealth = 5,
            battlecryEffect = { gameEngine, targets ->
                // Deal 3 damage to any character
                if (targets.isNotEmpty()) {
                    when (val target = targets[0]) {
                        is MinionCard -> target.takeDamage(3)
                        is Hero -> target.takeDamage(3)
                    }
                }
            }
        )
    }

    fun createElvenArcher(): MinionCard {
        return MinionCard(
            id = 102,
            name = "Elven Archer",
            manaCost = 1,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 1,
            maxHealth = 1,
            battlecryEffect = { gameEngine, targets ->
                // Deal 1 damage to any character
                if (targets.isNotEmpty()) {
                    when (val target = targets[0]) {
                        is MinionCard -> target.takeDamage(1)
                        is Hero -> target.takeDamage(1)
                    }
                }
            }
        )
    }

    fun createNoviceEngineer(): MinionCard {
        return MinionCard(
            id = 103,
            name = "Novice Engineer",
            manaCost = 2,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 1,
            maxHealth = 1,
            battlecryEffect = { gameEngine, _ ->
                // Draw a card
                if (gameEngine.currentTurn == Turn.PLAYER) {
                    gameEngine.drawCardForPlayer()
                } else {
                    gameEngine.drawCardForBot()
                }
            }
        )
    }

    fun createShatteredSunCleric(): MinionCard {
        return MinionCard(
            id = 104,
            name = "Shattered Sun Cleric",
            manaCost = 3,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 3,
            maxHealth = 2,
            battlecryEffect = { gameEngine, targets ->
                // Give a friendly minion +1/+1
                if (targets.isNotEmpty() && targets[0] is MinionCard) {
                    val target = targets[0] as MinionCard
                    val friendlyBoard = if (gameEngine.currentTurn == Turn.PLAYER) {
                        gameEngine.playerBoard
                    } else {
                        gameEngine.botBoard
                    }

                    if (friendlyBoard.contains(target)) {
                        target.buffAttack(1)
                        target.buffHealth(1)
                    }
                }
            }
        )
    }

    // DEATHRATTLE CARDS
    fun createLootHoarder(): MinionCard {
        return MinionCard(
            id = 201,
            name = "Loot Hoarder",
            manaCost = 2,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 2,
            maxHealth = 1,
            deathrattleEffect = { gameEngine ->
                // Draw a card when this minion dies
                if (gameEngine.currentTurn == Turn.PLAYER) {
                    gameEngine.drawCardForPlayer()
                } else {
                    gameEngine.drawCardForBot()
                }
            }
        )
    }

    fun createHarvestGolem(): MinionCard {
        return MinionCard(
            id = 202,
            name = "Harvest Golem",
            manaCost = 3,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 2,
            maxHealth = 3,
            deathrattleEffect = { gameEngine ->
                // Summon a 2/1 Damaged Golem
                val damagedGolem = MinionCard(
                    id = 9999,
                    name = "Damaged Golem",
                    manaCost = 1,
                    imageRes = R.drawable.ic_card_minion_generic,
                    attack = 2,
                    maxHealth = 1
                )

                val board = if (gameEngine.playerBoard.any { it.name == "Harvest Golem" }) {
                    gameEngine.playerBoard
                } else {
                    gameEngine.botBoard
                }

                if (board.size < 7) {
                    board.add(damagedGolem)
                }
            }
        )
    }

    fun createAbomination(): MinionCard {
        return MinionCard(
            id = 203,
            name = "Abomination",
            manaCost = 5,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 4,
            maxHealth = 4,
            deathrattleEffect = { gameEngine ->
                // Deal 2 damage to all characters
                val allCharacters = gameEngine.playerBoard + gameEngine.botBoard +
                        listOf(gameEngine.playerHero, gameEngine.botHero)

                allCharacters.forEach { character ->
                    when (character) {
                        is MinionCard -> character.takeDamage(2)
                        is Hero -> character.takeDamage(2)
                    }
                }
            }
        )
    }

    // DIVINE SHIELD CARDS
    fun createArgentSquire(): MinionCard {
        return MinionCard(
            id = 301,
            name = "Argent Squire",
            manaCost = 1,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 1,
            maxHealth = 1,
            hasDivineShield = true
        )
    }

    fun createSilvermoonGuardian(): MinionCard {
        return MinionCard(
            id = 302,
            name = "Silvermoon Guardian",
            manaCost = 4,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 3,
            maxHealth = 3,
            hasDivineShield = true
        )
    }

    fun createArgentCommander(): MinionCard {
        return MinionCard(
            id = 303,
            name = "Argent Commander",
            manaCost = 6,
            imageRes = R.drawable.ic_card_minion_generic,
            attack = 4,
            maxHealth = 2,
            hasDivineShield = true,
            battlecryEffect = { gameEngine, targets ->
                // Deal 2 damage (Charge-like effect with divine shield)
                if (targets.isNotEmpty()) {
                    when (val target = targets[0]) {
                        is MinionCard -> target.takeDamage(2)
                        is Hero -> target.takeDamage(2)
                    }
                }
            }
        )
    }

    // SPELL CARDS
    fun createFireball(): SpellCard {
        return SpellCard(
            id = 401,
            name = "Fireball",
            manaCost = 4,
            imageRes = R.drawable.ic_card_spell_generic,
            targetingType = TargetingType.SINGLE_CHARACTER,
            description = "Deal 6 damage",
            effect = { gameEngine, targets ->
                if (targets.isNotEmpty()) {
                    when (val target = targets[0]) {
                        is MinionCard -> target.takeDamage(6)
                        is Hero -> target.takeDamage(6)
                    }
                }
            }
        )
    }

    fun createHealingPotion(): SpellCard {
        return SpellCard(
            id = 402,
            name = "Healing Potion",
            manaCost = 1,
            imageRes = R.drawable.ic_card_spell_generic,
            targetingType = TargetingType.SINGLE_CHARACTER,
            description = "Restore 3 health",
            effect = { gameEngine, targets ->
                if (targets.isNotEmpty()) {
                    when (val target = targets[0]) {
                        is MinionCard -> target.heal(3)
                        is Hero -> target.heal(3)
                    }
                }
            }
        )
    }

    fun createConsecration(): SpellCard {
        return SpellCard(
            id = 403,
            name = "Consecration",
            manaCost = 4,
            imageRes = R.drawable.ic_card_spell_generic,
            targetingType = TargetingType.ALL_ENEMY_MINIONS,
            description = "Deal 2 damage to all enemies",
            effect = { gameEngine, _ ->
                val enemies = if (gameEngine.currentTurn == Turn.PLAYER) {
                    gameEngine.botBoard + listOf(gameEngine.botHero)
                } else {
                    gameEngine.playerBoard + listOf(gameEngine.playerHero)
                }

                enemies.forEach { enemy ->
                    when (enemy) {
                        is MinionCard -> enemy.takeDamage(2)
                        is Hero -> enemy.takeDamage(2)
                    }
                }
            }
        )
    }

    fun createDivineShieldBuff(): SpellCard {
        return SpellCard(
            id = 404,
            name = "Divine Favor",
            manaCost = 2,
            imageRes = R.drawable.ic_card_spell_generic,
            targetingType = TargetingType.SINGLE_MINION,
            description = "Give a minion Divine Shield",
            effect = { gameEngine, targets ->
                if (targets.isNotEmpty() && targets[0] is MinionCard) {
                    val target = targets[0] as MinionCard
                    target.gainDivineShield()
                }
            }
        )
    }

    // Create sample decks
    fun createPlayerDeck(): List<Card> {
        return listOf(
            // Basic minions
            createBasicMinion(1, "River Crocolisk", 2, 2, 3),
            createBasicMinion(2, "Chillwind Yeti", 4, 4, 5),
            createBasicMinion(3, "Boulderfist Ogre", 6, 6, 7),

            // Battlecry minions
            createElvenArcher(),
            createNoviceEngineer(),
            createShatteredSunCleric(),
            createFireElemental(),

            // Deathrattle minions
            createLootHoarder(),
            createHarvestGolem(),
            createAbomination(),

            // Divine Shield minions
            createArgentSquire(),
            createSilvermoonGuardian(),
            createArgentCommander(),

            // Spells
            createFireball(),
            createHealingPotion(),
            createConsecration(),
            createDivineShieldBuff(),

            // More basic minions to fill deck
            createBasicMinion(10, "Bloodfen Raptor", 2, 3, 2),
            createBasicMinion(11, "Sen'jin Shieldmasta", 4, 3, 5),
            createBasicMinion(12, "Magma Rager", 3, 5, 1),
            createBasicMinion(13, "Frostwolf Grunt", 2, 2, 2),
            createBasicMinion(14, "Ironfur Grizzly", 3, 3, 3),
            createBasicMinion(15, "Raid Leader", 3, 2, 2)
        )
    }

    fun createBotDeck(): List<Card> = createPlayerDeck() // Same deck for simplicity
}
