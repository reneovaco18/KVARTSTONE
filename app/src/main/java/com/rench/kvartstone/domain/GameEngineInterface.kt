package com.rench.kvartstone.domain

interface GameEngineInterface {
    val currentTurn: Turn
    val playerMana: Int
    val botMana: Int
    val playerMaxMana: Int
    val botMaxMana: Int
    val turnNumber: Int
    val playerDeck: MutableList<Card>
    val botDeck: MutableList<Card>
    val playerHand: MutableList<Card>
    val botHand: MutableList<Card>
    val playerBoard: MutableList<MinionCard>
    val botBoard: MutableList<MinionCard>
    val playerHero: Hero
    val botHero: Hero
    val gameOver: Boolean
    val playerWon: Boolean

    fun drawCardForPlayer(): Card?
    fun drawCardForBot(): Card?
    fun playCardFromHand(cardIndex: Int, target: Any?): Boolean
    fun endTurn()
    fun useHeroPower(target: Any?): Boolean


    fun attack(attacker: MinionCard, target: Any): Boolean
}
