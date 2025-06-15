package com.rench.kvartstone.ui.GamePlay

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.*
import com.rench.kvartstone.ui.adapters.CardHandAdapter
import com.rench.kvartstone.ui.adapters.MinionBoardAdapter
import com.rench.kvartstone.ui.fragments.CardDetailFragment
import com.rench.kvartstone.ui.viewmodel.GameViewModel

import android.view.ViewGroup
import com.rench.kvartstone.ui.anim.animateSpell


class GamePlayFragment : Fragment(R.layout.fragment_game_play) {

    private val vm: GameViewModel by viewModels()
    private val args: GamePlayFragmentArgs by navArgs()
    private lateinit var deckCountText: TextView
    /* ---------- adapters ---------- */
    private lateinit var enemyHeroCard: CardView
    private lateinit var handAdapter: CardHandAdapter
    private lateinit var playerBoardAdapter: MinionBoardAdapter
    private lateinit var botBoardAdapter: MinionBoardAdapter
    private lateinit var handRv      : RecyclerView
    private lateinit var playerBoardRv: RecyclerView
    private lateinit var botBoardRv  : RecyclerView
    /* ---------- views ---------- */

    private var hasNavigatedToResult = false

    private lateinit var handToggle: FloatingActionButton
    private lateinit var playerHandArea: CardView
    private lateinit var playerHeroHealth: TextView
    private lateinit var botHeroHealth: TextView
    private lateinit var playerManaText: TextView
    private lateinit var turnNumberText: TextView
    private lateinit var endTurnBtn: Button
    private lateinit var heroPowerBtn: Button
    private lateinit var statusText: TextView
    private lateinit var botManaText: TextView
    /* ---------- targeting state ---------- */

    private var awaitingTarget = false
    private var validTargets: List<Any> = emptyList()
    private var handVisible = false
    private var awaitingHeroPower = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerViews(view)
        observeVm()
        setupClicks()

        vm.initializeGame(args.heroPowerId ?: 1, args.deckId ?: 1)
        initialiseHandPosition()
    }

    /* ---------- binding ---------- */

    private fun bindViews(v: View) {
        playerHeroHealth = v.findViewById(R.id.playerHeroHealth)
        botHeroHealth    = v.findViewById(R.id.botHeroHealth)
        playerManaText   = v.findViewById(R.id.playerManaText)
        turnNumberText   = v.findViewById(R.id.turnNumberText)
        endTurnBtn       = v.findViewById(R.id.endTurnButton)
        heroPowerBtn     = v.findViewById(R.id.heroPowerButton)
        statusText       = v.findViewById(R.id.gameStatusText)
        handToggle       = v.findViewById(R.id.handToggleButton)
        playerHandArea   = v.findViewById(R.id.playerHandArea)
        enemyHeroCard = v.findViewById(R.id.enemyHeroCard)
        botManaText      = v.findViewById(R.id.botManaDisplay)
        deckCountText    = v.findViewById(R.id.deckCountText)
    }

    /* ---------- recycler views ---------- */

    private fun setupRecyclerViews(v: View) {
        handRv = v.findViewById(R.id.playerHandRecyclerView)
        playerBoardRv = v.findViewById(R.id.playerBoardRecyclerView)
        botBoardRv    = v.findViewById(R.id.botBoardRecyclerView)
        handAdapter = CardHandAdapter(
            onCardClick     = ::onCardClick,
            onCardLongClick = { showCardDetails(it) },
            canPlayCard     = vm::canPlayCard,
            isCardSelected  = { vm.selectedCard.value == it }
        )
        handRv.adapter = handAdapter
        handRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val playerRv = v.findViewById<RecyclerView>(R.id.playerBoardRecyclerView)
        playerBoardAdapter = MinionBoardAdapter(
            onMinionClick     = ::onPlayerMinionClick,
            canAttackWithMinion = vm::canAttackWithMinion,
            isMinionSelected  = { vm.selectedMinion.value == it },
            isPlayerBoard     = true
        )
        playerRv.adapter = playerBoardAdapter
        playerRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val botRv = v.findViewById<RecyclerView>(R.id.botBoardRecyclerView)
        botBoardAdapter = MinionBoardAdapter(
            onMinionClick  = ::onBotMinionClick,
            isValidTarget  = { vm.validAttackTargets.value?.contains(it) ?: false },
            isPlayerBoard  = false
        )
        botRv.adapter = botBoardAdapter
        botRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    /* ---------- observers ---------- */

    private fun observeVm() {
        vm.playerHand.observe(viewLifecycleOwner) {
            handAdapter.submitList(it) { handAdapter.notifyDataSetChanged() }
        }

        vm.playerBoard.observe(viewLifecycleOwner) {
            playerBoardAdapter.submitList(it) { playerBoardAdapter.notifyDataSetChanged() }
        }

        vm.botBoard.observe(viewLifecycleOwner) {
            botBoardAdapter.submitList(it) { botBoardAdapter.notifyDataSetChanged() }
        }
        vm.deckCount.observe(viewLifecycleOwner) { count ->
            deckCountText.text = count.toString()
        }
        vm.validAttackTargets.observe(viewLifecycleOwner) {
            playerBoardAdapter.notifyDataSetChanged()
            botBoardAdapter.notifyDataSetChanged()
            updateHeroHighlight()
        }

        vm.selectedMinion.observe(viewLifecycleOwner) {
            playerBoardAdapter.notifyDataSetChanged()
            updateHeroHighlight()
        }

        vm.playerHero.observe(viewLifecycleOwner) { updateHeroDisplay(it, playerHeroHealth) }
        vm.botHero.observe(viewLifecycleOwner)    { updateHeroDisplay(it, botHeroHealth) }

        vm.playerMana.observe(viewLifecycleOwner) { mana ->
            playerManaText.text = "Mana: $mana/${vm.playerMaxMana.value}"
            refreshHeroPowerBtn()
        }
        vm.playerMaxMana.observe(viewLifecycleOwner) { max ->
            playerManaText.text = "Mana: ${vm.playerMana.value}/$max"
            refreshHeroPowerBtn()
        }
        vm.botMana.observe(viewLifecycleOwner) { mana ->
            botManaText.text = "Mana: $mana/${vm.botMaxMana.value}"
        }
        vm.botMaxMana.observe(viewLifecycleOwner) { max ->
            botManaText.text = "Mana: ${vm.botMana.value}/$max"
        }

        vm.turnNumber.observe(viewLifecycleOwner) { turnNumberText.text = "Turn: $it" }

        vm.gameState.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            endTurnBtn.isEnabled = !state.isProcessingTurn && !state.gameOver
            statusText.text = state.toMessage()
            refreshHeroPowerBtn()

            if (state.gameOver && !hasNavigatedToResult) {
                hasNavigatedToResult = true
                val action = GamePlayFragmentDirections
                    .actionGamePlayFragmentToGameResultFragment(state.playerWon)
                findNavController().navigate(action)
            }
        }
        vm.gameMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                statusText.text = msg
                view?.postDelayed({
                    statusText.text = vm.gameState.value.toMessage()
                }, 3000)
            }
        }

        vm.selectedCard.observe(viewLifecycleOwner) {
            handAdapter.notifyDataSetChanged()
            updateTargetingMode()
        }
    }

    /* ---------- click handlers ---------- */

    private fun setupClicks() {
        endTurnBtn.setOnClickListener {
            vm.endTurn()
            clearSelections()
        }
        heroPowerBtn.setOnClickListener {
            if (vm.heroPowerRequiresTarget()) {
                awaitingHeroPower = true
                awaitingTarget = true
                validTargets = vm.validTargetsForHeroPower()
                statusText.text = "Select a target for Fireblast"
            } else {
                if (vm.useHeroPower()) refreshHeroPowerBtn()
                else toast("Cannot use hero power!")
            }
        }
        handToggle.setOnClickListener { toggleHand() }

        botHeroHealth   .setOnClickListener { onBotHeroClick() }
        playerHeroHealth.setOnClickListener { onPlayerHeroClick() }
        view?.findViewById<CardView>(R.id.enemyHeroCard)
            ?.setOnClickListener { onBotHeroClick() }
    }

    private fun onCardClick(pos: Int) {
        val card = vm.playerHand.value?.getOrNull(pos) ?: return
        if (awaitingTarget) { clearTargeting(); return }

        if (vm.selectedCard.value == pos) {

            if (card is SpellCard && card.requiresTarget()) {
                enterTargeting(card)
            } else {
                vm.playSelectedCard(null)
                clearSelections()
            }
        } else {
            clearSelections()
            vm.selectCard(pos)
        }
    }

    /* ----- player clicks a friendly minion ----- */
    private fun onPlayerMinionClick(pos: Int) {
        val target = vm.playerBoard.value?.getOrNull(pos) ?: return
        when {
            awaitingHeroPower        -> { fireHeroPower(target) }
            awaitingTarget           -> { vm.playSelectedCard(target); clearTargeting() }
            vm.selectedMinion.value != null -> vm.attackWithSelectedMinion(target)
            else                     -> vm.selectMinion(pos)
        }
    }

    private fun onBotMinionClick(pos: Int) {
        val target = vm.botBoard.value?.getOrNull(pos) ?: return
        when {
            awaitingHeroPower        -> { fireHeroPower(target) }
            awaitingTarget           -> { vm.playSelectedCard(target); clearTargeting() }
            vm.selectedMinion.value != null -> vm.attackWithSelectedMinion(target)
        }
    }

    private fun GameViewModel.getSelectedSpell(): SpellCard? =
        playerHand.value?.getOrNull(selectedCard.value ?: -1) as? SpellCard

    private fun onBotHeroClick()   = heroClick(vm.botHero.value)
    private fun onPlayerHeroClick() = heroClick(vm.playerHero.value)
    private fun heroClick(hero: Hero?) {
        hero ?: return


        if (awaitingHeroPower) {
            fireHeroPower(hero)
            return
        }


        if (awaitingTarget && validTargets.contains(hero)) {
            val spell = vm.getSelectedSpell() ?: return
            fireSpell(spell, listOf(hero))
            return
        }


        if (vm.selectedMinion.value != null &&
            vm.validAttackTargets.value?.contains(hero) == true) {

            vm.attackWithSelectedMinion(hero)
            clearSelections()
        }
    }
    private fun fireHeroPower(target: Any) {
        if (vm.useHeroPower(target)) {
            refreshHeroPowerBtn()
            toast("Hero power used!")
        }
        clearTargeting()
    }
    /* ---------- targeting UI ---------- */

    private fun enterTargeting(spell: SpellCard) {
        awaitingTarget = true
        validTargets = vm.getValidTargetsForSelectedCard()
        toast("Select a target for ${spell.name}")
    }

    private fun updateTargetingMode() {
        awaitingTarget = vm.selectedCard.value != null && validTargets.isNotEmpty()
        if (awaitingTarget) statusText.text = "Select a target..."
    }

    private fun clearTargeting() {
        awaitingTarget = false
        validTargets = emptyList()
        awaitingHeroPower = false
    }

    private fun clearSelections() {
        vm.selectCard(null)
        vm.selectMinion(null)
        clearTargeting()
    }
    private fun fireSpell(card: SpellCard, targets: List<Any>) {

        val handPos  = vm.selectedCard.value ?: return
        val cardView = handRv.findViewHolderForAdapterPosition(handPos)?.itemView ?: return
        val first    = targets.firstOrNull() ?: return


        val targetView = when (first) {

            is MinionCard -> {

                val playerIdx = vm.playerBoard.value?.indexOf(first) ?: -1
                if (playerIdx != -1) {
                    playerBoardRv.findViewHolderForAdapterPosition(playerIdx)?.itemView
                } else {
                    val botIdx = vm.botBoard.value?.indexOf(first) ?: -1
                    botBoardRv.findViewHolderForAdapterPosition(botIdx)?.itemView
                }
            }

            is Hero -> enemyHeroCard
            else    -> null
        } ?: return


        val root = requireView() as ViewGroup
        animateSpell(cardView, targetView, root) {
            vm.playSelectedCard(first)
            clearSelections()
        }
    }




    /* ---------- visuals ---------- */

    private fun toggleHand() {
        handVisible = !handVisible
        val delta = if (handVisible) 0f else playerHandArea.height.toFloat()
        val alpha = if (handVisible) 1f else 0f
        playerHandArea.visibility = View.VISIBLE
        playerHandArea.animate().translationY(delta).alpha(alpha).setDuration(300)
            .withEndAction { if (!handVisible) playerHandArea.visibility = View.GONE }.start()
        handToggle.animate().rotation(if (handVisible) 180f else 0f).setDuration(300).start()
    }

    private fun initialiseHandPosition() {
        playerHandArea.translationY = playerHandArea.height.toFloat()
        playerHandArea.alpha = 0f
    }

    private fun updateHeroHighlight() {
        val hero = vm.botHero.value ?: return
        val border = if (vm.validAttackTargets.value?.contains(hero) == true)
            R.drawable.border_valid_target else R.drawable.header_background
        view?.findViewById<CardView>(R.id.enemyHeroCard)?.setBackgroundResource(border)
    }

    private fun updateHeroDisplay(hero: Hero, tv: TextView) {
        val text = if (hero.armor > 0)
            "Health: ${hero.currentHealth}/${hero.maxHealth} (+${hero.armor})"
        else
            "Health: ${hero.currentHealth}/${hero.maxHealth}"
        tv.text = text

        val pct = hero.currentHealth.toFloat() / hero.maxHealth
        val colorRes = when {
            pct > 0.6f -> R.color.hero_health_high
            pct > 0.3f -> R.color.hero_health_medium
            else       -> R.color.hero_health_low
        }
        tv.setTextColor(requireContext().getColor(colorRes))
    }

    private fun refreshHeroPowerBtn() {
        val canUse = vm.canUseHeroPower()
        heroPowerBtn.isEnabled = canUse
        heroPowerBtn.alpha = if (canUse) 1f else 0.5f
    }

    private fun showCardDetails(card: Card) =
        CardDetailFragment.newInstance(card).show(childFragmentManager, "CardDetail")

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    /* ---------- helpers ---------- */

    private fun GameState?.toMessage(): String = when {
        this == null         -> "Loading..."
        gameOver             -> if (playerWon) "Victory!" else "Defeat!"
        isProcessingTurn     -> "Enemy Turn..."
        currentTurn == Turn.PLAYER -> "Your Turn - Turn $turnNumber"
        else                 -> "Enemy Turn..."
    }
}
