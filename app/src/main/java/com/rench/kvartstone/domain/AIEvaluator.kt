package com.rench.kvartstone.domain

class AIEvaluator(private val gameEngine: GameEngineInterface) {

    fun planTurn(hand: List<Card>, friendlyBoard: List<MinionCard>, enemyBoard: List<MinionCard>, mana: Int): List<AIDecision> {
        val decisions = mutableListOf<AIDecision>()
        var remainingMana = mana
        val availableCards = hand.toMutableList()

        while (remainingMana > 0 && availableCards.isNotEmpty()) {
            val bestCard = findBestCard(availableCards, remainingMana, enemyBoard)
            if (bestCard != null) {
                val cardIndex = hand.indexOf(bestCard)
                val target = if (bestCard is SpellCard) {
                    findBestSpellTarget(bestCard, enemyBoard, gameEngine.playerHero)
                } else null

                decisions.add(AIDecision.PlayCard(cardIndex, target))
                remainingMana -= bestCard.manaCost
                availableCards.remove(bestCard)
            } else {
                break
            }
        }

        // Add attack decisions for available minions
        friendlyBoard.forEachIndexed { index, minion ->
            if (minion.canAttack()) {
                val target = findBestAttackTarget(minion, enemyBoard, gameEngine.playerHero)
                if (target != null) {
                    decisions.add(AIDecision.Attack(index, target))
                }
            }
        }

        return decisions
    }

    private fun findBestCard(cards: List<Card>, mana: Int, enemyBoard: List<MinionCard>): Card? {
        val playableCards = cards.filter { it.manaCost <= mana }
        if (playableCards.isEmpty()) return null

        return when {
            enemyBoard.size > 2 -> {
                playableCards.filterIsInstance<SpellCard>()
                    .filter { it.targetingType == TargetingType.ALL_ENEMY_MINIONS }
                    .maxByOrNull { evaluateCard(it) }
                    ?: playableCards.maxByOrNull { evaluateCard(it) }
            }
            else -> {
                playableCards.maxByOrNull { evaluateCard(it) }
            }
        }
    }

    private fun evaluateCard(card: Card): Double {
        return when (card) {
            is MinionCard -> {
                val statValue = (card.attack + card.maxHealth) / card.manaCost.toDouble()
                val efficiency = if (card.manaCost == 0) 10.0 else statValue

                val keywordBonus = when {
                    card.hasDivineShield -> 1.5
                    card.battlecryEffect != null -> 1.2
                    card.deathrattleEffect != null -> 1.1
                    else -> 1.0
                }

                efficiency * keywordBonus
            }
            is SpellCard -> {
                val baseCost = card.manaCost.toDouble()
                if (baseCost == 0.0) 5.0 else 3.0 / baseCost
            }
            else -> 1.0
        }
    }

    fun canPlayMoreCards(hand: List<Card>, mana: Int): Boolean {
        return hand.any { it.manaCost <= mana }
    }

    fun findBestPlayableCard(hand: List<Card>, mana: Int): Int? {
        val playableCards = hand.withIndex().filter { it.value.manaCost <= mana }
        return playableCards.maxByOrNull { evaluateCard(it.value) }?.index
    }

    fun findBestAttackTarget(attacker: MinionCard, enemyBoard: List<MinionCard>, enemyHero: Hero): Any? {
        // Prioritize weak minions that can be killed
        val killableMinions = enemyBoard.filter { it.currentHealth <= attacker.attack }
        if (killableMinions.isNotEmpty()) {
            return killableMinions.maxByOrNull { it.attack + it.maxHealth }
        }

        // Otherwise attack face if no good trades
        return if (enemyBoard.isEmpty()) enemyHero else enemyBoard.randomOrNull()
    }

    fun findBestSpellTarget(spell: SpellCard, enemyBoard: List<MinionCard>, enemyHero: Hero): Any? {
        return when (spell.targetingType) {
            TargetingType.SINGLE_ENEMY_MINION -> enemyBoard.maxByOrNull { it.attack + it.maxHealth }
            TargetingType.SINGLE_CHARACTER -> {
                val allTargets = enemyBoard + listOf(enemyHero)
                allTargets.randomOrNull()
            }
            else -> null
        }
    }
}
