package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import java.nio.file.Path

fun readRsmFromTxt(pathToTXT: String): RsmState {
    return readRsmFromTxt(Path.of(pathToTXT))
}

fun readRsmFromTxt(pathToTXT: Path): RsmState {
    val idToState: HashMap<Int, RsmState> = HashMap()
    var startRsmState: RsmState? = null

    fun makeRsmState(
        id: Int,
        nonterminal: Nonterminal,
        isStart: Boolean = false,
        isFinal: Boolean = false
    ): RsmState {
        val y = RsmState(nonterminal, isStart, isFinal)

        if (!idToState.containsKey(id)) idToState[id] = y

        return idToState[id]!!
    }

    val nameToNonterminal: HashMap<String, Nonterminal> = HashMap()

    fun makeNonterminal(name: String): Nonterminal {
        return nameToNonterminal.getOrPut(name) { Nonterminal(name) }
    }

    val startStateRegex =
        """^StartState\(
        |id=(?<id>.*),
        |nonterminal=Nonterminal\("(?<nonterminalValue>.*)"\),
        |isStart=(?<isStart>.*),
        |isFinal=(?<isFinal>.*)
        |\)$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val rsmStateRegex =
        """^State\(
        |id=(?<id>.*),
        |nonterminal=Nonterminal\("(?<nonterminalValue>.*)"\),
        |isStart=(?<isStart>.*),
        |isFinal=(?<isFinal>.*)
        |\)$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val rsmTerminalEdgeRegex =
        """^TerminalEdge\(
        |tail=(?<tailId>.*),
        |head=(?<headId>.*),
        |terminal=Terminal\("(?<literalValue>.*)"\)
        |\)$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val rsmNonterminalEdgeRegex =
        """^NonterminalEdge\(
        |tail=(?<tailId>.*),
        |head=(?<headId>.*),
        |nonterminal=Nonterminal\("(?<nonterminalValue>.*)"\)
        |\)$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val reader = pathToTXT.toFile().inputStream().bufferedReader()

    while (true) {
        val line = reader.readLine() ?: break

        if (startStateRegex.matches(line)) {
            val (idValue, nonterminalValue, isStartValue, isFinalValue) =
                startStateRegex.matchEntire(line)!!.destructured

            val tmpNonterminal = makeNonterminal(nonterminalValue)

            startRsmState =
                makeRsmState(
                    id = idValue.toInt(),
                    nonterminal = tmpNonterminal,
                    isStart = isStartValue == "true",
                    isFinal = isFinalValue == "true",
                )

            if (startRsmState.isStart) tmpNonterminal.startState = startRsmState

        } else if (rsmStateRegex.matches(line)) {
            val (idValue, nonterminalValue, isStartValue, isFinalValue) =
                rsmStateRegex.matchEntire(line)!!.destructured

            val tmpNonterminal = makeNonterminal(nonterminalValue)

            val tmpRsmState =
                makeRsmState(
                    id = idValue.toInt(),
                    nonterminal = tmpNonterminal,
                    isStart = isStartValue == "true",
                    isFinal = isFinalValue == "true",
                )

            if (tmpRsmState.isStart) tmpNonterminal.startState = tmpRsmState

        } else if (rsmTerminalEdgeRegex.matches(line)) {
            val (tailId, headId, terminalValue) = rsmTerminalEdgeRegex.matchEntire(line)!!.destructured

            val tailRsmState = idToState[tailId.toInt()]!!
            val headRsmState = idToState[headId.toInt()]!!

            tailRsmState.addEdge(Terminal(terminalValue),headRsmState)
        } else if (rsmNonterminalEdgeRegex.matches(line)) {
            val (tailId, headId, nonterminalValue) =
                rsmNonterminalEdgeRegex.matchEntire(line)!!.destructured

            val tailRSMState = idToState[tailId.toInt()]!!
            val headRSMState = idToState[headId.toInt()]!!

            tailRSMState.addEdge(makeNonterminal(nonterminalValue), headRSMState)
        }
    }

    return startRsmState!!
}
