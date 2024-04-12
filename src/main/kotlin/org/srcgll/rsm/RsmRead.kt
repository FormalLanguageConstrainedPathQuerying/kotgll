package org.ucfs.rsm

import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.rsm.symbol.Term
import java.nio.file.Path

fun readRsmFromDot(filePath: String): RsmState = readRsmFromDot(Path.of(filePath))

fun readRsmFromTxt(filePath: String): RsmState = readRsmFromTxt(Path.of(filePath))

fun readRsmFromTxt(filePath: Path): RsmState {
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

    val reader = filePath.toFile().inputStream().bufferedReader()

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

            tailRsmState.addEdge(Term(terminalValue),headRsmState)
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

//TODO: Resolve problems with regular expression for rsm state
// and decide how to determine start state for RSM
fun readRsmFromDot(pathToTXT: Path): RsmState {
    val idToState: HashMap<String, RsmState> = HashMap()
    val nonterminals: HashSet<String> = HashSet()
    var startRsmState: RsmState? = null
    val nameToNonterminal: HashMap<String, Nonterminal> = HashMap()

    fun makeRsmState(
        id: String,
        nonterminal: Nonterminal,
        isStart: Boolean,
        isFinal: Boolean,
    ): RsmState {
        return idToState.getOrPut(id) { RsmState(nonterminal, isStart, isFinal) }
    }

    fun makeNonterminal(name: String): Nonterminal {
        return nameToNonterminal.getOrPut(name) { Nonterminal(name) }
    }

    val startStateRegex =
        """^(?<id>.*) \[label = \"(?<nonterminalName>.*),.*\", shape = (?<shape>.*), color = green\]$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val rsmStateRegex =
        """^(?<id>.*) \[label = \"?(<nonterminalName>.*),.*\", shape = (?<shape>.*), color = black|red\]$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val rsmEdgeRegex =
        """^(?<edgeFrom>.*) -> (?<edgeTo>.*) \[label = \"(?<edgeLabel>.*)\"\]$"""
            .trimMargin()
            .replace("\n", "")
            .toRegex()

    val reader = pathToTXT.toFile().inputStream().bufferedReader()

    while (true) {
        val line = reader.readLine() ?: break

        if (startStateRegex.matches(line)) {
            val (id, nonterminalName, shape) =
                startStateRegex.matchEntire(line)!!.destructured

            val tmpNonterminal = makeNonterminal(nonterminalName)

            tmpNonterminal.startState =
                makeRsmState(
                    id = id,
                    nonterminal = tmpNonterminal,
                    isStart = true,
                    isFinal = shape == "doublecircle",
                )
        } else if (rsmStateRegex.matches(line)) {
            val (id, nonterminalName, shape) =
                rsmStateRegex.matchEntire(line)!!.destructured

            val tmpNonterminal = makeNonterminal(nonterminalName)

            makeRsmState(
                id = id,
                nonterminal = tmpNonterminal,
                isStart = false,
                isFinal = shape == "doublecircle",
            )
        } else if (rsmEdgeRegex.matches(line)) {
            val (tailId, headId, edgeLabel) = rsmEdgeRegex.matchEntire(line)!!.destructured
            val tailRsmState = idToState[tailId]!!
            val headRsmState = idToState[headId]!!

            if (nameToNonterminal.containsKey(edgeLabel)) {
                tailRsmState.addEdge(makeNonterminal(edgeLabel), headRsmState)
            } else {
                tailRsmState.addEdge(Term(edgeLabel),headRsmState)
            }
        }
    }

    return startRsmState!!
}
