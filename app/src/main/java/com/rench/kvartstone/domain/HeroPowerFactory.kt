package com.rench.kvartstone.domain

object HeroPowerFactory {

    fun createHeroPower(id: Int): HeroPower = when (id) {
        1 -> HeroPower(
            id          = 1,
            name        = "Fireblast",
            description = "Deal 1 damage.",
            cost        = 2,
            imageResName = "ic_hero_power_fire",
            effect = { engine, target ->
                // target can be a single object, a List, or null
                val realTarget = when (target) {
                    is List<*> -> target.firstOrNull()       // take first element of the list
                    null       -> engine.botHero            // no target supplied – hit enemy hero
                    else       -> target                    // already a single object
                }

                when (realTarget) {
                    is MinionCard -> realTarget.takeDamage(1, engine)
                    is Hero       -> realTarget.takeDamage(1)
                }
            }
        )

        2 -> HeroPower(
            id          = 2,
            name        = "Reinforce",
            description = "Summon a 1/1 Silver Hand Recruit.",
            cost        = 2,
            imageResName = "ic_hero_power_recruit",
            effect = { engine, _ ->
                // summon logic here
            }
        )

        else -> createHeroPower(1)
    }
}
