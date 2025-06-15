package com.rench.kvartstone.domain
object HeroPowerFactory {

    // Central registry of all hero powers
    fun getAllHeroPowers(): List<HeroPower> = listOf(
        createFireblast(),
        createLesserHeal(),
        createArmorUp()
    )

    fun createHeroPower(id: Int): HeroPower =
        getAllHeroPowers().find { it.id == id } ?: createFireblast()

    // Individual power definitions
    private fun createFireblast() = HeroPower(
        id = 1,
        name = "Fireblast",
        description = "Deal 1 damage to any character",
        cost = 2,
        imageResName = "ic_hero_power_fire",
        effect = { engine, target ->
            val real = when (target) {
                is List<*> -> target.firstOrNull()
                null -> if (engine.currentTurn == Turn.PLAYER)
                    engine.botHero else engine.playerHero
                else -> target
            }
            when (real) {
                is MinionCard -> real.takeDamage(1, engine)
                is Hero -> real.takeDamage(1)
            }
        }
    )

    private fun createLesserHeal() = HeroPower(
        id = 2,
        name = "Lesser Heal",
        description = "Restore 2 Health to your hero",
        cost = 2,
        imageResName = "ic_hero_power_priest",
        effect = { engine, _ ->
            if (engine.currentTurn == Turn.PLAYER)
                engine.playerHero.heal(2)
            else
                engine.botHero.heal(2)
        }
    )

    private fun createArmorUp() = HeroPower(
        id = 3,
        name = "Armor Up!",
        description = "Gain 2 Armor",
        cost = 2,
        imageResName = "ic_hero_power_warrior",
        effect = { engine, _ ->
            if (engine.currentTurn == Turn.PLAYER)
                engine.playerHero.addArmor(2)
            else
                engine.botHero.addArmor(2)
        }
    )

    // Easy method to add new powers - just add them here!
    // Example: Paladin hero power
    /*
    private fun createHealingLight() = HeroPower(
        id = 4,
        name = "Healing Light",
        description = "Restore 1 Health to all friendly characters",
        cost = 2,
        imageResName = "ic_hero_power_paladin",
        effect = { engine, _ ->
            val friendlyCharacters = if (engine.currentTurn == Turn.PLAYER)
                engine.playerBoard + listOf(engine.playerHero)
            else
                engine.botBoard + listOf(engine.botHero)
            friendlyCharacters.forEach { character ->
                when (character) {
                    is MinionCard -> character.heal(1)
                    is Hero -> character.heal(1)
                }
            }
        }
    )
    */
}
